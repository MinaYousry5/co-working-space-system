package com.workspace.booking.serviceimpl;

import com.workspace.booking.common.enums.BookingStatus;
import com.workspace.booking.common.enums.DurationType;
import com.workspace.booking.common.enums.PaymentStatus;
import com.workspace.booking.common.enums.PaymentTransactionType;
import com.workspace.booking.common.enums.YesNo;
import com.workspace.booking.common.exception.BookingOverlapException;
import com.workspace.booking.common.exception.InvalidBookingTimeException;
import com.workspace.booking.dto.booking.BookingCancelRequest;
import com.workspace.booking.dto.booking.BookingCancelResponse;
import com.workspace.booking.dto.booking.BookingCreateRequest;
import com.workspace.booking.dto.booking.BookingResponse;
import com.workspace.booking.entity.booking.Booking;
import com.workspace.booking.entity.finance.Payment;
import com.workspace.booking.entity.identity.User;
import com.workspace.booking.entity.workspace.Workspace;
import com.workspace.booking.mapper.BookingMapper;
import com.workspace.booking.repository.BookingRepository;
import com.workspace.booking.repository.PaymentRepository;
import com.workspace.booking.repository.UserRepository;
import com.workspace.booking.repository.WorkspaceRepository;
import com.workspace.booking.service.EmailService;
import com.workspace.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    private final BookingRepository bookingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final BookingMapper bookingMapper;

    @Value("${server.ip}")
    private String serverIp;

    @Override
    @Transactional
    public BookingResponse create(BookingCreateRequest request) {
        validateDateRange(request.startDatetime(), request.endDatetime());

        Workspace workspace = lockWorkspace(request.workspaceId());
        validateWorkspaceCanBeBooked(workspace, request.startDatetime(), request.endDatetime(), request.numAttendees());

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new InvalidBookingTimeException("User not found"));

        rejectOverlappingBooking(workspace.getId(), request.startDatetime(), request.endDatetime());

        DurationType calculatedDurationType = determineDurationType(request.startDatetime(), request.endDatetime());

        log.info("The request for booking is: "+request);
        Booking booking = Booking.builder()
                .user(user)
                .workspace(workspace)
                .promoCodeId(request.promoCodeId())
                .bookingRef(generateBookingRef())
                .startDatetime(request.startDatetime())
                .endDatetime(request.endDatetime())
                .durationType(calculatedDurationType)
                .status(BookingStatus.PENDING)
                .numAttendees(request.numAttendees() == null ? 1 : request.numAttendees())
                .purpose(request.purpose())
                .internalNotes(request.internalNotes())
                .basePrice(calculateBasePrice(workspace, calculatedDurationType, request.startDatetime(), request.endDatetime()))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .currency(workspace.getCurrency() == null ? "EGP" : workspace.getCurrency())
                .build();
        booking.setTotalAmount(booking.getBasePrice().subtract(booking.getDiscountAmount()).add(booking.getTaxAmount()));
        log.info("The saved for booking is: "+booking);
        Booking savedBooking = bookingRepository.save(booking);
        sendBookingPaymentInstructionEmail(savedBooking);
        return bookingMapper.toResponse(savedBooking);
    }

    @Override
    @Transactional
    public BookingCancelResponse cancel(Long bookingId, BookingCancelRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidBookingTimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new InvalidBookingTimeException("Only active bookings can be cancelled");
        }

        lockWorkspace(booking.getWorkspace().getId());

        User cancelledBy = userRepository.findById(request.cancelledByUserId())
                .orElseThrow(() -> new InvalidBookingTimeException("Cancelling user not found"));

        LocalDateTime cancelStart = request.cancelStartDatetime() == null ? booking.getStartDatetime() : request.cancelStartDatetime();
        LocalDateTime cancelEnd = request.cancelEndDatetime() == null ? booking.getEndDatetime() : request.cancelEndDatetime();
        validateCancellationRange(booking, cancelStart, cancelEnd);

        List<Booking> remaining = new ArrayList<>();
        LocalDateTime originalStart = booking.getStartDatetime();
        LocalDateTime originalEnd = booking.getEndDatetime();

        boolean cancelWholeBooking = cancelStart.equals(originalStart) && cancelEnd.equals(originalEnd);
        BigDecimal refundAmount = calculateRefundAmount(booking, cancelStart, cancelEnd, cancelWholeBooking);
        if (cancelWholeBooking) {
            markCancelled(booking, cancelledBy, request.reason());
        } else if (cancelStart.equals(originalStart)) {
            booking.setStartDatetime(cancelEnd);
            refreshPrice(booking);
            remaining.add(booking);
        } else if (cancelEnd.equals(originalEnd)) {
            booking.setEndDatetime(cancelStart);
            refreshPrice(booking);
            remaining.add(booking);
        } else {
            booking.setEndDatetime(cancelStart);
            refreshPrice(booking);
            remaining.add(booking);

            Booking tail = copyRemainingBooking(booking, cancelEnd, originalEnd);
            remaining.add(bookingRepository.save(tail));
        }

        if (!cancelWholeBooking) {
            booking.setInternalNotes(appendCancelNote(booking.getInternalNotes(), request.reason(), cancelStart, cancelEnd));
        }

        Booking savedBooking = bookingRepository.save(booking);
        createRefundRequestIfEligible(savedBooking, refundAmount, request.reason());
        return new BookingCancelResponse(
                bookingMapper.toResponse(savedBooking),
                remaining.stream().map(bookingMapper::toResponse).toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getById(Long id) {
        return bookingRepository.findById(id)
                .map(bookingMapper::toResponse)
                .orElseThrow(() -> new InvalidBookingTimeException("Booking not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getByUser(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByStartDatetimeDesc(userId, pageable)
                .map(bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWorkspaceAvailable(Long workspaceId, LocalDateTime startDatetime, LocalDateTime endDatetime) {
        validateDateRange(startDatetime, endDatetime);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new InvalidBookingTimeException("Workspace not found"));
        return workspace.getIsAvailable() == YesNo.Y
                && bookingRepository.findOverlappingBookings(workspaceId, startDatetime, endDatetime, BLOCKING_STATUSES).isEmpty();
    }

    private Workspace lockWorkspace(Long workspaceId) {
        try {
            return workspaceRepository.lockById(workspaceId)
                    .orElseThrow(() -> new InvalidBookingTimeException("Workspace not found"));
        } catch (CannotAcquireLockException ex) {
            throw new BookingOverlapException("Workspace is currently being booked by another request. Please try again.");
        }
    }

    private void validateWorkspaceCanBeBooked(Workspace workspace, LocalDateTime start, LocalDateTime end, Integer attendees) {
        if (workspace.getIsAvailable() != YesNo.Y) {
            throw new InvalidBookingTimeException("Workspace is not available for booking");
        }

        int requestedAttendees = attendees == null ? 1 : attendees;
        if (requestedAttendees > workspace.getCapacity()) {
            throw new InvalidBookingTimeException("Number of attendees exceeds workspace capacity");
        }

        long hours = ceilHours(start, end);
        if (workspace.getMinBookingHrs() != null && hours < workspace.getMinBookingHrs()) {
            throw new InvalidBookingTimeException("Booking duration is less than workspace minimum hours");
        }
        if (workspace.getMaxBookingHrs() != null && hours > workspace.getMaxBookingHrs()) {
            throw new InvalidBookingTimeException("Booking duration exceeds workspace maximum hours");
        }
        if (workspace.getAdvanceNotice() != null && start.isBefore(LocalDateTime.now().plusHours(workspace.getAdvanceNotice()))) {
            throw new InvalidBookingTimeException("Booking does not satisfy workspace advance notice");
        }
    }

    private void rejectOverlappingBooking(Long workspaceId, LocalDateTime start, LocalDateTime end) {
        if (!bookingRepository.findOverlappingBookings(workspaceId, start, end, BLOCKING_STATUSES).isEmpty()) {
            throw new BookingOverlapException("Workspace already has an active booking in this time range");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidBookingTimeException("Booking end date/time must be after start date/time");
        }
    }

    private void validateCancellationRange(Booking booking, LocalDateTime cancelStart, LocalDateTime cancelEnd) {
        validateDateRange(cancelStart, cancelEnd);
        if (cancelStart.isBefore(booking.getStartDatetime()) || cancelEnd.isAfter(booking.getEndDatetime())) {
            throw new InvalidBookingTimeException("Cancellation range must be inside the booking range");
        }
    }

    private void markCancelled(Booking booking, User cancelledBy, String reason) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(cancelledBy);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelReason(reason);
    }

    private Booking copyRemainingBooking(Booking source, LocalDateTime start, LocalDateTime end) {
        Booking booking = Booking.builder()
                .user(source.getUser())
                .workspace(source.getWorkspace())
                .promoCodeId(source.getPromoCodeId())
                .bookingRef(generateBookingRef())
                .startDatetime(start)
                .endDatetime(end)
                .durationType(source.getDurationType())
                .status(source.getStatus())
                .numAttendees(source.getNumAttendees())
                .purpose(source.getPurpose())
                .internalNotes(source.getInternalNotes())
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .currency(source.getCurrency())
                .build();
        refreshPrice(booking);
        return booking;
    }

    private void refreshPrice(Booking booking) {
        booking.setDurationType(determineDurationType(booking.getStartDatetime(), booking.getEndDatetime()));
        BigDecimal basePrice = calculateBasePrice(
                booking.getWorkspace(),
                booking.getDurationType(),
                booking.getStartDatetime(),
                booking.getEndDatetime()
        );
        booking.setBasePrice(basePrice);
        booking.setDiscountAmount(booking.getDiscountAmount() == null ? BigDecimal.ZERO : booking.getDiscountAmount());
        booking.setTaxAmount(booking.getTaxAmount() == null ? BigDecimal.ZERO : booking.getTaxAmount());
        booking.setTotalAmount(basePrice.subtract(booking.getDiscountAmount()).add(booking.getTaxAmount()));
    }

    private DurationType determineDurationType(LocalDateTime start, LocalDateTime end) {
        long hours = ceilHours(start, end);
        if (hours < 4) {
            return DurationType.HOURLY;
        }
        long days = ceilUnits(Duration.between(start, end).toMinutes(), 24L * 60L);
        if (days < 10) {
            return DurationType.DAILY;
        }
        return DurationType.MONTHLY;
    }

    private BigDecimal calculateBasePrice(Workspace workspace, DurationType durationType, LocalDateTime start, LocalDateTime end) {
        BigDecimal unitPrice = switch (durationType) {
            case HOURLY -> workspace.getPriceHourly();
            case DAILY -> workspace.getPriceDaily();
            case MONTHLY -> workspace.getPriceMonthly();
        };
        if (unitPrice == null) {
            throw new InvalidBookingTimeException("Workspace does not have a price for " + durationType);
        }

        BigDecimal units = switch (durationType) {
            case HOURLY -> BigDecimal.valueOf(ceilHours(start, end));
            case DAILY -> BigDecimal.valueOf(ceilUnits(Duration.between(start, end).toMinutes(), 24L * 60L));
            case MONTHLY -> BigDecimal.valueOf(ceilUnits(Duration.between(start, end).toMinutes(), 30L * 24L * 60L));
        };

        return unitPrice.multiply(units).setScale(2, RoundingMode.HALF_UP);
    }

    private long ceilHours(LocalDateTime start, LocalDateTime end) {
        return ceilUnits(Duration.between(start, end).toMinutes(), 60L);
    }

    private long ceilUnits(long totalMinutes, long unitMinutes) {
        return Math.max(1, (totalMinutes + unitMinutes - 1) / unitMinutes);
    }

    private String appendCancelNote(String notes, String reason, LocalDateTime cancelStart, LocalDateTime cancelEnd) {
        String cancelNote = "Partial cancellation from " + cancelStart + " to " + cancelEnd
                + (reason == null || reason.isBlank() ? "" : ". Reason: " + reason);
        return notes == null || notes.isBlank() ? cancelNote : notes + " | " + cancelNote;
    }

    private String generateBookingRef() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void sendBookingPaymentInstructionEmail(Booking booking) {
        String paymentLink = serverIp + "payment?bookingId=" + booking.getId();
        String body = "Thanks for booking and to confirm booking please send your fees "
                + booking.getTotalAmount() + " " + booking.getCurrency()
                + " in 24 hrs to confirm booking and you can pay on\n"
                + "vodafone cash : 01099070589 or instapay on : 01099070589\n"
                + "and when pay please fill the form in this link : " + paymentLink;

        emailService.sendEmail(
                booking.getUser().getEmail(),
                "Booking payment confirmation",
                body
        );
    }

    private BigDecimal calculateRefundAmount(Booking booking, LocalDateTime cancelStart, LocalDateTime cancelEnd, boolean cancelWholeBooking) {
        if (cancelWholeBooking) {
            return booking.getTotalAmount();
        }

        BigDecimal originalBasePrice = booking.getBasePrice();
        BigDecimal newBasePrice = BigDecimal.ZERO;
        
        LocalDateTime originalStart = booking.getStartDatetime();
        LocalDateTime originalEnd = booking.getEndDatetime();
        
        if (cancelStart.equals(originalStart)) {
            newBasePrice = newBasePrice.add(calculateBasePrice(booking.getWorkspace(), determineDurationType(cancelEnd, originalEnd), cancelEnd, originalEnd));
        } else if (cancelEnd.equals(originalEnd)) {
            newBasePrice = newBasePrice.add(calculateBasePrice(booking.getWorkspace(), determineDurationType(originalStart, cancelStart), originalStart, cancelStart));
        } else {
            newBasePrice = newBasePrice.add(calculateBasePrice(booking.getWorkspace(), determineDurationType(originalStart, cancelStart), originalStart, cancelStart));
            newBasePrice = newBasePrice.add(calculateBasePrice(booking.getWorkspace(), determineDurationType(cancelEnd, originalEnd), cancelEnd, originalEnd));
        }
        
        BigDecimal refund = originalBasePrice.subtract(newBasePrice);
        return refund.compareTo(BigDecimal.ZERO) > 0 ? refund : BigDecimal.ZERO;
    }

    private void createRefundRequestIfEligible(Booking booking, BigDecimal refundAmount, String reason) {
        boolean hasConfirmedPayment = paymentRepository
                .findFirstByBookingIdAndTransactionTypeAndStatus(
                        booking.getId(),
                        PaymentTransactionType.PAYMENT,
                        PaymentStatus.CONFIRMED
                )
                .isPresent();

        boolean beforeReservationByMoreThan24Hours = booking.getStartDatetime().isAfter(LocalDateTime.now().plusHours(24));
        if (!hasConfirmedPayment || !beforeReservationByMoreThan24Hours) {
            return;
        }

        Payment refundPayment = Payment.builder()
                .booking(booking)
                .user(booking.getUser())
                .transactionType(PaymentTransactionType.REFUND)
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.ZERO)
                .refundAmount(refundAmount)
                .reasonOfReject(reason)
                .build();
        paymentRepository.save(refundPayment);
    }
}

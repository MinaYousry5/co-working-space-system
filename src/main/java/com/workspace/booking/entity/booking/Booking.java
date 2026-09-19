package com.workspace.booking.entity.booking;

import com.workspace.booking.common.enums.*;
import com.workspace.booking.entity.BaseEntity;
import com.workspace.booking.entity.identity.User;
import com.workspace.booking.entity.workspace.Workspace;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "WS_BOOKINGS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "USER_ID")
    private User user;

    @ManyToOne @JoinColumn(name = "WORKSPACE_ID")
    private Workspace workspace;

    @Column(name = "PROMO_CODE_ID")
    private Long promoCodeId;

    @ManyToOne @JoinColumn(name = "CANCELLED_BY")
    private User cancelledBy;

    @Column(name = "BOOKING_REF", nullable = false, unique = true)
    private String bookingRef;

    @Column(name = "START_DATETIME", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "END_DATETIME", nullable = false)
    private LocalDateTime endDatetime;

    @Enumerated(EnumType.STRING)
    @Column(name = "DURATION_TYPE", nullable = false)
    private DurationType durationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private BookingStatus status;

    @Column(name = "NUM_ATTENDEES", nullable = false)
    private Integer numAttendees;

    @Column(name = "PURPOSE")
    private String purpose;

    @Column(name = "INTERNAL_NOTES")
    private String internalNotes;

    @Column(name = "BASE_PRICE", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "DISCOUNT_AMOUNT", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "TAX_AMOUNT", nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "TOTAL_AMOUNT", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "CURRENCY", nullable = false)
    private String currency;

    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    @Column(name = "CANCEL_REASON")
    private String cancelReason;
}

package com.workspace.booking.serviceimpl;

import com.workspace.booking.common.enums.BookingStatus;
import com.workspace.booking.common.enums.YesNo;
import com.workspace.booking.common.exception.InvalidBookingTimeException;
import com.workspace.booking.dto.Workspace.WorkspaceCreateRequest;
import com.workspace.booking.dto.Workspace.WorkspaceResponse;
import com.workspace.booking.entity.workspace.Workspace;
import com.workspace.booking.entity.workspace.WorkspaceType;
import com.workspace.booking.mapper.WorkspaceMapper;
import com.workspace.booking.repository.BookingRepository;
import com.workspace.booking.repository.WorkspaceRepository;
import com.workspace.booking.repository.WorkspaceTypeRepository;
import com.workspace.booking.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceTypeRepository workspaceTypeRepository;
    private final BookingRepository bookingRepository;
    private final WorkspaceMapper mapper;

    private static final List<BookingStatus> BLOCKING_BOOKING_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN
    );

    @Override
    @Transactional
    public WorkspaceResponse create(WorkspaceCreateRequest req) {
        // 1. Fetch the related entities (Required to avoid NULL foreign keys)
        WorkspaceType type = workspaceTypeRepository.findById(req.workspaceTypeId())
                .orElseThrow(() -> new RuntimeException("Workspace Type not found"));


        byte[] blobData = req.floorPlanBlob() != null
                ? Base64.getDecoder().decode(req.floorPlanBlob())
                : null;
                
        YesNo isAvailable = req.isAvailable() != null ? req.isAvailable() : YesNo.Y;
        if (YesNo.N.equals(type.getIsActive())) {
            isAvailable = YesNo.N;
        }

        // 2. Build the entity and SET the relations
        Workspace workspace = Workspace.builder()
                .workspaceName(req.workspaceName())
                .workspaceCode(req.workspaceCode())
                .description(req.description())
                .capacity(req.capacity())
                .priceHourly(req.priceHourly())
                .priceDaily(req.priceDaily())
                .priceMonthly(req.priceMonthly())
                .currency(req.currency())
                .floorNumber(req.floorNumber())
                .roomNumber(req.roomNumber())
                .minBookingHrs(req.minBookingHrs())
                .maxBookingHrs(req.maxBookingHrs())
                .isAvailable(isAvailable)
                .isFeatured(YesNo.N)
                // IMPORTANT: Assign the objects here
                .workspaceType(type)
                .advanceNotice(req.advanceNotice())
                .cancellationHrs(req.cancellationHrs())
                .floorPlanBlob(blobData)
                .floorPlanMimeType(req.floorPlanMimeType())
                .floorPlanFilename(req.floorPlanFilename())
                .build();

        // 3. Save
        return mapper.toResponse(workspaceRepository.save(workspace));
    }


    @Override
    @Transactional
    public WorkspaceResponse update(Long id, WorkspaceCreateRequest req) {
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
                
        WorkspaceType type = workspaceTypeRepository.findById(req.workspaceTypeId())
                .orElseThrow(() -> new RuntimeException("Workspace Type not found"));

        ws.setWorkspaceName(req.workspaceName());
        ws.setCapacity(req.capacity());
        ws.setPriceHourly(req.priceHourly());

        YesNo isAvailable = req.isAvailable() != null ? req.isAvailable() : ws.getIsAvailable();
        if (YesNo.N.equals(type.getIsActive())) {
            isAvailable = YesNo.N;
        }
        ws.setIsAvailable(isAvailable);
        ws.setWorkspaceType(type);

        return mapper.toResponse(workspaceRepository.save(ws));
    }

    @Override
    public WorkspaceResponse getById(Long id) {
        return workspaceRepository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
    }

    @Override
    public List<WorkspaceResponse> getAll() {
        return workspaceRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getAvailableForBooking(LocalDateTime startDatetime, LocalDateTime endDatetime) {
        validateBookingRange(startDatetime, endDatetime);

        long durationHours = ceilHours(startDatetime, endDatetime);
        LocalDateTime now = LocalDateTime.now();

        return workspaceRepository.findAvailableForBooking(
                        startDatetime,
                        endDatetime,
                        BLOCKING_BOOKING_STATUSES,
                        YesNo.Y
                )
                .stream()
                .filter(workspace -> satisfiesDurationRules(workspace, durationHours))
                .filter(workspace -> satisfiesAdvanceNotice(workspace, startDatetime, now))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {
        workspaceRepository.deleteById(id);
    }

    private void validateBookingRange(LocalDateTime startDatetime, LocalDateTime endDatetime) {
        if (startDatetime == null || endDatetime == null || !endDatetime.isAfter(startDatetime)) {
            throw new InvalidBookingTimeException("End date/time must be after start date/time");
        }
    }

    private boolean  satisfiesDurationRules(Workspace workspace, long durationHours) {
        return (workspace.getMinBookingHrs() == null || durationHours >= workspace.getMinBookingHrs())
                && (workspace.getMaxBookingHrs() == null || durationHours <= workspace.getMaxBookingHrs());
    }

    private boolean satisfiesAdvanceNotice(Workspace workspace, LocalDateTime startDatetime, LocalDateTime now) {
        Integer advanceNotice = workspace.getAdvanceNotice();
        return advanceNotice == null || !startDatetime.isBefore(now.plusHours(advanceNotice));
    }

    private long ceilHours(LocalDateTime startDatetime, LocalDateTime endDatetime) {
        long minutes = Duration.between(startDatetime, endDatetime).toMinutes();
        return Math.max(1, (minutes + 59L) / 60L);
    }
//    @Override
//    @Transactional(readOnly = true)
//    public Page<WorkspaceResponse> search(WorkspaceSearchRequest request) {
//        return workspaceRepository.search(
//                        request.locationId(),
//                        request.typeId(),
//                        request.minCapacity(),
//                        PageRequest.of(request.page(), request.size()))
//                .map(this::toResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public WorkspaceResponse findById(Long id) {
//        return workspaceRepository.findById(id)
//                .map(this::toResponse)
//                .orElseThrow(() -> new CustomException(ErrorCode.VALIDATION_ERROR, "Workspace not found"));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public WorkspaceAvailabilityResponse availability(Long workspaceId, LocalDateTime start, LocalDateTime end) {
//        var workspace = workspaceRepository.findById(workspaceId)
//                .orElseThrow(() -> new CustomException(ErrorCode.VALIDATION_ERROR, "Workspace not found"));
//        var reserved = bookingRepository.countReservedAttendees(
//                workspaceId,
//                start,
//                end,
//                List.of(com.workspace.booking.common.enums.BookingStatus.PENDING,
//                        com.workspace.booking.common.enums.BookingStatus.CONFIRMED,
//                        com.workspace.booking.common.enums.BookingStatus.CHECKED_IN));
//        var remaining = workspace.getCapacity() - reserved.intValue();
//        return new WorkspaceAvailabilityResponse(
//                workspace.getId(),
//                workspace.getWorkspaceName(),
//                workspace.getWorkspaceType().getTypeName(),
//                workspace.getCapacity(),
//                workspace.getPriceHourly(),
//                null,
//                List.of(),
//                remaining > 0
//        );
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ResourceResponse> resources(Long locationId) {
//        return resourceRepository.findByLocationIdAndIsActiveOrderByResourceName(locationId, YesNo.Y)
//                .stream()
//                .map(resource -> new ResourceResponse(
//                        resource.getId(),
//                        resource.getResourceName(),
//                        resource.getResourceType() == null ? null : resource.getResourceType().getTypeName(),
//                        resource.getDescription(),
//                        resource.getQuantityAvailable(),
//                        resource.getPricePerUnit(),
//                        resource.getPriceUnit() == null ? null : resource.getPriceUnit().name(),
//                        resource.getCurrency()))
//                .toList();
//    }
//
//    private WorkspaceResponse toResponse(com.workspace.booking.entity.workspace.Workspace workspace) {
//        return new WorkspaceResponse(
//                workspace.getId(),
//                workspace.getWorkspaceName(),
//                workspace.getWorkspaceCode(),
//                workspace.getDescription(),
//                null,
//                workspace.getCapacity(),
//                workspace.getPriceHourly(),
//                workspace.getWorkspaceType() == null ? null : workspace.getWorkspaceType().getTypeName(),
//                workspace.getLocation() == null ? null : workspace.getLocation().getLocationName(),
//                workspace.getFloorNumber(),
//                workspace.getRoomNumber(),
//                workspace.getIsAvailable() != null && workspace.getIsAvailable() == 1,
//                List.of(),
//                null
//        );
//    }
}

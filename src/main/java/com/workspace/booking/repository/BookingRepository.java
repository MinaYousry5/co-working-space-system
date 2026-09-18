package com.workspace.booking.repository;

import com.workspace.booking.common.enums.BookingStatus;
import com.workspace.booking.entity.booking.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserIdOrderByStartDatetimeDesc(Long userId, Pageable pageable);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByStatusAndCreatedOnBefore(BookingStatus status, LocalDateTime createdOn);

    Page<Booking> findAllByOrderByStartDatetimeDesc(Pageable pageable);

    @Query("""
            select b from Booking b
            where b.workspace.id = :workspaceId
              and b.status in :statuses
              and b.startDatetime < :endDatetime
              and b.endDatetime > :startDatetime
            order by b.startDatetime asc
            """)
    List<Booking> findOverlappingBookings(@Param("workspaceId") Long workspaceId,
                                          @Param("startDatetime") LocalDateTime startDatetime,
                                          @Param("endDatetime") LocalDateTime endDatetime,
                                          @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select coalesce(sum(b.numAttendees), 0) from Booking b
            where b.workspace.id = :workspaceId
              and b.status in :statuses
              and b.startDatetime < :endDatetime
              and b.endDatetime > :startDatetime
            """)
    Long countReservedAttendees(@Param("workspaceId") Long workspaceId,
                                @Param("startDatetime") LocalDateTime startDatetime,
                                @Param("endDatetime") LocalDateTime endDatetime,
                                @Param("statuses") Collection<BookingStatus> statuses);

    long countByStatusIn(Collection<BookingStatus> statuses);
}

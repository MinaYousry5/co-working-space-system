package com.workspace.booking.repository;

import com.workspace.booking.common.enums.BookingStatus;
import com.workspace.booking.common.enums.YesNo;
import com.workspace.booking.entity.workspace.Workspace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Workspace w where w.id = :workspaceId")
    Optional<Workspace> lockById(@Param("workspaceId") Long workspaceId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update Workspace w set w.isAvailable = :isAvailable where w.workspaceType.id = :typeId")
    void updateIsAvailableByWorkspaceTypeId(@Param("typeId") Long typeId, @Param("isAvailable") YesNo isAvailable);

    @Query("""
            select w from Workspace w
            where w.isAvailable = :isAvailable
              and not exists (
                  select 1 from Booking b
                  where b.workspace = w
                    and b.status in :blockingStatuses
                    and b.startDatetime < :endDatetime
                    and b.endDatetime > :startDatetime
              )
            order by w.workspaceName asc
            """)
    List<Workspace> findAvailableForBooking(@Param("startDatetime") LocalDateTime startDatetime,
                                            @Param("endDatetime") LocalDateTime endDatetime,
                                            @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses,
                                            @Param("isAvailable") YesNo isAvailable);

//    @Query("""
//            select w from Workspace w
//            where (:typeId is null or w.workspaceType.id = :typeId)
//              and (:minCapacity is null or w.capacity >= :minCapacity)
//              and w.isAvailable = 1
//            """)
//    Page<Workspace> search(@Param("typeId") Long typeId,
//                           @Param("minCapacity") Integer minCapacity,
//                           Pageable pageable);

}

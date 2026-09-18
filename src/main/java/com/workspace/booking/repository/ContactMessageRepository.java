package com.workspace.booking.repository;

import com.workspace.booking.common.enums.ContactStatus;
import com.workspace.booking.entity.engagement.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    Page<ContactMessage> findByUserIdOrderByCreatedOnDesc(Long userId, Pageable pageable);

    Page<ContactMessage> findByStatusOrderByCreatedOnDesc(ContactStatus status, Pageable pageable);

    Page<ContactMessage> findAllByOrderByStatusAscCreatedOnDesc(Pageable pageable);
}

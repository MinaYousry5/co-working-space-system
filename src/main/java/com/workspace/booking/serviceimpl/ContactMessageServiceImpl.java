package com.workspace.booking.serviceimpl;

import com.workspace.booking.common.enums.ContactStatus;
import com.workspace.booking.common.enums.ErrorCode;
import com.workspace.booking.common.exception.CustomException;
import com.workspace.booking.dto.contact.ContactMessageCreateRequest;
import com.workspace.booking.dto.contact.ContactMessageResponse;
import com.workspace.booking.entity.engagement.ContactMessage;
import com.workspace.booking.entity.identity.User;
import com.workspace.booking.mapper.ContactMessageMapper;
import com.workspace.booking.repository.ContactMessageRepository;
import com.workspace.booking.repository.UserRepository;
import com.workspace.booking.service.ContactMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final UserRepository userRepository;
    private final ContactMessageMapper mapper;

    @Override
    @Transactional
    public ContactMessageResponse create(ContactMessageCreateRequest request) {
        log.info("Creating contact message for email={} userId={}", request.email(), request.userId());

        User user = null;
        if (request.userId() != null) {
            user = userRepository.findById(request.userId())
                    .orElseThrow(() -> {
                        log.warn("Contact message rejected because userId={} was not found", request.userId());
                        return new CustomException(ErrorCode.USER_NOT_FOUND);
                    });
        }

        ContactMessage message = ContactMessage.builder()
                .user(user)
                .customerName(request.customerName())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .message(request.message())
                .status(ContactStatus.OPEN)
                .build();

        ContactMessage saved = contactMessageRepository.save(message);
        log.info("Contact message created successfully id={} status={}", saved.getId(), saved.getStatus());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getAll(Pageable pageable) {
        log.info("Admin fetching all contact messages page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return contactMessageRepository.findAllByOrderByStatusAscCreatedOnDesc(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getByUserId(Long userId, Pageable pageable) {
        log.info("Admin fetching contact messages for userId={} page={} size={}", userId, pageable.getPageNumber(), pageable.getPageSize());

        if (!userRepository.existsById(userId)) {
            log.warn("Contact messages by user failed because userId={} was not found", userId);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return contactMessageRepository.findByUserIdOrderByCreatedOnDesc(userId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getByStatus(ContactStatus status, Pageable pageable) {
        log.info("Admin fetching contact messages by status={} page={} size={}", status, pageable.getPageNumber(), pageable.getPageSize());
        return contactMessageRepository.findByStatusOrderByCreatedOnDesc(status, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ContactMessageResponse close(Long id) {
        log.info("Admin closing contact message id={}", id);

        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Close contact message failed because id={} was not found", id);
                    return new CustomException(ErrorCode.CONTACT_MESSAGE_NOT_FOUND);
                });

        if (message.getStatus() == ContactStatus.CLOSED) {
            log.info("Contact message id={} is already closed", id);
            return mapper.toResponse(message);
        }

        message.setStatus(ContactStatus.CLOSED);
        ContactMessage saved = contactMessageRepository.save(message);
        log.info("Contact message id={} closed successfully", saved.getId());
        return mapper.toResponse(saved);
    }
}

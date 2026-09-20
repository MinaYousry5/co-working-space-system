package com.workspace.booking.controller;

import com.workspace.booking.common.ApiResponse;
import com.workspace.booking.common.enums.ContactStatus;
import com.workspace.booking.dto.contact.ContactMessageCreateRequest;
import com.workspace.booking.dto.contact.ContactMessageResponse;
import com.workspace.booking.service.ContactMessageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact-us")
@RequiredArgsConstructor
@Slf4j
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping
    @Operation(summary = "Create contact-us message")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> create(@Valid @RequestBody ContactMessageCreateRequest request) {
        log.info("Received contact-us create request for email={}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(contactMessageService.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @Operation(summary = "Admin get all contact-us messages")
    public ResponseEntity<ApiResponse<Page<ContactMessageResponse>>> getAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(contactMessageService.getAll(pageable)));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @Operation(summary = "Admin get contact-us messages by user ID")
    public ResponseEntity<ApiResponse<Page<ContactMessageResponse>>> getByUserId(
            @PathVariable Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(contactMessageService.getByUserId(userId, pageable)));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @Operation(summary = "Admin get contact-us messages by status")
    public ResponseEntity<ApiResponse<Page<ContactMessageResponse>>> getByStatus(
            @RequestParam ContactStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(contactMessageService.getByStatus(status, pageable)));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @Operation(summary = "Admin close contact-us message")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contactMessageService.close(id)));
    }
}

package com.workspace.booking.controller;

import com.workspace.booking.common.ApiResponse;
import com.workspace.booking.common.enums.ManualDepositMethod;
import com.workspace.booking.common.enums.PaymentStatus;
import com.workspace.booking.common.enums.PaymentTransactionType;
import com.workspace.booking.dto.payment.PaymentRequest;
import com.workspace.booking.dto.payment.PaymentResponse;
import com.workspace.booking.dto.payment.PaymentStatusUpdateRequest;
import com.workspace.booking.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping(value = "/bookings/{bookingId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PaymentResponse>> submitPayment(
            @PathVariable Long bookingId,
            @Valid PaymentRequest request
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                paymentService.submitPayment(bookingId, request)
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getAll(pageable)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getByStatus(
            @PathVariable PaymentStatus status,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByStatus(status, pageable)));
    }

    @GetMapping("/transaction-type/{transactionType}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getByTransactionType(
            @PathVariable PaymentTransactionType transactionType,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByTransactionType(transactionType, pageable)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<PaymentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.updateStatus(id, request)));
    }
}

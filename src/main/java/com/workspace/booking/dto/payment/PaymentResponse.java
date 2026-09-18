package com.workspace.booking.dto.payment;

import com.workspace.booking.common.enums.ManualDepositMethod;
import com.workspace.booking.common.enums.PaymentStatus;
import com.workspace.booking.common.enums.PaymentTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long bookingId,
        String bookingRef,
        Long userId,
        String customerEmail,
        PaymentTransactionType transactionType,
        PaymentStatus status,
        ManualDepositMethod depositMethod,
        String paidToNumber,
        String senderNumber,
        String referenceCode,
        BigDecimal amount,
        BigDecimal refundAmount,
        String screenshotFilename,
        String screenshotBase64,
        String reasonOfReject,
        LocalDateTime adminDecisionAt,
        LocalDateTime createdOn
) {}

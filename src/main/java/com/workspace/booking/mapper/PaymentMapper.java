package com.workspace.booking.mapper;

import com.workspace.booking.dto.payment.PaymentResponse;
import com.workspace.booking.entity.finance.Payment;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        String base64Image = null;

        if (payment.getScreenshotBlob() != null && payment.getScreenshotBlob().length > 0) {
            String encoded = Base64.getEncoder().encodeToString(payment.getScreenshotBlob());
            base64Image = "data:" + payment.getScreenshotMimeType() + ";base64," + encoded;
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getBooking().getBookingRef(),
                payment.getUser().getId(),
                payment.getUser().getEmail(),
                payment.getTransactionType(),
                payment.getStatus(),
                payment.getDepositMethod(),
                payment.getPaidToNumber(),
                payment.getSenderNumber(),
                payment.getReferenceCode(),
                payment.getAmount(),
                payment.getRefundAmount(),
                payment.getScreenshotFilename(),
                base64Image,
                payment.getReasonOfReject(),
                payment.getAdminDecisionAt(),
                payment.getCreatedOn()
        );
    }
}

package com.workspace.booking.serviceimpl;

import com.workspace.booking.common.enums.ErrorCode;
import com.workspace.booking.common.exception.CustomException;
import com.workspace.booking.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.noreply}")
    private String noReplyEmail;

    @Override
    public void sendEmail(String to, String subject, String body) {

        if (to == null || to.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR, "Email is required");
        }

        try {
            log.info("Sending email to: {}", to);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(noReplyEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "EMAIL_SEND_FAILED");
        }
    }
}
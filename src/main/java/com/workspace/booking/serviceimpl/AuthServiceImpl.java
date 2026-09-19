package com.workspace.booking.serviceimpl;

import com.workspace.booking.common.ApiResponse;
import com.workspace.booking.common.enums.ErrorCode;
import com.workspace.booking.common.enums.TokenType;
import com.workspace.booking.common.exception.CustomException;
import com.workspace.booking.dto.AuthResponse;
import com.workspace.booking.dto.LoginRequest;
import com.workspace.booking.dto.RegisterRequest;
import com.workspace.booking.dto.ResetPasswordRequest;
import com.workspace.booking.entity.identity.AccessToken;
import com.workspace.booking.entity.identity.User;
import com.workspace.booking.repository.AccessTokenRepository;
import com.workspace.booking.repository.UserRepository;
import com.workspace.booking.security.CustomUserDetails;
import com.workspace.booking.security.JwtService;
import com.workspace.booking.service.AuthService;
import com.workspace.booking.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AccessTokenRepository accessTokenRepository;
    @Value("${server.ip}")
    private String ipPort;

    @Override
    public ApiResponse<?> login(LoginRequest request) {

        log.info("Login attempt for username={}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password attempt for user={}", request.getUsername());
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String token = jwtService.generateToken(new CustomUserDetails(user));

        log.info("Login successful for username={}", user.getUsername());

        return ApiResponse.success(new AuthResponse(user.getId(),token));
    }

    @Override
    public ApiResponse<Void> forgotPassword(String email) {

        log.info("Forgot password request for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = DigestUtils.sha256Hex(rawToken);

        AccessToken tokenEntity = new AccessToken();
        tokenEntity.setUser(user);
        tokenEntity.setTokenHash(hashedToken);
        tokenEntity.setTokenType(TokenType.PWD_RESET);
        tokenEntity.setExpireAt(LocalDateTime.now().plusMinutes(30));
        tokenEntity.setRevoked(0L);

        accessTokenRepository.save(tokenEntity);

        String forgetPasswordLink= "https://misa7a.seamrmoussa.workers.dev/forgot-password";
        String Mesa7a = "https://misa7a.seamrmoussa.workers.dev/forgot-password?token="+ rawToken;
        String localServer = "http://localhost:4200/forgot-password?token="+rawToken;

        String body =
                "Hello " + user.getFirstName() + ",\n\n" +
                        "You requested to reset your password.\n\n" +
                        "Click the link below to reset it:\n" +
                        Mesa7a + "\n\n" +
                        "another link for another server " + localServer + "\n\n" +
                        "This link will expire in 15 minutes.\n\n" +
                        "If you didn't request this, ignore this email.\n\n"+forgetPasswordLink;

        emailService.sendEmail(
                user.getEmail(),
                "Password Reset Request",
                body
        );

        log.info("Reset password email sent to {}", user.getEmail());

        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {

        String hashed = DigestUtils.sha256Hex(request.getToken());

        AccessToken token = accessTokenRepository
                .findByTokenHashAndTokenTypeAndRevoked(hashed, TokenType.PWD_RESET, 0)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_INVALID));

        if (token.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }

        User user = token.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setRevoked(1L);
        accessTokenRepository.save(token);

        log.info("Password reset successful for user={}", user.getUsername());

        return ApiResponse.success(null);
    }
}
package com.workspace.booking.repository;

import com.workspace.booking.common.enums.TokenType;
import com.workspace.booking.entity.identity.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {


    Optional<AccessToken> findByTokenHashAndTokenTypeAndRevoked(
            String tokenHash,
            TokenType tokenType,
            Integer revoked
    );

}

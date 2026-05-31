package com.assignment.task_tracker.service;

import com.assignment.task_tracker.entity.RefreshToken;
import com.assignment.task_tracker.entity.User;
import com.assignment.task_tracker.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(generateRefreshToken());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(180));

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            throw new AuthenticationServiceException("Refresh token is expired or revoked");
        }

        return refreshToken;
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }

    private String generateRefreshToken(){
        return UUID.randomUUID().toString();
    }
}

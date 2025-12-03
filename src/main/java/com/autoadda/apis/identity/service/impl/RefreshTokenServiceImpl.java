package com.autoadda.apis.identity.service.impl;

import com.autoadda.apis.identity.dao.RefreshTokenRepository;
import com.autoadda.apis.identity.entity.RefreshToken;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.exception.RefreshTokenExpiredException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;

    @Override
    public String generateRefreshToken(User user) {

        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setToken(refreshToken.getToken() + "exp:" + refreshToken.getExpiryDate().toEpochMilli());
        refreshTokenRepository.save(refreshToken);

        return refreshToken.getToken();
    }

    @Override
    public RefreshToken validateRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken).orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        return verifyExpiration(token);

    }

    @Transactional
    @Override
    public GenericResponse deleteRefreshTokenByUserId(Integer currentUserId) {
        refreshTokenRepository.deleteByUserId(currentUserId);
        return GenericResponse.builder().status("success").code("200").message("successfully logout").build();
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenExpiredException("Refresh token was expired");
        }
        return token;
    }

}

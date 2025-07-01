package com.platform.auth.service.impl;

import com.platform.auth.dao.RefreshTokenRepository;
import com.platform.auth.entity.RefreshToken;
import com.platform.auth.entity.User;
import com.platform.auth.exception.RefreshTokenExpiredException;
import com.platform.auth.exception.ResourceNotFoundException;
import com.platform.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

        refreshTokenRepository.findByUser ( user ).ifPresent ( refreshTokenRepository::delete );

        RefreshToken refreshToken = new RefreshToken ( );
        refreshToken.setUser ( user );
        refreshToken.setExpiryDate ( Instant.now ( ).plusMillis ( refreshTokenDurationMs ) );
        refreshToken.setToken ( UUID.randomUUID ( ).toString ( ) );
        refreshToken.setToken ( refreshToken.getToken ( ) + "exp:" + refreshToken.getExpiryDate ( ).toEpochMilli ( ) );
        refreshTokenRepository.save ( refreshToken );

        return refreshToken.getToken ( );
    }

    @Override
    public RefreshToken validateRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken ( refreshToken ).orElseThrow ( () -> new ResourceNotFoundException ( "Token not found" ) );
        return verifyExpiration ( token );

    }

    public RefreshToken verifyExpiration(RefreshToken token) {

        try {
            if ( token.getExpiryDate ( ).compareTo ( Instant.now ( ) ) < 0 ) {
                refreshTokenRepository.delete ( token );
                throw new RefreshTokenExpiredException ( "Refresh token was expired" );
            }
            return token;
        }

        catch ( RefreshTokenExpiredException e ) {
            throw new RefreshTokenExpiredException ( "Refresh token was expired" );
        }
    }

}

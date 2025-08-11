package com.service.identity.service;

import com.service.identity.entity.RefreshToken;
import com.service.identity.entity.User;
import com.service.identity.response.GenericResponse;

public interface RefreshTokenService {
    String generateRefreshToken(User user);

    RefreshToken validateRefreshToken(String refreshToken);

    GenericResponse deleteRefreshTokenByUserId(Integer currentUserId);
}

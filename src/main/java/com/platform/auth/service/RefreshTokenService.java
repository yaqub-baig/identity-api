package com.platform.auth.service;

import com.platform.auth.entity.RefreshToken;
import com.platform.auth.entity.User;
import com.platform.auth.response.GenericResponse;

public interface RefreshTokenService {
    String generateRefreshToken(User user);

    RefreshToken validateRefreshToken(String refreshToken);

    GenericResponse deleteRefreshTokenByUserId(Integer currentUserId);
}

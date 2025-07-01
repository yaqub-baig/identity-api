package com.platform.auth.service;

import com.platform.auth.entity.RefreshToken;
import com.platform.auth.entity.User;

public interface RefreshTokenService {
    String generateRefreshToken(User user);

    RefreshToken validateRefreshToken(String refreshToken);
}

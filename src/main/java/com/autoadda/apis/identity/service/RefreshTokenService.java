package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.entity.RefreshToken;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.response.GenericResponse;

public interface RefreshTokenService {
    String generateRefreshToken(User user);

    RefreshToken validateRefreshToken(String refreshToken);

    GenericResponse deleteRefreshTokenByUserId(Integer currentUserId);
}

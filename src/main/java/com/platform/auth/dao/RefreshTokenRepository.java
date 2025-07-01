package com.platform.auth.dao;

import com.platform.auth.entity.RefreshToken;
import com.platform.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByToken(String refreshToken);
}

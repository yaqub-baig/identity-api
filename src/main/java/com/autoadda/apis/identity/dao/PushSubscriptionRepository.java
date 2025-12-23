package com.autoadda.apis.identity.dao;

import com.autoadda.apis.identity.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository
        extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByRole(String role);

    Optional<PushSubscription> findByUserIdAndFcmTokenAndRole(String userId, String fcmToken, String role);

    List<PushSubscription> findByUserIdAndFcmToken(String userId, String fcmToken);

    List<PushSubscription> findByUserId(String userId);

    List<PushSubscription> findByUserIdAndRole(String userId, String role);

    void deleteByUserIdAndFcmToken(String userId, String fcmToken);

    void deleteByUserIdAndFcmTokenAndRole(String userId, String fcmToken, String role);
}

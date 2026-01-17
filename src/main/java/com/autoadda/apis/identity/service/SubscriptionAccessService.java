package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.dao.PlatformSubscriptionRepository;
import com.autoadda.apis.identity.entity.PlatformSubscription;
import com.autoadda.apis.identity.exception.UnauthorizedAccessException;
import com.autoadda.apis.identity.mapper.SubscriptionMapper;
import com.autoadda.apis.identity.response.SubscriptionStatusResponse;
import com.autoadda.apis.identity.util.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAccessService {

    private final PlatformSubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper mapper;

    @Transactional(readOnly = true)
    public boolean canAccessDashboard(EntityType entityType, Integer entityId) {
        return subscriptionRepository.hasActiveSubscription(entityType, entityId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public void checkAccess(EntityType entityType, Integer entityId) {
        if (!canAccessDashboard(entityType, entityId)) {
            throw new UnauthorizedAccessException(
                    "No active subscription found for " + entityType.name().toLowerCase() +
                    ". Please subscribe to access the dashboard.");
        }
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getAccessStatus(EntityType entityType, Integer entityId) {
        log.info("Checking access status for {} {}", entityType, entityId);

        Optional<PlatformSubscription> activeSubscription = subscriptionRepository
                .findActiveSubscription(entityType, entityId, LocalDate.now());

        SubscriptionStatusResponse response = mapper.toStatusResponse(activeSubscription.orElse(null));
        response.setEntityId(entityId);
        response.setEntityType(entityType);

        return response;
    }

    // ==================== User-based Access Methods (New Per-User Model) ====================

    @Transactional(readOnly = true)
    public boolean canAccessDashboard(Integer userId) {
        return subscriptionRepository.hasActiveSubscriptionByUserId(userId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public void checkUserAccess(Integer userId) {
        if (!canAccessDashboard(userId)) {
            throw new UnauthorizedAccessException("No active subscription. Please subscribe.");
        }
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getUserAccessStatus(Integer userId) {
        log.info("Checking access status for user {}", userId);

        Optional<PlatformSubscription> activeSubscription = subscriptionRepository
                .findActiveSubscriptionByUserId(userId, LocalDate.now());

        return mapper.toUserStatusResponse(activeSubscription.orElse(null), userId);
    }
}

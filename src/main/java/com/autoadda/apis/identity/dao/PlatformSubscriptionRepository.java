package com.autoadda.apis.identity.dao;

import com.autoadda.apis.identity.entity.PlatformSubscription;
import com.autoadda.apis.identity.util.EntityType;
import com.autoadda.apis.identity.util.PaymentStatus;
import com.autoadda.apis.identity.util.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformSubscriptionRepository extends JpaRepository<PlatformSubscription, Integer> {

    List<PlatformSubscription> findByEntityTypeAndEntityId(EntityType entityType, Integer entityId);

    List<PlatformSubscription> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(EntityType entityType, Integer entityId);

    List<PlatformSubscription> findByUserId(Integer userId);

    List<PlatformSubscription> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<PlatformSubscription> findByStatus(SubscriptionStatus status);

    List<PlatformSubscription> findByEntityTypeAndEntityIdAndStatus(
            EntityType entityType, Integer entityId, SubscriptionStatus status);

    @Query("SELECT s FROM PlatformSubscription s WHERE s.entityType = :entityType " +
           "AND s.entityId = :entityId AND s.status = 'ACTIVE' AND s.endDate >= :today")
    Optional<PlatformSubscription> findActiveSubscription(
            @Param("entityType") EntityType entityType,
            @Param("entityId") Integer entityId,
            @Param("today") LocalDate today);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM PlatformSubscription s " +
           "WHERE s.entityType = :entityType AND s.entityId = :entityId " +
           "AND s.status = 'ACTIVE' AND s.endDate >= :today")
    boolean hasActiveSubscription(
            @Param("entityType") EntityType entityType,
            @Param("entityId") Integer entityId,
            @Param("today") LocalDate today);

    @Query("SELECT s FROM PlatformSubscription s WHERE s.status = 'ACTIVE' " +
           "AND s.endDate >= :today AND s.endDate <= :futureDate")
    List<PlatformSubscription> findExpiringSubscriptions(
            @Param("today") LocalDate today,
            @Param("futureDate") LocalDate futureDate);

    @Query("SELECT s FROM PlatformSubscription s WHERE s.status = 'ACTIVE' AND s.endDate < :today")
    List<PlatformSubscription> findExpiredActiveSubscriptions(@Param("today") LocalDate today);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM PlatformSubscription s " +
           "WHERE s.entityType = :entityType AND s.entityId = :entityId AND s.isTrial = true")
    boolean hasUsedTrial(
            @Param("entityType") EntityType entityType,
            @Param("entityId") Integer entityId);

    Optional<PlatformSubscription> findByGatewayTransactionId(String transactionId);

    Optional<PlatformSubscription> findByGatewayOrderReference(String orderReference);

    @Query("SELECT s FROM PlatformSubscription s WHERE s.paymentStatus = :paymentStatus " +
           "AND s.paymentInitiatedAt < :expiryTime")
    List<PlatformSubscription> findByPaymentStatusAndPaymentInitiatedAtBefore(
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("expiryTime") LocalDateTime expiryTime);

    List<PlatformSubscription> findByPlanId(Integer planId);

    // ==================== User-based Queries (New Per-User Model) ====================

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM PlatformSubscription s " +
           "WHERE s.user.id = :userId AND s.status = 'ACTIVE' AND s.endDate >= :today")
    boolean hasActiveSubscriptionByUserId(@Param("userId") Integer userId, @Param("today") LocalDate today);

    @Query("SELECT s FROM PlatformSubscription s WHERE s.user.id = :userId " +
           "AND s.status = 'ACTIVE' AND s.endDate >= :today")
    Optional<PlatformSubscription> findActiveSubscriptionByUserId(@Param("userId") Integer userId, @Param("today") LocalDate today);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM PlatformSubscription s " +
           "WHERE s.user.id = :userId AND s.isTrial = true")
    boolean hasUsedTrialByUserId(@Param("userId") Integer userId);

    List<PlatformSubscription> findByUserIdAndStatus(Integer userId, SubscriptionStatus status);
}

package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.dao.PlatformSubscriptionRepository;
import com.autoadda.apis.identity.dao.SubscriptionPlanRepository;
import com.autoadda.apis.identity.dao.UserRepository;
import com.autoadda.apis.identity.entity.PlatformSubscription;
import com.autoadda.apis.identity.entity.SubscriptionPlan;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.exception.BadRequestException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.exception.UnauthorizedAccessException;
import com.autoadda.apis.identity.mapper.SubscriptionMapper;
import com.autoadda.apis.identity.request.ActivateSubscriptionRequest;
import com.autoadda.apis.identity.request.CreateSubscriptionRequest;
import com.autoadda.apis.identity.response.PlatformSubscriptionResponse;
import com.autoadda.apis.identity.response.SubscriptionStatusResponse;
import com.autoadda.apis.identity.util.EntityType;
import com.autoadda.apis.identity.util.PaymentStatus;
import com.autoadda.apis.identity.util.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSubscriptionService {

    private final PlatformSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionMapper mapper;

    private static final int FREE_TRIAL_DURATION_MONTHS = 1;

    // ==================== Admin Operations ====================

    @Transactional
    public PlatformSubscriptionResponse createSubscription(CreateSubscriptionRequest request, Integer adminId) {
        log.info("Admin {} creating subscription for entity {} ({})",
                adminId, request.getEntityId(), request.getEntityType());

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.getPlanId()));

        if (!plan.getIsActive()) {
            throw new BadRequestException("Cannot create subscription with inactive plan");
        }

        // For admin-created subscriptions, we need to find or create a user association
        // This will be linked when the subscription is activated
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminId));

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(plan.getDurationMonths());

        PlatformSubscription subscription = PlatformSubscription.builder()
                .user(admin) // Will be updated when proper user creates/claims it
                .entityId(request.getEntityId())
                .entityType(request.getEntityType())
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.PENDING)
                .paymentStatus(PaymentStatus.NOT_INITIATED)
                .paymentReference(request.getPaymentReference())
                .paymentAmount(request.getPaymentAmount())
                .notes(request.getNotes())
                .build();

        PlatformSubscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription created with ID: {} in PENDING status", saved.getId());

        return mapper.subscriptionToResponse(saved);
    }

    @Transactional
    public PlatformSubscriptionResponse activateSubscription(Integer subscriptionId,
                                                              ActivateSubscriptionRequest request,
                                                              Integer adminId) {
        log.info("Admin {} activating subscription {}", adminId, subscriptionId);

        PlatformSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Subscription is already active");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Cannot activate a cancelled subscription");
        }

        // Recalculate dates on activation
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(subscription.getPlan().getDurationMonths());

        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaymentStatus(PaymentStatus.SUCCESS);
        subscription.setActivatedBy(adminId);
        subscription.setActivatedAt(LocalDateTime.now());
        subscription.setPaymentReference(request.getPaymentReference());
        subscription.setPaymentAmount(request.getPaymentAmount());
        subscription.setPaymentCompletedAt(LocalDateTime.now());

        if (request.getNotes() != null) {
            subscription.setNotes(request.getNotes());
        }

        PlatformSubscription activated = subscriptionRepository.save(subscription);
        log.info("Subscription {} activated. Valid until {}", subscriptionId, endDate);

        return mapper.subscriptionToResponse(activated);
    }

    @Transactional
    public PlatformSubscriptionResponse cancelSubscription(Integer subscriptionId, Integer adminId, String reason) {
        log.info("Admin {} cancelling subscription {}", adminId, subscriptionId);

        PlatformSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setNotes(reason != null ? reason : subscription.getNotes());

        PlatformSubscription cancelled = subscriptionRepository.save(subscription);
        log.info("Subscription {} cancelled", subscriptionId);

        return mapper.subscriptionToResponse(cancelled);
    }

    @Transactional
    public PlatformSubscriptionResponse extendSubscription(Integer subscriptionId, Integer planId, Integer adminId) {
        log.info("Admin {} extending subscription {} with plan {}", adminId, subscriptionId, planId);

        PlatformSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        // Extend from current end date or today, whichever is later
        LocalDate newStartDate = subscription.getEndDate().isAfter(LocalDate.now())
                ? subscription.getEndDate()
                : LocalDate.now();
        LocalDate newEndDate = newStartDate.plusMonths(plan.getDurationMonths());

        subscription.setEndDate(newEndDate);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        PlatformSubscription extended = subscriptionRepository.save(subscription);
        log.info("Subscription {} extended until {}", subscriptionId, newEndDate);

        return mapper.subscriptionToResponse(extended);
    }

    @Transactional
    public PlatformSubscriptionResponse createTrialSubscription(EntityType entityType, Integer entityId, Integer userId) {
        log.info("Creating free trial for {} {}", entityType, entityId);

        if (subscriptionRepository.hasUsedTrial(entityType, entityId)) {
            throw new BadRequestException("Free trial has already been used for this " + entityType.name().toLowerCase());
        }

        if (hasActiveSubscription(entityType, entityId)) {
            throw new BadRequestException("Entity already has an active subscription");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Get first active plan for reference
        SubscriptionPlan plan = planRepository.findByIsActiveTrueOrderByDurationMonthsAsc()
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No active plans available. Cannot create trial."));

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(FREE_TRIAL_DURATION_MONTHS);

        PlatformSubscription trial = PlatformSubscription.builder()
                .user(user)
                .entityId(entityId)
                .entityType(entityType)
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .paymentStatus(PaymentStatus.SUCCESS)
                .isTrial(true)
                .activatedAt(LocalDateTime.now())
                .notes("1-month free trial - auto-created")
                .build();

        PlatformSubscription saved = subscriptionRepository.save(trial);
        log.info("Free trial created for {} {}. Valid until {}", entityType, entityId, endDate);

        return mapper.subscriptionToResponse(saved);
    }

    // ==================== Self-Service Operations ====================

    @Transactional
    public PlatformSubscriptionResponse createSelfServiceSubscription(CreateSubscriptionRequest request, Integer userId) {
        log.info("User {} creating subscription for {} {}",
                userId, request.getEntityType(), request.getEntityId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Check for existing active subscription
        if (hasActiveSubscription(request.getEntityType(), request.getEntityId())) {
            throw new BadRequestException("Entity already has an active subscription");
        }

        // Check for existing pending subscription
        List<PlatformSubscription> pending = subscriptionRepository
                .findByEntityTypeAndEntityIdAndStatus(
                        request.getEntityType(),
                        request.getEntityId(),
                        SubscriptionStatus.PENDING);

        if (!pending.isEmpty()) {
            throw new BadRequestException("A pending subscription already exists. Please complete payment or cancel it first.");
        }

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.getPlanId()));

        if (!plan.getIsActive()) {
            throw new BadRequestException("Selected plan is not active");
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(plan.getDurationMonths());

        PlatformSubscription subscription = PlatformSubscription.builder()
                .user(user)
                .entityId(request.getEntityId())
                .entityType(request.getEntityType())
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.PENDING)
                .paymentStatus(PaymentStatus.NOT_INITIATED)
                .paymentAmount(plan.getPrice())
                .notes("Self-service subscription - awaiting payment")
                .build();

        PlatformSubscription saved = subscriptionRepository.save(subscription);
        log.info("Self-service subscription created with ID: {}", saved.getId());

        return mapper.subscriptionToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PlatformSubscriptionResponse getPendingSubscriptionForPayment(EntityType entityType,
                                                                          Integer entityId,
                                                                          Integer userId) {
        List<PlatformSubscription> pending = subscriptionRepository
                .findByEntityTypeAndEntityIdAndStatus(entityType, entityId, SubscriptionStatus.PENDING);

        PlatformSubscription subscription = pending.stream()
                .filter(s -> s.getPaymentStatus() != PaymentStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No pending subscription found for payment"));

        // Verify ownership
        if (!subscription.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to access this subscription");
        }

        return mapper.subscriptionToResponse(subscription);
    }

    // ==================== Query Operations ====================

    @Transactional(readOnly = true)
    public List<PlatformSubscriptionResponse> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(mapper::subscriptionToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformSubscriptionResponse> getSubscriptionsByEntity(EntityType entityType, Integer entityId) {
        return subscriptionRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(mapper::subscriptionToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformSubscriptionResponse> getSubscriptionsByUser(Integer userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::subscriptionToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlatformSubscriptionResponse getSubscriptionById(Integer id) {
        PlatformSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
        return mapper.subscriptionToResponse(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(EntityType entityType, Integer entityId) {
        log.info("Getting subscription status for {} {}", entityType, entityId);

        Optional<PlatformSubscription> activeSubscription = subscriptionRepository
                .findActiveSubscription(entityType, entityId, LocalDate.now());

        SubscriptionStatusResponse response = mapper.toStatusResponse(activeSubscription.orElse(null));
        response.setEntityId(entityId);
        response.setEntityType(entityType);

        return response;
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(EntityType entityType, Integer entityId) {
        return subscriptionRepository.hasActiveSubscription(entityType, entityId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public boolean hasUsedTrial(EntityType entityType, Integer entityId) {
        return subscriptionRepository.hasUsedTrial(entityType, entityId);
    }

    @Transactional(readOnly = true)
    public List<PlatformSubscriptionResponse> getExpiringSubscriptions(int days) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);
        return subscriptionRepository.findExpiringSubscriptions(today, futureDate)
                .stream()
                .map(mapper::subscriptionToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformSubscriptionResponse> getExpiredSubscriptions() {
        return subscriptionRepository.findExpiredActiveSubscriptions(LocalDate.now())
                .stream()
                .map(mapper::subscriptionToResponse)
                .toList();
    }

    // ==================== User-based Operations (New Per-User Model) ====================

    @Transactional
    public PlatformSubscriptionResponse createUserSubscription(Integer planId, String notes, Integer userId) {
        log.info("User {} creating user-based subscription with plan {}", userId, planId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Check for existing active subscription
        if (hasActiveSubscriptionForUser(userId)) {
            throw new BadRequestException("User already has an active subscription");
        }

        // Check for existing pending subscription
        List<PlatformSubscription> pending = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.PENDING);
        if (!pending.isEmpty()) {
            throw new BadRequestException("A pending subscription already exists. Please complete payment or cancel it first.");
        }

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        if (!plan.getIsActive()) {
            throw new BadRequestException("Selected plan is not active");
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(plan.getDurationMonths());

        PlatformSubscription subscription = PlatformSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.PENDING)
                .paymentStatus(PaymentStatus.NOT_INITIATED)
                .paymentAmount(plan.getPrice())
                .notes(notes != null ? notes : "User subscription - awaiting payment")
                .build();

        PlatformSubscription saved = subscriptionRepository.save(subscription);
        log.info("User subscription created with ID: {} for user {}", saved.getId(), userId);

        return mapper.subscriptionToResponse(saved);
    }

    @Transactional
    public PlatformSubscriptionResponse createUserTrialSubscription(Integer userId) {
        log.info("Creating free trial for user {}", userId);

        if (subscriptionRepository.hasUsedTrialByUserId(userId)) {
            throw new BadRequestException("Free trial has already been used");
        }

        if (hasActiveSubscriptionForUser(userId)) {
            throw new BadRequestException("User already has an active subscription");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Get first active plan for reference
        SubscriptionPlan plan = planRepository.findByIsActiveTrueOrderByDurationMonthsAsc()
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No active plans available. Cannot create trial."));

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(FREE_TRIAL_DURATION_MONTHS);

        PlatformSubscription trial = PlatformSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .paymentStatus(PaymentStatus.SUCCESS)
                .isTrial(true)
                .activatedAt(LocalDateTime.now())
                .notes("1-month free trial - user subscription")
                .build();

        PlatformSubscription saved = subscriptionRepository.save(trial);
        log.info("Free trial created for user {}. Valid until {}", userId, endDate);

        return mapper.subscriptionToResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscriptionForUser(Integer userId) {
        return subscriptionRepository.hasActiveSubscriptionByUserId(userId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getUserSubscriptionStatus(Integer userId) {
        log.info("Getting subscription status for user {}", userId);

        Optional<PlatformSubscription> activeSubscription = subscriptionRepository
                .findActiveSubscriptionByUserId(userId, LocalDate.now());

        return mapper.toUserStatusResponse(activeSubscription.orElse(null), userId);
    }

    @Transactional(readOnly = true)
    public boolean hasUsedTrialForUser(Integer userId) {
        return subscriptionRepository.hasUsedTrialByUserId(userId);
    }

    // ==================== Scheduled Jobs ====================

    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    @Transactional
    public void expireSubscriptions() {
        log.info("Running subscription expiry job");

        List<PlatformSubscription> expired = subscriptionRepository
                .findExpiredActiveSubscriptions(LocalDate.now());

        for (PlatformSubscription subscription : expired) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            log.info("Expired subscription {} for {} {}",
                    subscription.getId(),
                    subscription.getEntityType(),
                    subscription.getEntityId());
        }

        log.info("Expired {} subscriptions", expired.size());
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void expireStalePaymentInitiations() {
        log.debug("Running payment expiry check");

        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(20); // 15 min + 5 buffer

        List<PlatformSubscription> stalePayments = subscriptionRepository
                .findByPaymentStatusAndPaymentInitiatedAtBefore(PaymentStatus.PENDING, expiryThreshold);

        for (PlatformSubscription subscription : stalePayments) {
            subscription.setPaymentStatus(PaymentStatus.EXPIRED);
            subscription.setNotes("Payment link expired");
            subscriptionRepository.save(subscription);
            log.info("Expired stale payment for subscription {}", subscription.getId());
        }
    }
}

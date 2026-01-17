package com.autoadda.apis.identity.controller;

import com.autoadda.apis.identity.request.ActivateSubscriptionRequest;
import com.autoadda.apis.identity.request.CreateSubscriptionRequest;
import com.autoadda.apis.identity.request.CreateUserSubscriptionRequest;
import com.autoadda.apis.identity.response.PlatformSubscriptionResponse;
import com.autoadda.apis.identity.response.SubscriptionStatusResponse;
import com.autoadda.apis.identity.service.PlatformSubscriptionService;
import com.autoadda.apis.identity.service.SubscriptionAccessService;
import com.autoadda.apis.identity.util.EntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Platform Subscriptions", description = "APIs for managing platform subscriptions")
public class SubscriptionController {

    private final PlatformSubscriptionService subscriptionService;
    private final SubscriptionAccessService accessService;

    // ==================== Admin Endpoints ====================

    @Operation(summary = "Create subscription for entity (Admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subscription created in PENDING status"),
            @ApiResponse(responseCode = "400", description = "Invalid request or plan inactive"),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    @PostMapping("/admin/create")
    public ResponseEntity<PlatformSubscriptionResponse> adminCreateSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @RequestHeader("X-Auto-Adda-Id") Integer adminId) {
        PlatformSubscriptionResponse subscription = subscriptionService.createSubscription(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @Operation(summary = "Activate subscription (Admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription activated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PostMapping("/admin/{id}/activate")
    public ResponseEntity<PlatformSubscriptionResponse> activateSubscription(
            @PathVariable Integer id,
            @Valid @RequestBody ActivateSubscriptionRequest request,
            @RequestHeader("X-Auto-Adda-Id") Integer adminId) {
        return ResponseEntity.ok(subscriptionService.activateSubscription(id, request, adminId));
    }

    @Operation(summary = "Cancel subscription (Admin)")
    @PutMapping("/admin/{id}/cancel")
    public ResponseEntity<PlatformSubscriptionResponse> cancelSubscription(
            @PathVariable Integer id,
            @RequestParam(required = false) String reason,
            @RequestHeader("X-Auto-Adda-Id") Integer adminId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(id, adminId, reason));
    }

    @Operation(summary = "Extend subscription (Admin)")
    @PutMapping("/admin/{id}/extend")
    public ResponseEntity<PlatformSubscriptionResponse> extendSubscription(
            @PathVariable Integer id,
            @RequestParam Integer planId,
            @RequestHeader("X-Auto-Adda-Id") Integer adminId) {
        return ResponseEntity.ok(subscriptionService.extendSubscription(id, planId, adminId));
    }

    @Operation(summary = "Get all subscriptions (Admin)")
    @GetMapping("/admin")
    public ResponseEntity<List<PlatformSubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @Operation(summary = "Get subscription by ID (Admin)")
    @GetMapping("/admin/{id}")
    public ResponseEntity<PlatformSubscriptionResponse> getSubscriptionById(@PathVariable Integer id) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
    }

    @Operation(summary = "Get subscriptions for entity (Admin)")
    @GetMapping("/admin/entity/{entityType}/{entityId}")
    public ResponseEntity<List<PlatformSubscriptionResponse>> getSubscriptionsByEntity(
            @PathVariable String entityType,
            @PathVariable Integer entityId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByEntity(
                EntityType.valueOf(entityType.toUpperCase()), entityId));
    }

    @Operation(summary = "Get expiring subscriptions (Admin)")
    @GetMapping("/admin/expiring")
    public ResponseEntity<List<PlatformSubscriptionResponse>> getExpiringSubscriptions(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(subscriptionService.getExpiringSubscriptions(days));
    }

    @Operation(summary = "Get expired subscriptions (Admin)")
    @GetMapping("/admin/expired")
    public ResponseEntity<List<PlatformSubscriptionResponse>> getExpiredSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getExpiredSubscriptions());
    }

    @Operation(summary = "Grant free trial (Admin)")
    @PostMapping("/admin/trial/{entityType}/{entityId}")
    public ResponseEntity<PlatformSubscriptionResponse> grantFreeTrial(
            @PathVariable String entityType,
            @PathVariable Integer entityId,
            @RequestHeader("X-Auto-Adda-Id") Integer adminId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                subscriptionService.createTrialSubscription(
                        EntityType.valueOf(entityType.toUpperCase()),
                        entityId,
                        adminId));
    }

    @Operation(summary = "Check if trial has been used (Admin)")
    @GetMapping("/admin/trial/check/{entityType}/{entityId}")
    public ResponseEntity<Map<String, Boolean>> checkTrialUsed(
            @PathVariable String entityType,
            @PathVariable Integer entityId) {
        boolean used = subscriptionService.hasUsedTrial(
                EntityType.valueOf(entityType.toUpperCase()), entityId);
        return ResponseEntity.ok(Map.of("hasUsedTrial", used));
    }

    // ==================== Self-Service Endpoints (Entity-based - Deprecated) ====================

    @Deprecated
    @Operation(summary = "Create subscription (Self-service)", deprecated = true,
               description = "Deprecated: Use POST /subscriptions/user/create for user-based subscriptions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subscription created in PENDING status"),
            @ApiResponse(responseCode = "400", description = "Already has active/pending subscription")
    })
    @PostMapping("/create")
    public ResponseEntity<PlatformSubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        PlatformSubscriptionResponse subscription = subscriptionService.createSelfServiceSubscription(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @Deprecated
    @Operation(summary = "Get pending subscription for payment", deprecated = true)
    @GetMapping("/pending")
    public ResponseEntity<PlatformSubscriptionResponse> getPendingSubscription(
            @RequestParam String entityType,
            @RequestParam Integer entityId,
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        return ResponseEntity.ok(subscriptionService.getPendingSubscriptionForPayment(
                EntityType.valueOf(entityType.toUpperCase()), entityId, userId));
    }

    @Deprecated
    @Operation(summary = "Get subscription status", deprecated = true,
               description = "Deprecated: Use GET /subscriptions/user/status for user-based subscriptions")
    @GetMapping("/status")
    public ResponseEntity<SubscriptionStatusResponse> getSubscriptionStatus(
            @RequestParam String entityType,
            @RequestParam Integer entityId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionStatus(
                EntityType.valueOf(entityType.toUpperCase()), entityId));
    }

    @Deprecated
    @Operation(summary = "Get subscription history", deprecated = true)
    @GetMapping("/history")
    public ResponseEntity<List<PlatformSubscriptionResponse>> getSubscriptionHistory(
            @RequestParam String entityType,
            @RequestParam Integer entityId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByEntity(
                EntityType.valueOf(entityType.toUpperCase()), entityId));
    }

    @Deprecated
    @Operation(summary = "Check dashboard access", deprecated = true,
               description = "Deprecated: Use GET /subscriptions/user/access for user-based subscriptions")
    @GetMapping("/access")
    public ResponseEntity<SubscriptionStatusResponse> checkAccess(
            @RequestParam String entityType,
            @RequestParam Integer entityId) {
        return ResponseEntity.ok(accessService.getAccessStatus(
                EntityType.valueOf(entityType.toUpperCase()), entityId));
    }

    @Deprecated
    @Operation(summary = "Check if has active subscription", deprecated = true,
               description = "Deprecated: Use GET /subscriptions/user/check for user-based subscriptions")
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> hasActiveSubscription(
            @RequestParam String entityType,
            @RequestParam Integer entityId) {
        boolean hasActive = subscriptionService.hasActiveSubscription(
                EntityType.valueOf(entityType.toUpperCase()), entityId);
        return ResponseEntity.ok(Map.of("hasActiveSubscription", hasActive));
    }

    // ==================== User-based Endpoints (New Per-User Model) ====================

    @Operation(summary = "Create subscription for user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subscription created in PENDING status"),
            @ApiResponse(responseCode = "400", description = "Already has active/pending subscription")
    })
    @PostMapping("/user/create")
    public ResponseEntity<PlatformSubscriptionResponse> createUserSubscription(
            @Valid @RequestBody CreateUserSubscriptionRequest request,
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        PlatformSubscriptionResponse subscription = subscriptionService.createUserSubscription(
                request.getPlanId(), request.getNotes(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @Operation(summary = "Start 1-month free trial for user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Trial created and activated"),
            @ApiResponse(responseCode = "400", description = "Trial already used or has active subscription")
    })
    @PostMapping("/user/trial")
    public ResponseEntity<PlatformSubscriptionResponse> createUserTrial(
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        PlatformSubscriptionResponse subscription = subscriptionService.createUserTrialSubscription(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @Operation(summary = "Get user's subscription status")
    @GetMapping("/user/status")
    public ResponseEntity<SubscriptionStatusResponse> getUserSubscriptionStatus(
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        return ResponseEntity.ok(subscriptionService.getUserSubscriptionStatus(userId));
    }

    @Operation(summary = "Check if user has active subscription")
    @GetMapping("/user/check")
    public ResponseEntity<Map<String, Boolean>> hasUserActiveSubscription(
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        boolean hasActive = subscriptionService.hasActiveSubscriptionForUser(userId);
        return ResponseEntity.ok(Map.of("hasActiveSubscription", hasActive));
    }

    @Operation(summary = "Check user's dashboard access")
    @GetMapping("/user/access")
    public ResponseEntity<SubscriptionStatusResponse> checkUserAccess(
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        return ResponseEntity.ok(accessService.getUserAccessStatus(userId));
    }

    @Operation(summary = "Check if user has used trial")
    @GetMapping("/user/trial/check")
    public ResponseEntity<Map<String, Boolean>> hasUserUsedTrial(
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        boolean used = subscriptionService.hasUsedTrialForUser(userId);
        return ResponseEntity.ok(Map.of("hasUsedTrial", used));
    }
}

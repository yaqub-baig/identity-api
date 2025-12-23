package com.autoadda.apis.identity.controller;

import com.autoadda.apis.identity.dao.PushSubscriptionRepository;
import com.autoadda.apis.identity.entity.PushSubscription;
import com.autoadda.apis.identity.request.*;
import com.autoadda.apis.identity.service.NotificationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class PushSubscriptionController {

    public static final String SUCCESS = "success";
    public static final String MESSAGE = "message";
    private final PushSubscriptionRepository repository;
    private final NotificationDispatcher notificationDispatcher;

    public PushSubscriptionController(PushSubscriptionRepository repository,
                                      NotificationDispatcher notificationDispatcher) {
        this.repository = repository;
        this.notificationDispatcher = notificationDispatcher;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody SubscriptionRequest request) {
        log.info("Subscription request received for user: {} with role: {}",
                request.getUserId(), request.getRole());

        try {
            // Check for duplicate subscription (now includes role)
            Optional<PushSubscription> existing = repository.findByUserIdAndFcmTokenAndRole(
                    request.getUserId(), request.getFcmToken(), request.getRole());

            PushSubscription subscription;
            if (existing.isPresent()) {
                // Update existing subscription timestamp
                subscription = existing.get();
                subscription.setSubscribedAt(LocalDateTime.now());
                subscription.setDeviceInfo(request.getDeviceInfo());
                log.info("Updated existing subscription for user: {} role: {}",
                        request.getUserId(), request.getRole());
            } else {
                // Create new subscription for this role
                subscription = new PushSubscription();
                subscription.setUserId(request.getUserId());
                subscription.setRole(request.getRole());
                subscription.setFcmToken(request.getFcmToken());
                subscription.setDeviceInfo(request.getDeviceInfo());
                subscription.setSubscribedAt(LocalDateTime.now());
                log.info("Created new subscription for user: {} role: {}",
                        request.getUserId(), request.getRole());
            }

            subscription = repository.save(subscription);

            Map<String, Object> response = new HashMap<>();
            response.put(SUCCESS, true);
            response.put("subscriptionId", subscription.getId());
            response.put(MESSAGE, "Subscription saved successfully");
            response.put("role", request.getRole());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to save subscription for user: {} role: {}",
                    request.getUserId(), request.getRole(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS, false);
            errorResponse.put(MESSAGE, "Failed to save subscription: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody UnsubscribeRequest request) {
        log.info("Unsubscribe ALL roles request for user: {}", request.getUserId());

        try {
            repository.deleteByUserIdAndFcmToken(request.getUserId(), request.getFcmToken());

            Map<String, Object> response = new HashMap<>();
            response.put(SUCCESS, true);
            response.put(MESSAGE, "Unsubscribed from all roles successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to unsubscribe user: {}", request.getUserId(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS, false);
            errorResponse.put(MESSAGE, "Failed to unsubscribe: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/unsubscribe-role")
    @Transactional
    public ResponseEntity<Map<String, Object>> unsubscribeRole(@RequestBody UnsubscribeRoleRequest request) {
        log.info("Unsubscribe from role request for user: {} role: {}",
                request.getUserId(), request.getRole());

        try {
            repository.deleteByUserIdAndFcmTokenAndRole(
                    request.getUserId(), request.getFcmToken(), request.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put(SUCCESS, true);
            response.put(MESSAGE, "Unsubscribed from role: " + request.getRole());
            response.put("role", request.getRole());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to unsubscribe user: {} from role: {}",
                    request.getUserId(), request.getRole(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS, false);
            errorResponse.put(MESSAGE, "Failed to unsubscribe from role: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/subscriptions/{userId}")
    public ResponseEntity<List<PushSubscription>> getUserSubscriptions(@PathVariable String userId) {
        log.info("Fetching subscriptions for user: {}", userId);
        List<PushSubscription> subscriptions = repository.findByUserId(userId);
        return ResponseEntity.ok(subscriptions);
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    public ResponseEntity<Map<String, Object>> deleteSubscription(@PathVariable Long subscriptionId) {
        log.info("Deleting subscription: {}", subscriptionId);

        try {
            if (repository.existsById(subscriptionId)) {
                repository.deleteById(subscriptionId);
                Map<String, Object> response = new HashMap<>();
                response.put(SUCCESS, true);
                response.put(MESSAGE, "Subscription deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(SUCCESS, false);
                errorResponse.put(MESSAGE, "Subscription not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Failed to delete subscription: {}", subscriptionId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS, false);
            errorResponse.put(MESSAGE, "Failed to delete subscription: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testNotification(@RequestBody TestNotificationRequest request) {
        log.info("Test notification request for role: {}", request.getRole());
        try {
            notificationDispatcher.notifyRole(
                    request.getRole(),
                    request.getTitle(),
                    request.getBody(),
                    request.getUrl()
            );
            Map<String, Object> response = new HashMap<>();
            response.put(SUCCESS, true);
            response.put(MESSAGE, "Notification sent to role: " + request.getRole());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send test notification", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS, false);
            errorResponse.put(MESSAGE, "Failed to send notification: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody SendNotificationRequest request) {
        log.info("Send notification request - userId: {}, role: {}",
                request.getUserId(), request.getRole());

        try {
            if (request.getUserId() == null && request.getRole() != null && !request.getRole().isEmpty()) {
                // Send to ALL users with specific role
                notificationDispatcher.notifyRole(
                        request.getRole(),
                        request.getTitle(),
                        request.getBody(),
                        request.getUrl()
                );
            } else if (request.getUserId() != null && request.getRole() != null && !request.getRole().isEmpty()) {
                // Send to specific user + role
                notificationDispatcher.notifyUserByRole(
                        request.getUserId(),
                        request.getRole(),
                        request.getTitle(),
                        request.getBody(),
                        request.getUrl()
                );
            } else if (request.getUserId() != null) {
                // Send to specific user (all roles)
                notificationDispatcher.notifyUser(
                        request.getUserId(),
                        request.getTitle(),
                        request.getBody(),
                        request.getUrl()
                );
            }

            Map<String, Object> response = new HashMap<>();
            response.put(SUCCESS, true);
            response.put(MESSAGE, "Notification sent successfully");
            response.put("userId", request.getUserId());
            if (request.getRole() != null && !request.getRole().isEmpty()) {
                response.put("role", request.getRole());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS, false);
            errorResponse.put(MESSAGE, "Failed to send notification: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

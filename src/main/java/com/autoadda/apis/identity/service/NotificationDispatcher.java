package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.dao.PushSubscriptionRepository;
import com.autoadda.apis.identity.entity.PushSubscription;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class NotificationDispatcher {

    private final PushSubscriptionRepository repository;
    private final FcmNotificationService fcmService;

    public NotificationDispatcher(
        PushSubscriptionRepository repository,
        FcmNotificationService fcmService) {
        this.repository = repository;
        this.fcmService = fcmService;
    }

    public void notifyRole(String role, String title, String body, String url) {
        log.info("Sending notification to role: {} - Title: {}", role, title);

        List<PushSubscription> subs = repository.findByRole(role);

        if (subs.isEmpty()) {
            log.warn("No subscriptions found for role: {}", role);
            return;
        }

        int sentCount = 0;
        int failedCount = 0;

        for (PushSubscription sub : subs) {
            try {
                fcmService.sendNotification(
                    sub.getFcmToken(), title, body, url);
                sentCount++;
                log.debug("Notification sent successfully to user: {}", sub.getUserId());
            } catch (FirebaseMessagingException e) {
                failedCount++;
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                    e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    // Token is invalid or unregistered, remove from DB
                    log.warn("Removing invalid FCM token for user {}: {}", sub.getUserId(), e.getMessage());
                    repository.delete(sub);
                } else {
                    log.error("Failed to send notification to user {}: {} - Error: {}",
                            sub.getUserId(), e.getMessagingErrorCode(), e.getMessage());
                }
            } catch (Exception e) {
                failedCount++;
                log.error("Unexpected error sending notification to user {}: {}",
                        sub.getUserId(), e.getMessage(), e);
            }
        }

        log.info("Notification dispatch complete for role: {} - Sent: {}, Failed: {}, Total: {}",
                role, sentCount, failedCount, subs.size());
    }

    public void notifyUser(String userId, String title, String body, String url) {
        log.info("Sending notification to user: {} - Title: {}", userId, title);

        List<PushSubscription> subs = repository.findByUserId(userId);

        if (subs.isEmpty()) {
            log.warn("No subscriptions found for user: {}", userId);
            return;
        }

        int sentCount = 0;
        int failedCount = 0;

        for (PushSubscription sub : subs) {
            try {
                fcmService.sendNotification(
                    sub.getFcmToken(), title, body, url);
                sentCount++;
                log.debug("Notification sent successfully to device: {}", sub.getDeviceInfo());
            } catch (FirebaseMessagingException e) {
                failedCount++;
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                    e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.warn("Removing invalid FCM token for user {} device {}: {}",
                            userId, sub.getDeviceInfo(), e.getMessage());
                    repository.delete(sub);
                } else {
                    log.error("Failed to send notification to user {} device {}: {} - Error: {}",
                            userId, sub.getDeviceInfo(), e.getMessagingErrorCode(), e.getMessage());
                }
            } catch (Exception e) {
                failedCount++;
                log.error("Unexpected error sending notification to user {} device {}: {}",
                        userId, sub.getDeviceInfo(), e.getMessage(), e);
            }
        }

        log.info("Notification dispatch complete for user: {} - Sent: {}, Failed: {}, Total devices: {}",
                userId, sentCount, failedCount, subs.size());
    }

    public void notifyUserByRole(String userId, String role, String title, String body, String url) {
        log.info("Sending notification to user: {} with role: {} - Title: {}", userId, role, title);

        List<PushSubscription> subs = repository.findByUserIdAndRole(userId, role);

        if (subs.isEmpty()) {
            log.warn("No subscriptions found for user: {} with role: {}", userId, role);
            return;
        }

        int sentCount = 0;
        int failedCount = 0;

        for (PushSubscription sub : subs) {
            try {
                fcmService.sendNotification(sub.getFcmToken(), title, body, url);
                sentCount++;
                log.debug("Notification sent successfully to user: {} role: {}", userId, role);
            } catch (FirebaseMessagingException e) {
                failedCount++;
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                    e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.warn("Removing invalid FCM token for user {} role {}: {}",
                            userId, role, e.getMessage());
                    repository.delete(sub);
                } else {
                    log.error("Failed to send notification to user {} role {}: {} - Error: {}",
                            userId, role, e.getMessagingErrorCode(), e.getMessage());
                }
            } catch (Exception e) {
                failedCount++;
                log.error("Unexpected error sending notification to user {} role {}: {}",
                        userId, role, e.getMessage(), e);
            }
        }

        log.info("Notification dispatch complete for user: {} role: {} - Sent: {}, Failed: {}, Total: {}",
                userId, role, sentCount, failedCount, subs.size());
    }
}

package com.autoadda.apis.identity.mapper;

import com.autoadda.apis.identity.entity.PlatformSubscription;
import com.autoadda.apis.identity.entity.SubscriptionPlan;
import com.autoadda.apis.identity.response.PlatformSubscriptionResponse;
import com.autoadda.apis.identity.response.SubscriptionPlanResponse;
import com.autoadda.apis.identity.response.SubscriptionStatusResponse;
import com.autoadda.apis.identity.util.SubscriptionStatus;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public SubscriptionPlanResponse planToResponse(SubscriptionPlan plan) {
        if (plan == null) {
            return null;
        }

        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .durationMonths(plan.getDurationMonths())
                .price(plan.getPrice())
                .description(plan.getDescription())
                .applicableTo(plan.getApplicableTo())
                .isActive(plan.getIsActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    public PlatformSubscriptionResponse subscriptionToResponse(PlatformSubscription subscription) {
        if (subscription == null) {
            return null;
        }

        long daysRemaining = subscription.getDaysRemaining();
        boolean isExpiringSoon = daysRemaining <= 7 && daysRemaining > 0;

        String userName = null;
        if (subscription.getUser() != null) {
            String firstName = subscription.getUser().getFirstName();
            String lastName = subscription.getUser().getLastName();
            userName = (firstName != null ? firstName : "") +
                       (lastName != null ? " " + lastName : "");
            userName = userName.trim();
        }

        return PlatformSubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser() != null ? subscription.getUser().getId() : null)
                .userName(userName)
                .entityId(subscription.getEntityId())
                .entityType(subscription.getEntityType())
                .plan(planToResponse(subscription.getPlan()))
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .paymentStatus(subscription.getPaymentStatus())
                .activatedBy(subscription.getActivatedBy())
                .activatedAt(subscription.getActivatedAt())
                .paymentReference(subscription.getPaymentReference())
                .paymentAmount(subscription.getPaymentAmount())
                .notes(subscription.getNotes())
                .isTrial(subscription.getIsTrial())
                .daysRemaining(daysRemaining)
                .isExpiringSoon(isExpiringSoon)
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    public SubscriptionStatusResponse toStatusResponse(PlatformSubscription subscription) {
        if (subscription == null) {
            return SubscriptionStatusResponse.builder()
                    .hasActiveSubscription(false)
                    .status("NO_SUBSCRIPTION")
                    .message("No subscription found. Please subscribe to access the dashboard.")
                    .build();
        }

        long daysRemaining = subscription.getDaysRemaining();
        boolean isExpiringSoon = daysRemaining <= 7 && daysRemaining > 0;
        boolean isActive = subscription.getStatus() == SubscriptionStatus.ACTIVE && !subscription.isExpired();

        String message = buildStatusMessage(subscription, daysRemaining, isExpiringSoon);

        return SubscriptionStatusResponse.builder()
                .entityId(subscription.getEntityId())
                .entityType(subscription.getEntityType())
                .hasActiveSubscription(isActive)
                .status(subscription.getStatus().name())
                .planName(subscription.getPlan() != null ? subscription.getPlan().getName() : null)
                .isTrial(subscription.getIsTrial())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .daysRemaining(daysRemaining)
                .isExpiringSoon(isExpiringSoon)
                .message(message)
                .build();
    }

    public SubscriptionStatusResponse toUserStatusResponse(PlatformSubscription subscription, Integer userId) {
        if (subscription == null) {
            return SubscriptionStatusResponse.builder()
                    .userId(userId)
                    .hasActiveSubscription(false)
                    .status("NO_SUBSCRIPTION")
                    .message("No subscription found. Please subscribe to access the dashboard.")
                    .build();
        }

        long daysRemaining = subscription.getDaysRemaining();
        boolean isExpiringSoon = daysRemaining <= 7 && daysRemaining > 0;
        boolean isActive = subscription.getStatus() == SubscriptionStatus.ACTIVE && !subscription.isExpired();

        String message = buildStatusMessage(subscription, daysRemaining, isExpiringSoon);

        return SubscriptionStatusResponse.builder()
                .userId(userId)
                .hasActiveSubscription(isActive)
                .status(subscription.getStatus().name())
                .planName(subscription.getPlan() != null ? subscription.getPlan().getName() : null)
                .isTrial(subscription.getIsTrial())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .daysRemaining(daysRemaining)
                .isExpiringSoon(isExpiringSoon)
                .message(message)
                .build();
    }

    private String buildStatusMessage(PlatformSubscription subscription, long daysRemaining, boolean isExpiringSoon) {
        boolean isTrial = Boolean.TRUE.equals(subscription.getIsTrial());
        SubscriptionStatus status = subscription.getStatus();

        if (status == SubscriptionStatus.ACTIVE) {
            if (subscription.isExpired()) {
                return isTrial
                        ? "Your free trial has expired. Please subscribe to continue using the dashboard."
                        : "Your subscription has expired. Please renew to continue using the dashboard.";
            }
            if (isExpiringSoon) {
                return isTrial
                        ? String.format("Your free trial expires in %d day(s). Subscribe now to continue.", daysRemaining)
                        : String.format("Your subscription expires in %d day(s). Please renew soon.", daysRemaining);
            }
            return isTrial
                    ? String.format("Your free trial is active. %d day(s) remaining.", daysRemaining)
                    : String.format("Your subscription is active. %d day(s) remaining.", daysRemaining);
        }

        if (status == SubscriptionStatus.EXPIRED) {
            return isTrial
                    ? "Your free trial has expired. Please subscribe to continue using the dashboard."
                    : "Your subscription has expired. Please renew to continue using the dashboard.";
        }

        if (status == SubscriptionStatus.CANCELLED) {
            return "Your subscription has been cancelled.";
        }

        if (status == SubscriptionStatus.PENDING) {
            return "Your subscription is pending activation. Please complete payment.";
        }

        return "Subscription status: " + status.name();
    }
}

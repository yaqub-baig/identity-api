package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.adopter.PaymentGatewayAdaptor;
import com.autoadda.apis.identity.config.PaymentGatewayConfig;
import com.autoadda.apis.identity.dao.PlatformSubscriptionRepository;
import com.autoadda.apis.identity.entity.PlatformSubscription;
import com.autoadda.apis.identity.exception.BadRequestException;
import com.autoadda.apis.identity.exception.PaymentGatewayException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.exception.UnauthorizedAccessException;
import com.autoadda.apis.identity.request.InitiatePaymentRequest;
import com.autoadda.apis.identity.request.PaymentCallbackRequest;
import com.autoadda.apis.identity.response.PaymentInitiationResponse;
import com.autoadda.apis.identity.response.payment.PaymentGatewayResponse;
import com.autoadda.apis.identity.util.PaymentStatus;
import com.autoadda.apis.identity.util.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

    private final PaymentGatewayAdaptor paymentGatewayAdaptor;
    private final PaymentGatewayConfig config;
    private final PlatformSubscriptionRepository subscriptionRepository;

    @Transactional
    public PaymentInitiationResponse initiatePayment(InitiatePaymentRequest request, Integer userId) {
        log.info("Initiating payment for subscription {} by user {}", request.getSubscriptionId(), userId);

        // Find subscription
        PlatformSubscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription not found: " + request.getSubscriptionId()));

        // Verify ownership
        if (!subscription.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to pay for this subscription");
        }

        // Validate status
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new BadRequestException("Only PENDING subscriptions can be paid for");
        }

        if (subscription.getPaymentStatus() == PaymentStatus.PENDING) {
            throw new BadRequestException("Payment already initiated. Please complete or wait for expiry.");
        }

        if (subscription.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Payment already completed for this subscription.");
        }

        // Generate order reference
        String orderReference = paymentGatewayAdaptor.generateOrderReference(subscription.getId());

        // Call payment gateway
        PaymentGatewayResponse gatewayResponse = paymentGatewayAdaptor.createPayment(subscription, orderReference);

        if (!gatewayResponse.getSuccess()) {
            log.error("Payment gateway returned error: {}", gatewayResponse.getMessage());
            throw new PaymentGatewayException("Payment initiation failed: " + gatewayResponse.getMessage());
        }

        // Update subscription with payment details
        subscription.setGatewayTransactionId(gatewayResponse.getTransactionId());
        subscription.setGatewayOrderReference(orderReference);
        subscription.setPaymentInitiatedAt(LocalDateTime.now());
        subscription.setPaymentStatus(PaymentStatus.PENDING);
        subscriptionRepository.save(subscription);

        log.info("Payment initiated successfully. Transaction ID: {}", gatewayResponse.getTransactionId());

        return PaymentInitiationResponse.builder()
                .subscriptionId(subscription.getId())
                .transactionId(gatewayResponse.getTransactionId())
                .qrCodeImage(gatewayResponse.getQrCodeImage())
                .paymentUrl(gatewayResponse.getPaymentUrl())
                .amount(subscription.getPlan().getPrice())
                .currency(config.getCurrency())
                .expiryMinutes(config.getExpiryMinutes())
                .status("INITIATED")
                .message("Scan QR code or click payment link to complete payment")
                .build();
    }

    @Transactional
    public void handlePaymentCallback(PaymentCallbackRequest callback) {
        log.info("Received payment callback. Transaction: {}, Status: {}",
                callback.getTransactionId(), callback.getStatus());

        // Find subscription by order reference or transaction ID
        PlatformSubscription subscription = findSubscriptionByCallback(callback);

        // Verify transaction ID matches
        if (callback.getTransactionId() != null &&
            !callback.getTransactionId().equals(subscription.getGatewayTransactionId())) {
            log.error("Transaction ID mismatch. Expected: {}, Received: {}",
                    subscription.getGatewayTransactionId(), callback.getTransactionId());
            throw new BadRequestException("Transaction ID mismatch");
        }

        // Handle based on status
        String status = callback.getStatus() != null ? callback.getStatus().toUpperCase() : "";
        switch (status) {
            case "SUCCESS":
            case "PAID":
            case "COMPLETED":
                handleSuccessfulPayment(subscription, callback);
                break;
            case "FAILED":
            case "FAILURE":
                handleFailedPayment(subscription, callback);
                break;
            case "CANCELLED":
            case "CANCELED":
                handleCancelledPayment(subscription);
                break;
            default:
                log.warn("Unknown payment status: {}", callback.getStatus());
        }
    }

    @Transactional
    public String handleBrowserRedirect(String orderReference, String status, String transactionId) {
        log.info("Browser redirect received. OrderRef: {}, Status: {}", orderReference, status);

        PlatformSubscription subscription = subscriptionRepository.findByGatewayOrderReference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription not found for order: " + orderReference));

        // Update status based on redirect params
        String normalizedStatus = status != null ? status.toUpperCase() : "";

        if ("SUCCESS".equals(normalizedStatus) || "PAID".equals(normalizedStatus)) {
            if (subscription.getPaymentStatus() != PaymentStatus.SUCCESS) {
                // Activate if not already activated
                activateSubscription(subscription, transactionId);
            }
            return config.getFrontendSuccessUrl() + "?subscriptionId=" + subscription.getId();
        } else {
            if (subscription.getPaymentStatus() == PaymentStatus.PENDING) {
                subscription.setPaymentStatus(PaymentStatus.FAILED);
                subscription.setNotes("Payment failed via browser redirect");
                subscriptionRepository.save(subscription);
            }
            return config.getFrontendFailureUrl() + "?subscriptionId=" + subscription.getId();
        }
    }

    private PlatformSubscription findSubscriptionByCallback(PaymentCallbackRequest callback) {
        // Try to find by order reference first
        if (callback.getOrderReference() != null) {
            return subscriptionRepository.findByGatewayOrderReference(callback.getOrderReference())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Subscription not found for order: " + callback.getOrderReference()));
        }

        // Then try by transaction ID
        if (callback.getTransactionId() != null) {
            return subscriptionRepository.findByGatewayTransactionId(callback.getTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Subscription not found for transaction: " + callback.getTransactionId()));
        }

        throw new BadRequestException("Either orderReference or transactionId must be provided");
    }

    private void handleSuccessfulPayment(PlatformSubscription subscription, PaymentCallbackRequest callback) {
        log.info("Processing successful payment for subscription {}", subscription.getId());

        if (subscription.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already processed for subscription {}", subscription.getId());
            return;
        }

        activateSubscription(subscription, callback.getTransactionId());

        // TODO: Send confirmation notification to user
    }

    private void activateSubscription(PlatformSubscription subscription, String transactionId) {
        subscription.setPaymentStatus(PaymentStatus.SUCCESS);
        subscription.setPaymentCompletedAt(LocalDateTime.now());
        subscription.setPaymentReference(transactionId);

        // Activate subscription
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(subscription.getPlan().getDurationMonths());

        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setActivatedAt(LocalDateTime.now());
        subscription.setNotes("Auto-activated via payment gateway");

        subscriptionRepository.save(subscription);
        log.info("Subscription {} activated successfully. Valid until {}", subscription.getId(), endDate);
    }

    private void handleFailedPayment(PlatformSubscription subscription, PaymentCallbackRequest callback) {
        log.warn("Payment failed for subscription {}. Error: {}",
                subscription.getId(), callback.getErrorMessage());

        subscription.setPaymentStatus(PaymentStatus.FAILED);
        subscription.setNotes("Payment failed: " +
                (callback.getErrorMessage() != null ? callback.getErrorMessage() : "Unknown error"));
        subscriptionRepository.save(subscription);

        // TODO: Send failure notification to user
    }

    private void handleCancelledPayment(PlatformSubscription subscription) {
        log.info("Payment cancelled for subscription {}", subscription.getId());

        subscription.setPaymentStatus(PaymentStatus.CANCELLED);
        subscription.setNotes("Payment cancelled by user");
        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public PaymentStatus getPaymentStatus(Integer subscriptionId, Integer userId) {
        PlatformSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + subscriptionId));

        // Verify ownership
        if (!subscription.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to view this subscription");
        }

        return subscription.getPaymentStatus();
    }
}

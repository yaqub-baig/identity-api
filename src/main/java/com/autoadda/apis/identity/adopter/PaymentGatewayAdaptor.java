package com.autoadda.apis.identity.adopter;

import com.autoadda.apis.identity.config.PaymentGatewayConfig;
import com.autoadda.apis.identity.entity.PlatformSubscription;
import com.autoadda.apis.identity.entity.SubscriptionPlan;
import com.autoadda.apis.identity.exception.PaymentGatewayException;
import com.autoadda.apis.identity.request.payment.PaymentGatewayRequest;
import com.autoadda.apis.identity.response.payment.PaymentGatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PaymentGatewayAdaptor {

    private final RestTemplate restTemplate;
    private final PaymentGatewayConfig config;

    public PaymentGatewayAdaptor(RestTemplate restTemplate, PaymentGatewayConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 10000)
    )
    public PaymentGatewayResponse createPayment(PlatformSubscription subscription, String orderReference) {
        log.info("Initiating payment for subscription {} with order reference {}",
                subscription.getId(), orderReference);

        SubscriptionPlan plan = subscription.getPlan();
        String callbackUrl = config.getCallbackUrl() + "?orderRef=" + orderReference;

        PaymentGatewayRequest request = buildPaymentRequest(subscription, plan, orderReference, callbackUrl);

        String url = config.getBaseUrl() + config.getCreateEndpoint();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PaymentGatewayRequest> entity = new HttpEntity<>(request, headers);

            log.debug("Calling payment gateway: {} with payload: {}", url, request);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                return parseGatewayResponse(body);
            } else {
                log.error("Payment gateway returned error status: {}", response.getStatusCode());
                throw new PaymentGatewayException("Payment gateway returned error: " + response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("Failed to call payment gateway: {}", e.getMessage(), e);
            throw new PaymentGatewayException("Failed to connect to payment gateway: " + e.getMessage(), e);
        }
    }

    private PaymentGatewayRequest buildPaymentRequest(PlatformSubscription subscription,
                                                       SubscriptionPlan plan,
                                                       String orderReference,
                                                       String callbackUrl) {
        return PaymentGatewayRequest.builder()
                .subject("AutoAdda Subscription - " + plan.getName())
                .paySceneCode(config.getPaySceneCode())
                .payChannelCode(config.getPayChannelCode())
                .platformType(config.getPlatformType())
                .amount(plan.getPrice())
                .goodsDetail(PaymentGatewayRequest.GoodsDetail.builder()
                        .body("AutoAdda Platform Subscription")
                        .categoriesTree(config.getCategoriesTree())
                        .goodsCategory(config.getGoodsCategory())
                        .goodsId("PLAN-" + plan.getId())
                        .goodsName(plan.getName())
                        .price(PaymentGatewayRequest.GoodsDetail.Price.builder()
                                .currency(config.getCurrency())
                                .amount(plan.getPrice())
                                .build())
                        .quantity(1)
                        .build())
                .terminalDetail(PaymentGatewayRequest.TerminalDetail.builder()
                        .merchantName(config.getMerchantName())
                        .operatorId(config.getOperatorId())
                        .storeId(config.getStoreId())
                        .terminalId(config.getTerminalId())
                        .storeName(config.getStoreName())
                        .build())
                .redirectUrl(callbackUrl)
                .expiryMints(config.getExpiryMinutes())
                .userLeft(1)
                .isSendsms(false)
                .mobno("")
                .build();
    }

    private PaymentGatewayResponse parseGatewayResponse(Map<String, Object> body) {
        // Parse the response based on actual PayBy API response structure
        // This may need adjustment based on actual API documentation
        Boolean success = (Boolean) body.getOrDefault("success", false);
        String transactionId = (String) body.get("transactionId");
        String qrCodeImage = (String) body.get("qrCodeImage");
        String paymentUrl = (String) body.get("paymentUrl");
        String message = (String) body.get("message");
        String status = (String) body.get("status");

        // If the response has a different structure, handle it
        if (transactionId == null) {
            // Try alternate field names
            transactionId = (String) body.get("orderId");
            if (transactionId == null) {
                transactionId = (String) body.get("orderNo");
            }
        }

        if (qrCodeImage == null) {
            qrCodeImage = (String) body.get("qrCode");
            if (qrCodeImage == null) {
                qrCodeImage = (String) body.get("interactiveQR");
            }
        }

        if (paymentUrl == null) {
            paymentUrl = (String) body.get("redirectUrl");
        }

        return PaymentGatewayResponse.builder()
                .success(success != null ? success : transactionId != null)
                .transactionId(transactionId)
                .qrCodeImage(qrCodeImage)
                .paymentUrl(paymentUrl)
                .message(message)
                .status(status)
                .build();
    }

    public String generateOrderReference(Integer subscriptionId) {
        // Format: SUB-{subscriptionId}-{timestamp}-{random}
        return String.format("SUB-%d-%d-%s",
                subscriptionId,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    public Integer extractSubscriptionIdFromOrderReference(String orderReference) {
        try {
            String[] parts = orderReference.split("-");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            log.error("Failed to extract subscription ID from order reference: {}", orderReference);
            return null;
        }
    }
}

package com.autoadda.apis.identity.controller;

import com.autoadda.apis.identity.request.InitiatePaymentRequest;
import com.autoadda.apis.identity.request.PaymentCallbackRequest;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.response.PaymentInitiationResponse;
import com.autoadda.apis.identity.service.PaymentGatewayService;
import com.autoadda.apis.identity.util.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Gateway", description = "APIs for subscription payment processing")
public class PaymentController {

    private final PaymentGatewayService paymentService;

    @Operation(summary = "Initiate subscription payment",
            description = "Initiates payment for a pending subscription. Returns QR code and payment URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or subscription not in PENDING status"),
            @ApiResponse(responseCode = "404", description = "Subscription not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to pay for this subscription")
    })
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        PaymentInitiationResponse response = paymentService.initiatePayment(request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Payment callback (Webhook)",
            description = "Webhook endpoint for payment gateway to notify payment status. " +
                    "This endpoint is called by the payment gateway, not by the frontend.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Callback processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid callback data")
    })
    @PostMapping("/callback")
    public ResponseEntity<GenericResponse> handleWebhookCallback(
            @RequestBody PaymentCallbackRequest callback) {
        log.info("Payment webhook received for transaction: {}", callback.getTransactionId());
        paymentService.handlePaymentCallback(callback);
        return ResponseEntity.ok(GenericResponse.builder()
                .status("success")
                .code("200")
                .message("Callback processed successfully")
                .build());
    }

    @Operation(summary = "Payment callback (Browser Redirect)",
            description = "Redirect endpoint after payment completion. Redirects user to frontend.")
    @GetMapping("/callback")
    public void handleBrowserRedirect(
            @RequestParam(required = false) String orderRef,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String transactionId,
            HttpServletResponse response) throws IOException {
        log.info("Browser redirect received. OrderRef: {}, Status: {}", orderRef, status);

        String redirectUrl = paymentService.handleBrowserRedirect(orderRef, status, transactionId);
        response.sendRedirect(redirectUrl);
    }

    @Operation(summary = "Check payment status",
            description = "Check the current payment status for a subscription")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment status retrieved"),
            @ApiResponse(responseCode = "404", description = "Subscription not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to view this subscription")
    })
    @GetMapping("/status/{subscriptionId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable Integer subscriptionId,
            @RequestHeader("X-Auto-Adda-Id") Integer userId) {
        PaymentStatus status = paymentService.getPaymentStatus(subscriptionId, userId);
        return ResponseEntity.ok(Map.of(
                "subscriptionId", subscriptionId,
                "paymentStatus", status.name(),
                "message", getStatusMessage(status)
        ));
    }

    private String getStatusMessage(PaymentStatus status) {
        return switch (status) {
            case NOT_INITIATED -> "Payment has not been initiated yet";
            case PENDING -> "Payment is pending. Please complete the payment.";
            case SUCCESS -> "Payment completed successfully";
            case FAILED -> "Payment failed. Please try again.";
            case CANCELLED -> "Payment was cancelled";
            case EXPIRED -> "Payment link has expired. Please initiate a new payment.";
        };
    }
}

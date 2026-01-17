package com.autoadda.apis.identity.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentInitiationResponse {

    private Integer subscriptionId;

    private String transactionId;

    private String qrCodeImage; // Base64 encoded QR code

    private String paymentUrl; // Direct URL to payment page

    private BigDecimal amount;

    private String currency;

    private Integer expiryMinutes;

    private String status;

    private String message;
}

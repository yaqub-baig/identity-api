package com.autoadda.apis.identity.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentGatewayResponse {

    private Boolean success;

    private String message;

    private String transactionId;

    private String qrCodeImage; // Base64 encoded QR image

    private String paymentUrl; // Direct payment URL if available

    private String status;

    private String errorCode;

    private String errorMessage;
}

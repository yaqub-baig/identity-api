package com.autoadda.apis.identity.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCallbackRequest {

    private String transactionId;

    private String orderReference;

    private String status; // SUCCESS, FAILED, CANCELLED

    private BigDecimal amount;

    private String currency;

    private String paymentMethod;

    private String signature;

    private String timestamp;

    private String errorCode;

    private String errorMessage;
}

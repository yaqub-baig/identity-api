package com.autoadda.apis.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class PaymentGatewayException extends RuntimeException {

    private final String errorCode;

    public PaymentGatewayException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
    }

    public PaymentGatewayException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PAYMENT_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}

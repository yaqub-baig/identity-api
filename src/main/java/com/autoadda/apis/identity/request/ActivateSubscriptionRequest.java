package com.autoadda.apis.identity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivateSubscriptionRequest {

    @NotBlank(message = "Payment reference is required")
    private String paymentReference;

    @NotNull(message = "Payment amount is required")
    private BigDecimal paymentAmount;

    private String notes;
}

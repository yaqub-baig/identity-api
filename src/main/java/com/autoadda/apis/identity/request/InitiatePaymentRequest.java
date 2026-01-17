package com.autoadda.apis.identity.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitiatePaymentRequest {

    @NotNull(message = "Subscription ID is required")
    private Integer subscriptionId;

    private String mobileNumber;

    private Boolean sendSms;
}

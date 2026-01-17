package com.autoadda.apis.identity.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserSubscriptionRequest {

    @NotNull(message = "Plan ID is required")
    private Integer planId;

    private String notes;
}

package com.autoadda.apis.identity.request;

import com.autoadda.apis.identity.util.EntityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSubscriptionRequest {

    @NotNull(message = "Entity ID is required")
    private Integer entityId;

    @NotNull(message = "Entity type is required")
    private EntityType entityType;

    @NotNull(message = "Plan ID is required")
    private Integer planId;

    private String paymentReference;

    private BigDecimal paymentAmount;

    private String notes;
}

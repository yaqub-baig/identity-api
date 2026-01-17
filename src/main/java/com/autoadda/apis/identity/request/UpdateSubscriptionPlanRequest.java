package com.autoadda.apis.identity.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSubscriptionPlanRequest {

    private String name;

    @Min(value = 1, message = "Duration must be at least 1 month")
    private Integer durationMonths;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private String description;

    private String applicableTo;

    private Boolean isActive;
}

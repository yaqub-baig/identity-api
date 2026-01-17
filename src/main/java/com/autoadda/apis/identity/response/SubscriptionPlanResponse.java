package com.autoadda.apis.identity.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionPlanResponse {

    private Integer id;

    private String name;

    private Integer durationMonths;

    private BigDecimal price;

    private String description;

    private String applicableTo;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

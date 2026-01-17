package com.autoadda.apis.identity.response;

import com.autoadda.apis.identity.util.EntityType;
import com.autoadda.apis.identity.util.PaymentStatus;
import com.autoadda.apis.identity.util.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlatformSubscriptionResponse {

    private Integer id;

    private Integer userId;

    private String userName;

    private Integer entityId;

    private EntityType entityType;

    private SubscriptionPlanResponse plan;

    private LocalDate startDate;

    private LocalDate endDate;

    private SubscriptionStatus status;

    private PaymentStatus paymentStatus;

    private Integer activatedBy;

    private LocalDateTime activatedAt;

    private String paymentReference;

    private BigDecimal paymentAmount;

    private String notes;

    private Boolean isTrial;

    private Long daysRemaining;

    private Boolean isExpiringSoon;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

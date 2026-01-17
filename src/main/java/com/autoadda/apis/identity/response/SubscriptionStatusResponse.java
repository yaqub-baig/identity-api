package com.autoadda.apis.identity.response;

import com.autoadda.apis.identity.util.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionStatusResponse {

    private Integer userId;

    @Deprecated
    private Integer entityId;

    @Deprecated
    private EntityType entityType;

    private Boolean hasActiveSubscription;

    private String status;

    private String planName;

    private Boolean isTrial;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long daysRemaining;

    private Boolean isExpiringSoon;

    private String message;
}

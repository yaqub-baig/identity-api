package com.autoadda.apis.identity.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long garageOwners;
    private long newThisMonth;
}

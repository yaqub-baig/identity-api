package com.autoadda.apis.identity.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {
    private String userId;
    private String role;
    private String fcmToken;
    private String deviceInfo;
}
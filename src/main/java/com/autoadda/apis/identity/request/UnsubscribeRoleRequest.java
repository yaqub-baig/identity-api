package com.autoadda.apis.identity.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnsubscribeRoleRequest {
    private String userId;
    private String fcmToken;
    private String role;
}

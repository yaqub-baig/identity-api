package com.autoadda.apis.identity.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestNotificationRequest {
    private String role;
    private String title;
    private String body;
    private String url;
}
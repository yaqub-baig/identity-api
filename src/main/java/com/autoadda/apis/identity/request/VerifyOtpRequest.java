package com.autoadda.apis.identity.request;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class VerifyOtpRequest {
    private String mobileNumber;
    private String otp;
}

package com.autoadda.apis.identity.request;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserRegisterRequest {
    private String firstName;
    private String lastName;
    private String emailId;
    private String mobileNumber;
    private String password;
    private String captchaToken;
}

package com.service.identity.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignInRequest {
    private String mobileNumber;
    private String email;
    private String password;
}

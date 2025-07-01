package com.platform.auth.reqest;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignInRequestV2 {
    private String mobileNumber;
    private String password;
}

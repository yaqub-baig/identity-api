package com.autoadda.apis.identity.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationRequest {

    private Integer memberId;
    private String memberType;
    private String toEmailId;
    private Integer templateId;
    private String verifyUrl;

}

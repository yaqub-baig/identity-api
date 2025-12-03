package com.autoadda.apis.identity.response;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GenericResponse {

    private String status;
    private String code;
    private String message;
    private Object body;

}

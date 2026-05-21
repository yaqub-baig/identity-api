package com.autoadda.apis.identity.request;

import com.autoadda.apis.identity.util.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserStatusRequest {
    private Status status;
}

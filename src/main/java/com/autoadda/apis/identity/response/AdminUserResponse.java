package com.autoadda.apis.identity.response;

import com.autoadda.apis.identity.entity.MediaFile;
import com.autoadda.apis.identity.util.ERole;
import com.autoadda.apis.identity.util.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserResponse {
    private Integer id;
    private String name;
    private String email;
    private String mobileNumber;
    private MediaFile profileImage;
    private boolean isVerified;
    private Status status;
    private Set<ERole> roles;
    private LocalDateTime joinDate;
}

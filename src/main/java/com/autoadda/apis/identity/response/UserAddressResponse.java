package com.autoadda.apis.identity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressResponse {
    private Long id;
    private Integer userId;
    private String label;
    private String contactName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String area;
    private String city;
    private String emirate;
    private String mapLink;
    private Double latitude;
    private Double longitude;
    private String notes;
    /** Lombok names the getter isDefault(), which Jackson would call "default"; keep "isDefault". */
    @JsonProperty("isDefault")
    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

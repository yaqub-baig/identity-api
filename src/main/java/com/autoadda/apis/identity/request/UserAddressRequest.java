package com.autoadda.apis.identity.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Create / replace a saved address. contactName, phone, addressLine1, area and city are required. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressRequest {
    private String label;
    private String contactName;
    /** A UAE number; stored as +971XXXXXXXXX. */
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
    /** true makes it the default address (the previous default is cleared). */
    private Boolean isDefault;
}

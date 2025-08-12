package com.autoadda.apis.identity.request;


import com.autoadda.apis.identity.util.Status;
import lombok.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationPreferenceRequest {


    private Boolean emailOnServiceUpdates;
    private Boolean smsOnServiceUpdates;

    private Boolean emailOnAppointments;
    private Boolean smsOnAppointments;

    private Boolean emailOnReminders;
    private Boolean smsOnReminders;

    private Boolean emailOnOffers;
    private Boolean smsOnOffers;

    private Boolean emailOnQuotationRequests;
    private Boolean smsOnQuotationRequests;

    @Enumerated(EnumType.STRING)
    private Status status;

    // Getters and setters
}

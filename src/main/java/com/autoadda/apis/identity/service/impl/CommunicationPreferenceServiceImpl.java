package com.autoadda.apis.identity.service.impl;


import com.autoadda.apis.identity.dao.CommunicationPreferenceRepository;
import com.autoadda.apis.identity.entity.CommunicationPreference;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.request.CommunicationPreferenceRequest;
import com.autoadda.apis.identity.service.CommunicationPreferenceService;
import com.autoadda.apis.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunicationPreferenceServiceImpl implements CommunicationPreferenceService {

    private final CommunicationPreferenceRepository repository;
    private final IdentityService identityService;

    CommunicationPreference mapToEntity(CommunicationPreferenceRequest preferenceRequest, CommunicationPreference communicationPreference) {
        if ( preferenceRequest.getEmailOnServiceUpdates ( ) != null )
            communicationPreference.setEmailOnServiceUpdates ( preferenceRequest.getEmailOnServiceUpdates ( ) );
        if ( preferenceRequest.getSmsOnServiceUpdates ( ) != null )
            communicationPreference.setSmsOnServiceUpdates ( preferenceRequest.getSmsOnServiceUpdates ( ) );
        if ( preferenceRequest.getEmailOnAppointments ( ) != null )
            communicationPreference.setEmailOnAppointments ( preferenceRequest.getEmailOnAppointments ( ) );
        if ( preferenceRequest.getSmsOnAppointments ( ) != null )
            communicationPreference.setSmsOnAppointments ( preferenceRequest.getSmsOnAppointments ( ) );
        if ( preferenceRequest.getEmailOnReminders ( ) != null )
            communicationPreference.setEmailOnReminders ( preferenceRequest.getEmailOnReminders ( ) );
        if ( preferenceRequest.getSmsOnReminders ( ) != null )
            communicationPreference.setSmsOnReminders ( preferenceRequest.getSmsOnReminders ( ) );
        if ( preferenceRequest.getEmailOnOffers ( ) != null )
            communicationPreference.setEmailOnOffers ( preferenceRequest.getEmailOnOffers ( ) );
        if ( preferenceRequest.getSmsOnOffers ( ) != null )
            communicationPreference.setSmsOnOffers ( preferenceRequest.getSmsOnOffers ( ) );
        if ( preferenceRequest.getEmailOnQuotationRequests ( ) != null )
            communicationPreference.setEmailOnQuotationRequests ( preferenceRequest.getEmailOnQuotationRequests ( ) );
        if ( preferenceRequest.getSmsOnQuotationRequests ( ) != null )
            communicationPreference.setSmsOnQuotationRequests ( preferenceRequest.getSmsOnQuotationRequests ( ) );
        if ( preferenceRequest.getStatus ( ) != null )
            communicationPreference.setStatus ( preferenceRequest.getStatus ( ) );
        return communicationPreference;
    }

    @Override
    public CommunicationPreference saveOrUpdatePreference(CommunicationPreferenceRequest preference) {
        User currentUser = identityService.getCurrentUser ( );
        CommunicationPreference communicationPreference = repository
                .findByUserId ( currentUser.getId ( ) )
                .orElseGet ( CommunicationPreference::new );
        communicationPreference.setUser ( currentUser );
        return repository.save ( mapToEntity ( preference, communicationPreference ) );
    }

    @Override
    public Optional<CommunicationPreference> getPreference(Integer customerId) {
        return repository.findByUserId ( customerId );
    }

}

package com.autoadda.apis.identity.service.impl;


import com.autoadda.apis.identity.dao.CommunicationPreferenceRepository;
import com.autoadda.apis.identity.entity.CommunicationPreference;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.request.CommunicationPreferenceRequest;
import com.autoadda.apis.identity.service.CommunicationPreferenceService;
import com.autoadda.apis.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
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

    @Override
    public Optional<CommunicationPreference> getPreferenceByIdAndCustomerId(Integer id, Integer customerId) {
        return repository.findByIdAndUserId ( id, customerId );
    }

    @Override
    public Optional<CommunicationPreference> getPreferenceByCustomerIdAndStatus(Integer customerId, String status) {
        return repository.findByUserIdAndStatus ( customerId, status );
    }

    @Override
    public void deletePreference(Integer id) {
        repository.deleteById ( id );
    }

    @Override
    public CommunicationPreference updatePreference(CommunicationPreference preference) {
        Optional<CommunicationPreference> existing = repository.findByIdAndUserId ( preference.getId ( ),
                preference.getUser ( ).getId ( ) );
        if ( existing.isPresent ( ) ) {
            return repository.save ( preference );
        }
        return null; // or throw an exception
    }

    @Override
    public boolean isWithinAllowedTime(CommunicationPreference pref, LocalTime now) {
        if ( pref.getDoNotDisturbStart ( ) == null || pref.getDoNotDisturbEnd ( ) == null ) {
            return true; // No restriction
        }

        // e.g., DND from 9 PM to 7 AM
        if ( pref.getDoNotDisturbStart ( ).isBefore ( pref.getDoNotDisturbEnd ( ) ) ) {
            return ! (now.isAfter ( pref.getDoNotDisturbStart ( ) ) && now.isBefore ( pref.getDoNotDisturbEnd ( ) ));
        } else {
            // Over midnight case
            return now.isBefore ( pref.getDoNotDisturbEnd ( ) ) || now.isAfter ( pref.getDoNotDisturbStart ( ) );
        }
    }

}

package com.service.identity.service.impl;


import com.service.identity.dao.CommunicationPreferenceRepository;
import com.service.identity.entity.CommunicationPreference;
import com.service.identity.request.CommunicationPreferenceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunicationPreferenceService implements com.service.identity.service.CommunicationPreferenceService {

    private final CommunicationPreferenceRepository repository;

    CommunicationPreference mapToEntity(CommunicationPreferenceRequest preferenceRequest) {
        return CommunicationPreference.builder ( ).customerId ( preferenceRequest.getCustomerId ( ) )
                .emailOnServiceUpdates ( preferenceRequest.getEmailOnServiceUpdates ( ) )
                .smsOnServiceUpdates ( preferenceRequest.getSmsOnServiceUpdates ( ) )
                .emailOnAppointments ( preferenceRequest.getEmailOnAppointments ( ) )
                .smsOnAppointments ( preferenceRequest.getSmsOnAppointments ( ) )
                .emailOnReminders ( preferenceRequest.getEmailOnReminders ( ) )
                .smsOnReminders ( preferenceRequest.getSmsOnReminders ( ) )
                .emailOnOffers ( preferenceRequest.getEmailOnOffers ( ) ).smsOnOffers ( preferenceRequest.getSmsOnOffers ( ) )
                .emailOnQuotationRequests ( preferenceRequest.getEmailOnQuotationRequests ( ) )
                .smsOnQuotationRequests ( preferenceRequest.getSmsOnQuotationRequests ( ) )
                .status ( preferenceRequest.getStatus ( ) ).build ( );
    }

    @Override
    public CommunicationPreference saveOrUpdatePreference(CommunicationPreferenceRequest preference) {
        Optional<CommunicationPreference> existing = repository.findByCustomerId ( preference.getCustomerId ( ) );
        if ( existing.isPresent ( ) ) {
            CommunicationPreference old = existing.get ( );
            preference.setId ( old.getId ( ) ); // Update
        }
        return repository.save ( mapToEntity ( preference ) );
    }

    @Override
    public Optional<CommunicationPreference> getPreference(Integer customerId) {
        return repository.findByCustomerId ( customerId );
    }

    @Override
    public Optional<CommunicationPreference> getPreferenceByIdAndCustomerId(Integer id, Integer customerId) {
        return repository.findByIdAndCustomerId ( id, customerId );
    }

    @Override
    public Optional<CommunicationPreference> getPreferenceByCustomerIdAndStatus(Integer customerId, String status) {
        return repository.findByCustomerIdAndStatus ( customerId, status );
    }

    @Override
    public void deletePreference(Integer id) {
        repository.deleteById ( id );
    }

    @Override
    public CommunicationPreference updatePreference(CommunicationPreference preference) {
        Optional<CommunicationPreference> existing = repository.findByIdAndCustomerId ( preference.getId ( ),
                preference.getCustomerId ( ) );
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

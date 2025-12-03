package com.autoadda.apis.identity.service.impl;


import com.autoadda.apis.identity.dao.CommunicationPreferenceRepository;
import com.autoadda.apis.identity.entity.CommunicationPreference;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.request.CommunicationPreferenceRequest;
import com.autoadda.apis.identity.service.CommunicationPreferenceService;
import com.autoadda.apis.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationPreferenceServiceImpl implements CommunicationPreferenceService {

    private final CommunicationPreferenceRepository repository;
    private final IdentityService identityService;

    CommunicationPreference mapToEntity(CommunicationPreferenceRequest preferenceRequest, CommunicationPreference communicationPreference) {
        log.debug("mapToEntity - Mapping request to entity. Request: {}", preferenceRequest);
        log.debug("mapToEntity - Existing entity ID: {}", communicationPreference.getId());

        if (preferenceRequest.getEmailOnServiceUpdates() != null) {
            log.debug("mapToEntity - Setting emailOnServiceUpdates: {}", preferenceRequest.getEmailOnServiceUpdates());
            communicationPreference.setEmailOnServiceUpdates(preferenceRequest.getEmailOnServiceUpdates());
        }
        if (preferenceRequest.getSmsOnServiceUpdates() != null) {
            log.debug("mapToEntity - Setting smsOnServiceUpdates: {}", preferenceRequest.getSmsOnServiceUpdates());
            communicationPreference.setSmsOnServiceUpdates(preferenceRequest.getSmsOnServiceUpdates());
        }
        if (preferenceRequest.getEmailOnAppointments() != null) {
            log.debug("mapToEntity - Setting emailOnAppointments: {}", preferenceRequest.getEmailOnAppointments());
            communicationPreference.setEmailOnAppointments(preferenceRequest.getEmailOnAppointments());
        }
        if (preferenceRequest.getSmsOnAppointments() != null) {
            log.debug("mapToEntity - Setting smsOnAppointments: {}", preferenceRequest.getSmsOnAppointments());
            communicationPreference.setSmsOnAppointments(preferenceRequest.getSmsOnAppointments());
        }
        if (preferenceRequest.getEmailOnReminders() != null) {
            log.debug("mapToEntity - Setting emailOnReminders: {}", preferenceRequest.getEmailOnReminders());
            communicationPreference.setEmailOnReminders(preferenceRequest.getEmailOnReminders());
        }
        if (preferenceRequest.getSmsOnReminders() != null) {
            log.debug("mapToEntity - Setting smsOnReminders: {}", preferenceRequest.getSmsOnReminders());
            communicationPreference.setSmsOnReminders(preferenceRequest.getSmsOnReminders());
        }
        if (preferenceRequest.getEmailOnOffers() != null) {
            log.debug("mapToEntity - Setting emailOnOffers: {}", preferenceRequest.getEmailOnOffers());
            communicationPreference.setEmailOnOffers(preferenceRequest.getEmailOnOffers());
        }
        if (preferenceRequest.getSmsOnOffers() != null) {
            log.debug("mapToEntity - Setting smsOnOffers: {}", preferenceRequest.getSmsOnOffers());
            communicationPreference.setSmsOnOffers(preferenceRequest.getSmsOnOffers());
        }
        if (preferenceRequest.getEmailOnQuotationRequests() != null) {
            log.debug("mapToEntity - Setting emailOnQuotationRequests: {}", preferenceRequest.getEmailOnQuotationRequests());
            communicationPreference.setEmailOnQuotationRequests(preferenceRequest.getEmailOnQuotationRequests());
        }
        if (preferenceRequest.getSmsOnQuotationRequests() != null) {
            log.debug("mapToEntity - Setting smsOnQuotationRequests: {}", preferenceRequest.getSmsOnQuotationRequests());
            communicationPreference.setSmsOnQuotationRequests(preferenceRequest.getSmsOnQuotationRequests());
        }
        if (preferenceRequest.getStatus() != null) {
            log.debug("mapToEntity - Setting status: {}", preferenceRequest.getStatus());
            communicationPreference.setStatus(preferenceRequest.getStatus());
        }

        log.debug("mapToEntity - Mapped entity: {}", communicationPreference);
        return communicationPreference;
    }

    @Override
    public CommunicationPreference saveOrUpdatePreference(CommunicationPreferenceRequest preference) {
        log.debug("saveOrUpdatePreference - Received request: {}", preference);
        User currentUser = identityService.getCurrentUser();
        log.debug("saveOrUpdatePreference - Current user: {} (ID: {})", currentUser.getMobileNumber(), currentUser.getId());
        CommunicationPreference communicationPreference = repository
                .findByUserId(currentUser.getId())
                .orElseGet(CommunicationPreference::new);
        log.debug("saveOrUpdatePreference - Found existing preference: {}", communicationPreference.getId() != null);
        communicationPreference.setUser(currentUser);
        CommunicationPreference mapped = mapToEntity(preference, communicationPreference);
        log.debug("saveOrUpdatePreference - Mapped entity before save: {}", mapped);
        CommunicationPreference saved = repository.save(mapped);
        log.debug("saveOrUpdatePreference - Saved entity: {}", saved);
        return saved;
    }

    @Override
    public Optional<CommunicationPreference> getPreference(Integer customerId) {
        log.debug("getPreference - Looking for preferences for customer ID: {}", customerId);
        Optional<CommunicationPreference> result = repository.findByUserId(customerId);
        log.debug("getPreference - Found preference: {}", result.isPresent());
        if (result.isPresent()) {
            log.debug("getPreference - Preference details: {}", result.get());
        }
        return result;
    }

}

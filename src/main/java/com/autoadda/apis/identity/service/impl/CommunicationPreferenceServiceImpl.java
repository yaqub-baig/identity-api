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
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationPreferenceServiceImpl implements CommunicationPreferenceService {

    private final CommunicationPreferenceRepository repository;
    private final IdentityService identityService;

    private <T> void setIfNotNull(T value, Consumer<T> setter, String fieldName) {
        if (value != null) {
            log.debug("mapToEntity - Setting {}: {}", fieldName, value);
            setter.accept(value);
        }
    }

    CommunicationPreference mapToEntity(CommunicationPreferenceRequest preferenceRequest, CommunicationPreference communicationPreference) {
        log.debug("mapToEntity - Mapping request to entity. Request: {}", preferenceRequest);
        log.debug("mapToEntity - Existing entity ID: {}", communicationPreference.getId());

        setIfNotNull(preferenceRequest.getWebpushOnQuotationRequests(), communicationPreference::setWebpushOnQuotationRequests, "webpushOnQuotationRequests");
        setIfNotNull(preferenceRequest.getEmailOnQuotationRequests(), communicationPreference::setEmailOnQuotationRequests, "emailOnQuotationRequests");
        setIfNotNull(preferenceRequest.getWebpushOnServiceUpdates(), communicationPreference::setWebpushOnServiceUpdates, "webpushOnServiceUpdates");
        setIfNotNull(preferenceRequest.getSmsOnQuotationRequests(), communicationPreference::setSmsOnQuotationRequests, "smsOnQuotationRequests");
        setIfNotNull(preferenceRequest.getWebpushOnAppointments(), communicationPreference::setWebpushOnAppointments, "webpushOnAppointments");
        setIfNotNull(preferenceRequest.getEmailOnServiceUpdates(), communicationPreference::setEmailOnServiceUpdates, "emailOnServiceUpdates");
        setIfNotNull(preferenceRequest.getSmsOnServiceUpdates(), communicationPreference::setSmsOnServiceUpdates, "smsOnServiceUpdates");
        setIfNotNull(preferenceRequest.getEmailOnAppointments(), communicationPreference::setEmailOnAppointments, "emailOnAppointments");
        setIfNotNull(preferenceRequest.getWebpushOnReminders(), communicationPreference::setWebpushOnReminders, "webpushOnReminders");
        setIfNotNull(preferenceRequest.getSmsOnAppointments(), communicationPreference::setSmsOnAppointments, "smsOnAppointments");
        setIfNotNull(preferenceRequest.getEmailOnReminders(), communicationPreference::setEmailOnReminders, "emailOnReminders");
        setIfNotNull(preferenceRequest.getWebpushOnOffers(), communicationPreference::setWebpushOnOffers, "webpushOnOffers");
        setIfNotNull(preferenceRequest.getSmsOnReminders(), communicationPreference::setSmsOnReminders, "smsOnReminders");
        setIfNotNull(preferenceRequest.getEmailOnOffers(), communicationPreference::setEmailOnOffers, "emailOnOffers");
        setIfNotNull(preferenceRequest.getSmsOnOffers(), communicationPreference::setSmsOnOffers, "smsOnOffers");
        setIfNotNull(preferenceRequest.getStatus(), communicationPreference::setStatus, "status");

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
        result.ifPresent(communicationPreference -> log.debug("getPreference - Preference details: {}", communicationPreference));
        return result;
    }

}

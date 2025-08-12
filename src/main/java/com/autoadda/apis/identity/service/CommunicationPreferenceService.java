package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.entity.CommunicationPreference;
import com.autoadda.apis.identity.request.CommunicationPreferenceRequest;

import java.time.LocalTime;
import java.util.Optional;

public interface CommunicationPreferenceService {
    CommunicationPreference saveOrUpdatePreference(CommunicationPreferenceRequest preference);

    Optional<CommunicationPreference> getPreference(Integer customerId);

    Optional<CommunicationPreference> getPreferenceByIdAndCustomerId(Integer id, Integer customerId);

    Optional<CommunicationPreference> getPreferenceByCustomerIdAndStatus(Integer customerId, String status);

    void deletePreference(Integer id);

    CommunicationPreference updatePreference(CommunicationPreference preference);

    boolean isWithinAllowedTime(CommunicationPreference pref, LocalTime now);
}

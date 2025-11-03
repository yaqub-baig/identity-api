package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.entity.CommunicationPreference;
import com.autoadda.apis.identity.request.CommunicationPreferenceRequest;

import java.util.Optional;

public interface CommunicationPreferenceService {
    CommunicationPreference saveOrUpdatePreference(CommunicationPreferenceRequest preference);

    Optional<CommunicationPreference> getPreference(Integer customerId);
}

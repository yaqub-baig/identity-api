package com.autoadda.apis.identity.controller;


import com.autoadda.apis.identity.entity.CommunicationPreference;
import com.autoadda.apis.identity.request.CommunicationPreferenceRequest;
import com.autoadda.apis.identity.service.CommunicationPreferenceService;
import com.autoadda.apis.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
public class CommunicationPreferenceController {
    private final CommunicationPreferenceService service;
    private final IdentityService identityService;

    @PostMapping
    public ResponseEntity<CommunicationPreference> createOrUpdatePreferences(
            @RequestBody CommunicationPreferenceRequest preference) {
        log.debug("POST /preferences - Received request: {}", preference);
        CommunicationPreference result = service.saveOrUpdatePreference(preference);
        log.debug("POST /preferences - Returning result: {}", result);
        return ResponseEntity.ok(result);
    }

    @PutMapping
    public ResponseEntity<CommunicationPreference> updatePreferences(
            @RequestBody CommunicationPreferenceRequest preference) {
        log.debug("PUT /preferences - Received request: {}", preference);
        CommunicationPreference result = service.saveOrUpdatePreference(preference);
        log.debug("PUT /preferences - Returning result: {}", result);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<CommunicationPreference> getMyPreferences() {
        log.debug("GET /preferences - Request received");
        Integer currentUserId = identityService.getCurrentUser().getId();
        log.debug("GET /preferences - Current user ID: {}", currentUserId);
        return service.getPreference(currentUserId)
                .map(pref -> {
                    log.debug("GET /preferences - Found preference: {}", pref);
                    return ResponseEntity.ok(pref);
                })
                .orElseGet(() -> {
                    log.debug("GET /preferences - No preference found, returning 404");
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CommunicationPreference> getPreferencesByCustomerId(
            @PathVariable Integer customerId) {
        log.debug("GET /preferences/{} - Request received for customer ID: {}", customerId, customerId);

        return service.getPreference(customerId)
                .map(pref -> {
                    log.debug("GET /preferences/{} - Found preference: {}", customerId, pref);
                    return ResponseEntity.ok(pref);
                })
                .orElseGet(() -> {
                    log.debug("GET /preferences/{} - No preference found, returning 404", customerId);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/isc/{customerId}")
    public ResponseEntity<CommunicationPreference> getPreferencesByCustomerIdForAdmin(
            @PathVariable Integer customerId) {
        return service.getPreference(customerId)
                .map(ResponseEntity::ok
                )
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

}

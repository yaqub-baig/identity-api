package com.autoadda.apis.identity.controller;


import com.autoadda.apis.identity.entity.CommunicationPreference;
import com.autoadda.apis.identity.request.CommunicationPreferenceRequest;
import com.autoadda.apis.identity.service.CommunicationPreferenceService;
import com.autoadda.apis.identity.service.IdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
public class CommunicationPreferenceController {
    private final CommunicationPreferenceService service;
    private final IdentityService identityService;

    @PostMapping
    public ResponseEntity<CommunicationPreference> createOrUpdatePreferences(
            @RequestBody CommunicationPreferenceRequest preference) {
        return ResponseEntity.ok(service.saveOrUpdatePreference(preference));
    }

    @PutMapping
    public ResponseEntity<CommunicationPreference> updatePreferences(
            @RequestBody CommunicationPreferenceRequest preference) {
        return ResponseEntity.ok(service.saveOrUpdatePreference(preference));
    }

    @GetMapping
    public ResponseEntity<CommunicationPreference> getMyPreferences() {
        Integer currentUserId = identityService.getCurrentUser().getId();
        return service.getPreference(currentUserId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CommunicationPreference> getPreferencesByCustomerId(
            @PathVariable Integer customerId) {
        // This endpoint could be used by admins to view any user's preferences
        // For now, allowing it but in production you should add admin role check
        return service.getPreference(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

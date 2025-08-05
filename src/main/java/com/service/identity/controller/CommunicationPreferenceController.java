package com.service.identity.controller;


import com.service.identity.entity.CommunicationPreference;
import com.service.identity.request.CommunicationPreferenceRequest;
import com.service.identity.service.CommunicationPreferenceService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
public class CommunicationPreferenceController {
    private final CommunicationPreferenceService service;

    @PostMapping
    public ResponseEntity<CommunicationPreference> setPreferences(@RequestBody CommunicationPreferenceRequest preference) {
        return ResponseEntity.ok ( service.saveOrUpdatePreference ( preference ) );
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CommunicationPreference> getPreferences(@PathVariable Integer customerId) {
        return service.getPreference ( customerId ).map ( ResponseEntity::ok ).orElse ( ResponseEntity.notFound ( ).build ( ) );
    }

    @GetMapping("/{id}/{customerId}")
    public ResponseEntity<CommunicationPreference> getPreferencesByIdAndCustomerId(@PathVariable Integer id,
                                                                                   @PathVariable Integer customerId) {
        return service.getPreferenceByIdAndCustomerId ( id, customerId ).map ( ResponseEntity::ok )
                .orElse ( ResponseEntity.notFound ( ).build ( ) );
    }

    @GetMapping("/status/{customerId}/{status}")
    public ResponseEntity<CommunicationPreference> getPreferencesByCustomerIdAndStatus(@PathVariable Integer customerId,
                                                                                       @PathVariable String status) {
        return service.getPreferenceByCustomerIdAndStatus ( customerId, status ).map ( ResponseEntity::ok )
                .orElse ( ResponseEntity.notFound ( ).build ( ) );
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deletePreference(@PathVariable Integer id) {
        service.deletePreference ( id );
        return ResponseEntity.noContent ( ).build ( );
    }

    @PostMapping("/update")
    public ResponseEntity<CommunicationPreference> updatePreference(
            @RequestBody CommunicationPreferenceRequest preference) {
        return ResponseEntity.ok ( service.saveOrUpdatePreference ( preference ) );
    }

    @GetMapping("/update/{id}/{customerId}")
    public ResponseEntity<CommunicationPreference> updatePreferenceByIdAndCustomerId(@PathVariable Integer id,
                                                                                     @PathVariable Integer customerId) {
        return service.getPreferenceByIdAndCustomerId ( id, customerId ).map ( ResponseEntity::ok )
                .orElse ( ResponseEntity.notFound ( ).build ( ) );
    }

}

package com.autoadda.apis.identity.controller;

import com.autoadda.apis.identity.request.UserAddressRequest;
import com.autoadda.apis.identity.response.UserAddressResponse;
import com.autoadda.apis.identity.service.IdentityService;
import com.autoadda.apis.identity.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The logged-in user's saved addresses (taken from the JWT). /addresses/internal/** is for other AutoAdda services
 * only (part-api reads the address chosen at checkout); the gateway blocks it from outside.
 */
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;
    private final IdentityService identityService;

    private Integer me() {
        return identityService.getCurrentUser().getId();
    }

    @Operation(summary = "My saved addresses (default first)")
    @GetMapping
    public List<UserAddressResponse> list() {
        return addressService.list(me());
    }

    @GetMapping("/{addressId}")
    public UserAddressResponse get(@PathVariable Long addressId) {
        return addressService.get(me(), addressId);
    }

    @Operation(summary = "Save an address (the first one becomes the default)")
    @PostMapping
    public ResponseEntity<UserAddressResponse> create(@RequestBody UserAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(me(), request));
    }

    @Operation(summary = "Replace an address")
    @PutMapping("/{addressId}")
    public UserAddressResponse update(@PathVariable Long addressId, @RequestBody UserAddressRequest request) {
        return addressService.update(me(), addressId, request);
    }

    @Operation(summary = "Make an address the default")
    @PutMapping("/{addressId}/default")
    public UserAddressResponse setDefault(@PathVariable Long addressId) {
        return addressService.setDefault(me(), addressId);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable Long addressId) {
        addressService.delete(me(), addressId);
        return ResponseEntity.noContent().build();
    }

    /** Service-to-service: a user's address by id (404 when it isn't that user's). Blocked at the gateway. */
    @GetMapping("/internal/{addressId}")
    public UserAddressResponse internalGet(@PathVariable Long addressId, @RequestParam Integer userId) {
        return addressService.get(userId, addressId);
    }
}

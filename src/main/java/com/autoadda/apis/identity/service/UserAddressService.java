package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.dao.UserAddressRepository;
import com.autoadda.apis.identity.entity.UserAddress;
import com.autoadda.apis.identity.exception.InvalidInputException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.request.UserAddressRequest;
import com.autoadda.apis.identity.response.UserAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A user's saved addresses. At most one is the default; the first address saved becomes the default, and
 * deleting the default promotes the newest remaining one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressService {

    public static final int MAX_ADDRESSES = 20;
    private static final String NOT_FOUND = "Address not found";
    /** UAE mobile (5XXXXXXXX) or landline (2-9 + 7 digits), with +971 / 00971 / 971 / 0 in front. */
    private static final Pattern UAE_PHONE = Pattern.compile("^(?:\\+971|00971|971|0)(5\\d{8}|[2-9]\\d{7})$");

    private final UserAddressRepository repository;

    @Transactional(readOnly = true)
    public List<UserAddressResponse> list(Integer userId) {
        return repository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream().map(UserAddressService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserAddressResponse get(Integer userId, Long addressId) {
        return toResponse(owned(userId, addressId));
    }

    @Transactional
    public UserAddressResponse create(Integer userId, UserAddressRequest request) {
        if (repository.countByUserId(userId) >= MAX_ADDRESSES) {
            throw new InvalidInputException("You can save at most " + MAX_ADDRESSES + " addresses");
        }
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setCreatedAt(LocalDateTime.now());
        apply(request, address);
        boolean first = repository.countByUserId(userId) == 0;
        UserAddress saved = repository.save(address);
        if (first || Boolean.TRUE.equals(request.getIsDefault())) {
            makeDefault(userId, saved);
        }
        return toResponse(saved);
    }

    /** Replaces the address (all fields). */
    @Transactional
    public UserAddressResponse update(Integer userId, Long addressId, UserAddressRequest request) {
        UserAddress address = owned(userId, addressId);
        apply(request, address);
        address.setUpdatedAt(LocalDateTime.now());
        UserAddress saved = repository.save(address);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            makeDefault(userId, saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public UserAddressResponse setDefault(Integer userId, Long addressId) {
        UserAddress address = owned(userId, addressId);
        makeDefault(userId, address);
        return toResponse(address);
    }

    @Transactional
    public void delete(Integer userId, Long addressId) {
        UserAddress address = owned(userId, addressId);
        repository.delete(address);
        if (address.isDefault()) {
            repository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                    .filter(a -> !a.getId().equals(addressId))
                    .findFirst()
                    .ifPresent(next -> makeDefault(userId, next));
        }
    }

    private void makeDefault(Integer userId, UserAddress chosen) {
        List<UserAddress> all = repository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        for (UserAddress a : all) {
            boolean shouldBe = a.getId().equals(chosen.getId());
            if (a.isDefault() != shouldBe) {
                a.setDefault(shouldBe);
                repository.save(a);
            }
        }
        chosen.setDefault(true);
    }

    /** Someone else's address answers 404. */
    private UserAddress owned(Integer userId, Long addressId) {
        return repository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    private static void apply(UserAddressRequest in, UserAddress out) {
        if (in == null) {
            throw new InvalidInputException("Address details are required");
        }
        out.setLabel(optional(in.getLabel(), "label", 50));
        out.setContactName(required(in.getContactName(), "contactName", 100));
        out.setPhone(phone(in.getPhone()));
        out.setAddressLine1(required(in.getAddressLine1(), "addressLine1", 200));
        out.setAddressLine2(optional(in.getAddressLine2(), "addressLine2", 200));
        out.setArea(required(in.getArea(), "area", 100));
        out.setCity(required(in.getCity(), "city", 100));
        out.setEmirate(optional(in.getEmirate(), "emirate", 50));
        out.setMapLink(optional(in.getMapLink(), "mapLink", 1000));
        out.setNotes(optional(in.getNotes(), "notes", 500));
        if ((in.getLatitude() == null) != (in.getLongitude() == null)) {
            throw new InvalidInputException("Send both latitude and longitude, or neither");
        }
        if (in.getLatitude() != null && (Math.abs(in.getLatitude()) > 90 || Math.abs(in.getLongitude()) > 180)) {
            throw new InvalidInputException("latitude / longitude out of range");
        }
        out.setLatitude(in.getLatitude());
        out.setLongitude(in.getLongitude());
    }

    static String phone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("phone is required");
        }
        Matcher m = UAE_PHONE.matcher(raw.replaceAll("[\\s()-]", ""));
        if (!m.matches()) {
            throw new InvalidInputException("phone must be a UAE number, e.g. 0501234567 or +971501234567");
        }
        return "+971" + m.group(1);
    }

    private static String required(String value, String field, int max) {
        String v = optional(value, field, max);
        if (v == null) {
            throw new InvalidInputException(field + " is required");
        }
        return v;
    }

    private static String optional(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new InvalidInputException(field + " is longer than " + max + " characters");
        }
        return v;
    }

    public static UserAddressResponse toResponse(UserAddress a) {
        return UserAddressResponse.builder()
                .id(a.getId()).userId(a.getUserId()).label(a.getLabel()).contactName(a.getContactName())
                .phone(a.getPhone()).addressLine1(a.getAddressLine1()).addressLine2(a.getAddressLine2())
                .area(a.getArea()).city(a.getCity()).emirate(a.getEmirate()).mapLink(a.getMapLink())
                .latitude(a.getLatitude()).longitude(a.getLongitude()).notes(a.getNotes())
                .isDefault(a.isDefault()).createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt())
                .build();
    }
}

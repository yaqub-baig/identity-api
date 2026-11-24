package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.dao.UserAddressRepository;
import com.autoadda.apis.identity.entity.UserAddress;
import com.autoadda.apis.identity.exception.InvalidInputException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.request.UserAddressRequest;
import com.autoadda.apis.identity.response.UserAddressResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Saved addresses against an in-memory repository. */
class UserAddressServiceTest {

    private final List<UserAddress> rows = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();
    private UserAddressService service;

    @BeforeEach
    void setUp() {
        UserAddressRepository repo = mock(UserAddressRepository.class);
        when(repo.save(any(UserAddress.class))).thenAnswer(inv -> {
            UserAddress a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(ids.incrementAndGet());
                a.setCreatedAt(a.getCreatedAt().plusNanos(a.getId())); // distinct, increasing
                rows.add(a);
            }
            return a;
        });
        when(repo.findByUserIdOrderByIsDefaultDescCreatedAtDesc(any())).thenAnswer(inv -> rows.stream()
                .filter(a -> a.getUserId().equals(inv.getArgument(0)))
                .sorted(Comparator.comparing(UserAddress::isDefault).reversed()
                        .thenComparing(UserAddress::getCreatedAt, Comparator.reverseOrder()))
                .toList());
        when(repo.findByIdAndUserId(any(), any())).thenAnswer(inv -> rows.stream()
                .filter(a -> a.getId().equals(inv.getArgument(0)) && a.getUserId().equals(inv.getArgument(1))).findFirst());
        when(repo.countByUserId(any())).thenAnswer(inv -> rows.stream().filter(a -> a.getUserId().equals(inv.getArgument(0))).count());
        doAnswer(inv -> rows.remove(inv.<UserAddress>getArgument(0))).when(repo).delete(any(UserAddress.class));
        service = new UserAddressService(repo);
    }

    private static UserAddressRequest.UserAddressRequestBuilder home() {
        return UserAddressRequest.builder().label("Home").contactName(" Sara Khan ").phone("050 123 4567")
                .addressLine1("Villa 12, Street 4").area("Al Barsha").city("Dubai");
    }

    @Test
    void firstAddressIsDefaultAndPhoneIsNormalised() {
        UserAddressResponse saved = service.create(1, home().build());
        assertTrue(saved.isDefault());
        assertEquals("+971501234567", saved.getPhone());
        assertEquals("Sara Khan", saved.getContactName());

        UserAddressResponse work = service.create(1, home().label("Work").build());
        assertFalse(work.isDefault());
        assertEquals(List.of("Home", "Work"), service.list(1).stream().map(UserAddressResponse::getLabel).toList());
    }

    @Test
    void choosingAnotherDefaultClearsTheOldOne() {
        UserAddressResponse home = service.create(1, home().build());
        UserAddressResponse work = service.create(1, home().label("Work").isDefault(true).build());
        assertTrue(work.isDefault());
        assertFalse(service.get(1, home.getId()).isDefault());

        service.setDefault(1, home.getId());
        assertTrue(service.get(1, home.getId()).isDefault());
        assertFalse(service.get(1, work.getId()).isDefault());
    }

    @Test
    void deletingTheDefaultPromotesTheNewestOther() {
        UserAddressResponse home = service.create(1, home().build());
        service.create(1, home().label("Work").build());
        UserAddressResponse gym = service.create(1, home().label("Gym").build());

        service.delete(1, home.getId());

        assertEquals(2, service.list(1).size());
        assertTrue(service.get(1, gym.getId()).isDefault());
    }

    @Test
    void otherUsersAddressesLookMissing() {
        UserAddressResponse home = service.create(1, home().build());
        assertThrows(ResourceNotFoundException.class, () -> service.get(2, home.getId()));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(2, home.getId()));
        assertThrows(ResourceNotFoundException.class, () -> service.update(2, home.getId(), home().build()));
        assertTrue(service.list(2).isEmpty());
    }

    @Test
    void updateReplacesTheFields() {
        UserAddressResponse home = service.create(1, home().build());
        UserAddressResponse updated = service.update(1, home.getId(), home().area("Jumeirah").notes("Gate 3").build());
        assertEquals("Jumeirah", updated.getArea());
        assertEquals("Gate 3", updated.getNotes());
        assertNotNull(updated.getUpdatedAt());
        assertTrue(updated.isDefault());
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(InvalidInputException.class, () -> service.create(1, home().phone("12345").build()));
        assertThrows(InvalidInputException.class, () -> service.create(1, home().city(" ").build()));
        assertThrows(InvalidInputException.class, () -> service.create(1, home().addressLine1(null).build()));
        assertThrows(InvalidInputException.class, () -> service.create(1, home().latitude(25.0).build()));
        assertThrows(InvalidInputException.class, () -> service.create(1, null));
        assertTrue(rows.isEmpty());
    }

    @Test
    void atMostTwentyAddresses() {
        for (int i = 0; i < UserAddressService.MAX_ADDRESSES; i++) {
            service.create(1, home().label("A" + i).build());
        }
        assertThrows(InvalidInputException.class, () -> service.create(1, home().build()));
    }

    @Test
    void responseKeepsTheIsDefaultName() throws Exception {
        UserAddressResponse saved = service.create(1, home().build());
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(saved);
        assertTrue(json.contains("\"isDefault\":true"), json);
        assertFalse(json.contains("\"default\""), json);
    }
}

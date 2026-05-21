package com.autoadda.apis.identity.controller;

import com.autoadda.apis.identity.request.UpdateUserStatusRequest;
import com.autoadda.apis.identity.response.AdminUserResponse;
import com.autoadda.apis.identity.response.AdminUserStatsResponse;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.service.AdminUserService;
import com.autoadda.apis.identity.util.ERole;
import com.autoadda.apis.identity.util.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Page<AdminUserResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ERole role,
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return adminUserService.listUsers(search, role, status, pageable);
    }

    @GetMapping("/stats")
    public AdminUserStatsResponse getStats() {
        return adminUserService.getStats();
    }

    @PatchMapping("/{id}/status")
    public GenericResponse updateStatus(@PathVariable Integer id,
                                        @RequestBody UpdateUserStatusRequest request) {
        return adminUserService.updateStatus(id, request.getStatus());
    }

    @DeleteMapping("/{id}")
    public GenericResponse deleteUser(@PathVariable Integer id) {
        return adminUserService.softDelete(id);
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}

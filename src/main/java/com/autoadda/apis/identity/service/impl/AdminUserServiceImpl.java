package com.autoadda.apis.identity.service.impl;

import com.autoadda.apis.identity.dao.UserRepository;
import com.autoadda.apis.identity.entity.Role;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.response.AdminUserResponse;
import com.autoadda.apis.identity.response.AdminUserStatsResponse;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.service.AdminUserService;
import com.autoadda.apis.identity.util.ERole;
import com.autoadda.apis.identity.util.Status;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final String SUCCESS = "success";
    private static final String USER_NOT_FOUND = "User not found with id: ";

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String search, ERole role, Status status, Pageable pageable) {
        Specification<User> spec = buildSpec(search, role, status);
        return userRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserStatsResponse getStats() {
        LocalDateTime startOfMonth = LocalDateTime.now(ZoneId.systemDefault())
                .withDayOfMonth(1)
                .with(LocalTime.MIN);

        return AdminUserStatsResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(Status.ACTIVE))
                .garageOwners(userRepository.countByRole(ERole.GARAGE_OWNER))
                .newThisMonth(userRepository.countByCreatedAtGreaterThanEqual(startOfMonth))
                .build();
    }

    @Override
    @Transactional
    public GenericResponse updateStatus(Integer id, Status status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        user.setStatus(status);
        userRepository.save(user);
        return GenericResponse.builder()
                .status(SUCCESS)
                .code("200")
                .message("User status updated to " + status)
                .body(toResponse(user))
                .build();
    }

    @Override
    @Transactional
    public GenericResponse softDelete(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        user.setStatus(Status.DELETED);
        userRepository.save(user);
        return GenericResponse.builder()
                .status(SUCCESS)
                .code("200")
                .message("User deleted")
                .build();
    }

    private Specification<User> buildSpec(String search, ERole role, Status status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("mobileNumber")), like)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (role != null) {
                Join<User, Role> roles = root.join("roles");
                predicates.add(cb.equal(roles.get("name"), role));
                if (query != null) {
                    query.distinct(true);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminUserResponse toResponse(User user) {
        Set<ERole> roleNames = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        String firstName = user.getFirstName() == null ? "" : user.getFirstName();
        String lastName = user.getLastName() == null ? "" : user.getLastName();

        return AdminUserResponse.builder()
                .id(user.getId())
                .name((firstName + " " + lastName).trim())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .profileImage(user.getProfileImage().isEmpty() ? null : user.getProfileImage().get(0))
                .isVerified(Boolean.TRUE.equals(user.getIsVerified()))
                .status(user.getStatus())
                .roles(roleNames)
                .joinDate(user.getCreatedAt())
                .build();
    }
}

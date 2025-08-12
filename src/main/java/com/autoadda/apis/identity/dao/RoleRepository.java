package com.autoadda.apis.identity.dao;

import com.autoadda.apis.identity.entity.Role;
import com.autoadda.apis.identity.util.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);

    boolean existsByName(ERole role);
}

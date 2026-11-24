package com.autoadda.apis.identity.dao;

import com.autoadda.apis.identity.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    /** Default first, then newest. */
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Integer userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Integer userId);

    long countByUserId(Integer userId);
}

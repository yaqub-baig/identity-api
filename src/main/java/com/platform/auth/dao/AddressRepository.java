package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.CustomerAddress;

@Repository
public interface AddressRepository extends JpaRepository<CustomerAddress, String> {
}

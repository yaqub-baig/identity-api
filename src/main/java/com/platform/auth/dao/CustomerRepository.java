package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer>{

}

package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.EstimatedService;

@Repository
public interface EstimatedServiceRepository extends JpaRepository<EstimatedService, String> {

}

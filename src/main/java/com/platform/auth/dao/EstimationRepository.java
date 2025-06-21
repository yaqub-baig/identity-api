package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.Estimation;

@Repository
public interface EstimationRepository extends JpaRepository<Estimation, String> {

}

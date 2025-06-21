package com.platform.auth.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.GarageTechnicians;

@Repository
public interface GarageTechnicianRepository extends JpaRepository<GarageTechnicians, Integer> {

	List<GarageTechnicians> findByGarageId(Integer garageId);
}

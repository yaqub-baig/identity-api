package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.Garage;

@Repository
public interface GarageRepository extends JpaRepository<Garage, Integer> {
}

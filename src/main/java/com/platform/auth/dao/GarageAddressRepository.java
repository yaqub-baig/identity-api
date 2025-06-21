package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.GarageAddress;

@Repository
public interface GarageAddressRepository extends JpaRepository<GarageAddress, Integer> {

	GarageAddress findByGarageId(Integer garageId);
}

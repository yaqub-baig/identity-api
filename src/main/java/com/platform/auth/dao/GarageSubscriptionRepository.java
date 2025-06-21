package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.GarageSubscription;

@Repository
public interface GarageSubscriptionRepository extends JpaRepository<GarageSubscription, Integer> {
	
	GarageSubscription findByGarageId(Integer garageId);
}

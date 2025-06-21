package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.GaragePendingActions;

@Repository
public interface GaragePendingActionsRepository extends JpaRepository<GaragePendingActions, Integer> {

	GaragePendingActions findByGarageId(Integer garageId);
}

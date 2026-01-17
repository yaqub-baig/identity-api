package com.autoadda.apis.identity.dao;

import com.autoadda.apis.identity.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {

    List<SubscriptionPlan> findByIsActiveTrue();

    List<SubscriptionPlan> findByIsActiveTrueOrderByDurationMonthsAsc();

    List<SubscriptionPlan> findByApplicableToInAndIsActiveTrue(List<String> applicableTo);

    boolean existsByNameIgnoreCase(String name);
}

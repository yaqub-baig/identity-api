package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.dao.SubscriptionPlanRepository;
import com.autoadda.apis.identity.entity.SubscriptionPlan;
import com.autoadda.apis.identity.exception.BadRequestException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.mapper.SubscriptionMapper;
import com.autoadda.apis.identity.request.CreateSubscriptionPlanRequest;
import com.autoadda.apis.identity.request.UpdateSubscriptionPlanRequest;
import com.autoadda.apis.identity.response.SubscriptionPlanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionMapper mapper;

    @Transactional
    public SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request) {
        log.info("Creating subscription plan: {}", request.getName());

        if (planRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Plan with name '" + request.getName() + "' already exists");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .durationMonths(request.getDurationMonths())
                .price(request.getPrice())
                .description(request.getDescription())
                .applicableTo(request.getApplicableTo() != null ? request.getApplicableTo() : "ALL")
                .isActive(true)
                .build();

        SubscriptionPlan savedPlan = planRepository.save(plan);
        log.info("Subscription plan created with ID: {}", savedPlan.getId());

        return mapper.planToResponse(savedPlan);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAllPlans() {
        return planRepository.findAll().stream()
                .map(mapper::planToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getActivePlans() {
        return planRepository.findByIsActiveTrueOrderByDurationMonthsAsc().stream()
                .map(mapper::planToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getActivePlansForEntityType(String entityType) {
        List<String> applicableTo = Arrays.asList("ALL", entityType);
        return planRepository.findByApplicableToInAndIsActiveTrue(applicableTo).stream()
                .map(mapper::planToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanById(Integer id) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
        return mapper.planToResponse(plan);
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(Integer id, UpdateSubscriptionPlanRequest request) {
        log.info("Updating subscription plan: {}", id);

        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        if (request.getName() != null && !request.getName().equalsIgnoreCase(plan.getName())) {
            if (planRepository.existsByNameIgnoreCase(request.getName())) {
                throw new BadRequestException("Plan with name '" + request.getName() + "' already exists");
            }
            plan.setName(request.getName());
        }

        if (request.getDurationMonths() != null) {
            plan.setDurationMonths(request.getDurationMonths());
        }

        if (request.getPrice() != null) {
            plan.setPrice(request.getPrice());
        }

        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }

        if (request.getApplicableTo() != null) {
            plan.setApplicableTo(request.getApplicableTo());
        }

        if (request.getIsActive() != null) {
            plan.setIsActive(request.getIsActive());
        }

        SubscriptionPlan updatedPlan = planRepository.save(plan);
        log.info("Subscription plan updated: {}", id);

        return mapper.planToResponse(updatedPlan);
    }

    @Transactional
    public void deactivatePlan(Integer id) {
        log.info("Deactivating subscription plan: {}", id);

        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

        plan.setIsActive(false);
        planRepository.save(plan);

        log.info("Subscription plan deactivated: {}", id);
    }
}

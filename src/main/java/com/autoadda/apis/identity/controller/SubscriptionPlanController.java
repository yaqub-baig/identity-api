package com.autoadda.apis.identity.controller;

import com.autoadda.apis.identity.request.CreateSubscriptionPlanRequest;
import com.autoadda.apis.identity.request.UpdateSubscriptionPlanRequest;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.response.SubscriptionPlanResponse;
import com.autoadda.apis.identity.service.SubscriptionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/subscriptions/plans")
@RequiredArgsConstructor
@Tag(name = "Subscription Plans", description = "APIs for managing subscription plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    // ==================== Admin Endpoints ====================

    @Operation(summary = "Create a new subscription plan (Admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Plan created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or plan name already exists")
    })
    @PostMapping
    public ResponseEntity<SubscriptionPlanResponse> createPlan(
            @Valid @RequestBody CreateSubscriptionPlanRequest request) {
        SubscriptionPlanResponse plan = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    @Operation(summary = "Get all subscription plans (Admin)")
    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @Operation(summary = "Get subscription plan by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan found"),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> getPlanById(@PathVariable Integer id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @Operation(summary = "Update subscription plan (Admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan updated successfully"),
            @ApiResponse(responseCode = "404", description = "Plan not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request or name already exists")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> updatePlan(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSubscriptionPlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    @Operation(summary = "Deactivate subscription plan (Admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plan deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> deactivatePlan(@PathVariable Integer id) {
        planService.deactivatePlan(id);
        return ResponseEntity.ok(GenericResponse.builder()
                .status("success")
                .code("200")
                .message("Plan deactivated successfully")
                .build());
    }

    // ==================== Public Endpoints ====================

    @Operation(summary = "Get active subscription plans (Public)")
    @GetMapping("/active")
    public ResponseEntity<List<SubscriptionPlanResponse>> getActivePlans() {
        return ResponseEntity.ok(planService.getActivePlans());
    }

    @Operation(summary = "Get active plans for entity type")
    @GetMapping("/active/{entityType}")
    public ResponseEntity<List<SubscriptionPlanResponse>> getActivePlansForEntityType(
            @PathVariable String entityType) {
        return ResponseEntity.ok(planService.getActivePlansForEntityType(entityType.toUpperCase()));
    }
}

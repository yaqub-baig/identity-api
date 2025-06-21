package com.platform.auth.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estimation")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Estimation {

	@Id
	@Column(name = "id")
	private String id;
	@Column(name = "customer_notes")
	private String customerNotes;
	@Column(name = "technician_notes")
	private String technicianNotes;
	@Column(name = "phone_number")
	private String phoneNumber;
	@Column(name = "email")
	private String email;
	@Column(name = "full_name")
	private String fullName;
	@Column(name = "vehicle_id")
	private String vehicleId;
	@Column(name = "employee_id")
	private String employeeId;
	@Column(name = "service_id")
	private String serviceId;
	@Column(name = "customer_id")
	private String customerId;

}
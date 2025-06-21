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
@Table(name = "work_order")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class WorkOrder {

	@Id
	@Column(name = "id")
	private String id;
	@Column(name = "appointment_id")
	private String appointmentId;
	@Column(name = "vehicle_id")
	private String vehicleId;

}

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
@Table(name = "inspection")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Inspection {

	@Id
	@Column(name = "id")
	private String id;
	
	@Column(name = "details")
	private String details;
	
	@Column(name = "category")
	private String category;
	
	@Column(name = "appointment_id")
	private String appointmentId;
	
	@Column(name = "vehicle_id")
	private String vehicleId;

}

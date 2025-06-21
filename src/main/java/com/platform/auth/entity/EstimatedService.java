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
@Table(name = "estimated_service")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EstimatedService {

	@Id
	@Column(name = "id")
	private String id;
	
	@Column(name = "service_id")
	private String serviceId;
	
	@Column(name = "estimation_id")
	private String estimationId;
}
package com.platform.auth.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "communication_preference")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class CommunicationPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Integer id;
	
	@Column(name = "phone_call")
	private Boolean phoneCall;
	
	@Column(name = "phone_text")
	private Boolean phoneText;
	
	@Column(name = "email")
	private Boolean email;
	
	@Column(name = "language")
	private String language;
	
	
}

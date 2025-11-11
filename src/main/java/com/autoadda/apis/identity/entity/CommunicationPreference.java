package com.autoadda.apis.identity.entity;


import javax.persistence.*;

import com.autoadda.apis.identity.util.Status;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@OneToOne
	@JsonIgnore
	private User user; // Reference to the customer (could be VehicleOwner or User)

	private Boolean emailOnServiceUpdates;
	private Boolean smsOnServiceUpdates;

	private Boolean emailOnAppointments;
	private Boolean smsOnAppointments;

	private Boolean emailOnReminders;
	private Boolean smsOnReminders;

	private Boolean emailOnOffers;
	private Boolean smsOnOffers;

	private Boolean emailOnQuotationRequests;
	private Boolean smsOnQuotationRequests;

	@Enumerated(EnumType.STRING)
	private Status status;

}

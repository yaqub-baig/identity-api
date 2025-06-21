package com.platform.auth.response;

import com.platform.auth.entity.Garage;
import com.platform.auth.entity.GarageAddress;
import com.platform.auth.entity.GaragePendingActions;
import com.platform.auth.entity.GarageSubscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GarageLoginResponse {
	private Garage garageDetails;
	private GarageAddress address;
	private GaragePendingActions pendingActions;
	private GarageSubscription subscription;
}

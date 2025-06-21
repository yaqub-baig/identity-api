package com.platform.auth.reqest;

import com.platform.auth.entity.GarageAddress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GarageRegisterRequest {
	private String legalName;
	private String garageName;
	private String emailId;
	private String password;
	private String contactNumber;
	private String webSite;
	private String garageType;
	private GarageAddress address;
	
}

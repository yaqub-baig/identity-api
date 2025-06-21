package com.platform.auth.reqest;

import com.platform.auth.entity.CustomerAddress;

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
public class CustomerRegisterRequest {
	private String corporateInd;
	private String emiratesId;
	private String firstName;
	private String lastName;
	private String emailId;
	private String password;
	private String mobileNumber;
	private String webSite;
	private String companyName;
	private String vatNumber;
	private String accountStatus;
	private CustomerAddress address;
	
}

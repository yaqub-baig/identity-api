package com.autoadda.apis.identity.request;

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
public class UserRegisterRequest {
	private String firstName;
	private String lastName;
	private String emailId;
	private String mobileNumber;
	private String password;
}

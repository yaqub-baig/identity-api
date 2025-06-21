package com.platform.auth.reqest;

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
public class UpdatePasswordRequest {
	private Integer memberId;
	private String memberType;
	private String password;
	private String confirmPassword;
	
}

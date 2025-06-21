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
public class SendVerifyEmailRequest {
	private Integer customerId;
	private String emailId;
	private String customerType;
	
}

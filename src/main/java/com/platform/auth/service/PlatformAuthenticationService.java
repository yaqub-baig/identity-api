package com.platform.auth.service;

import com.platform.auth.entity.Customer;
import com.platform.auth.exception.InvalidLoginException;
import com.platform.auth.exception.MemberAlreadyExistException;
import com.platform.auth.exception.MemberNotExistException;
import com.platform.auth.reqest.*;
import com.platform.auth.response.GenericResponse;

public interface PlatformAuthenticationService {

	public Customer createCustomerDetails(Customer customer);
	
	public String encryptString(String inputString);
	
	public String decryptString(String inputString);

	public GenericResponse registerCustomerDetails(CustomerRegisterRequest request) throws MemberAlreadyExistException;

	public GenericResponse verifyEmailAddress(String token);

	public GenericResponse sendVerifyEmail(SendVerifyEmailRequest request);

	public GenericResponse registerGarageDetails(GarageRegisterRequest request) throws MemberAlreadyExistException;

	public GenericResponse genericSignIn(SignInRequest request) throws MemberNotExistException, InvalidLoginException;

	public GenericResponse updateMemberPassword(UpdatePasswordRequest request) throws MemberNotExistException;

    GenericResponse registerUserDetails(UserRegisterRequest request);

	GenericResponse verifyOtp(VerifyOtpRequest request);

	GenericResponse signIn(SignInRequestV2 loginRequest);

	GenericResponse getAccessTokenViaRefreshToken(String refreshToken);
}

package com.service.identity.service;

import com.service.identity.request.ResetPasswordRequest;
import com.service.identity.request.SignInRequest;
import com.service.identity.request.UserRegisterRequest;
import com.service.identity.request.VerifyOtpRequest;
import com.service.identity.response.GenericResponse;
import com.service.identity.response.UserResponse;

public interface IdentityService {

    GenericResponse registerUserDetails(UserRegisterRequest request);

    GenericResponse verifyOtp(VerifyOtpRequest request);

    GenericResponse signIn(SignInRequest loginRequest);

    GenericResponse getAccessTokenViaRefreshToken(String refreshToken);

    GenericResponse resendOtp(String request);

    GenericResponse forgotPassword(String request);

    GenericResponse logout();

    GenericResponse resetPassword(ResetPasswordRequest request);

    GenericResponse verifyResetPasswordOtp(VerifyOtpRequest request);

    UserResponse getUser(Integer id);

//	public Customer createCustomerDetails(Customer customer);

//	public String encryptString(String inputString);

//	public String decryptString(String inputString);

//	public GenericResponse registerCustomerDetails(CustomerRegisterRequest request) throws MemberAlreadyExistException;

//	public GenericResponse verifyEmailAddress(String token);

//	public GenericResponse sendVerifyEmail(SendVerifyEmailRequest request);

//	public GenericResponse registerGarageDetails(GarageRegisterRequest request) throws MemberAlreadyExistException;

//	public GenericResponse genericSignIn(SignInRequest request) throws MemberNotExistException, InvalidLoginException;

//	public GenericResponse updateMemberPassword(UpdatePasswordRequest request) throws MemberNotExistException;

}

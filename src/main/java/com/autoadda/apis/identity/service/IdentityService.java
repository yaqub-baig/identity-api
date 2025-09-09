package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.request.ResetPasswordRequest;
import com.autoadda.apis.identity.request.SignInRequest;
import com.autoadda.apis.identity.request.UserRegisterRequest;
import com.autoadda.apis.identity.request.VerifyOtpRequest;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.response.UserResponse;
import com.autoadda.apis.identity.util.ERole;

public interface IdentityService {

    GenericResponse registerUserDetails(UserRegisterRequest request);

    GenericResponse verifyOtp(VerifyOtpRequest request);

    GenericResponse signIn(SignInRequest loginRequest);

    GenericResponse getAccessTokenViaRefreshToken(String refreshToken);

    GenericResponse resendOtp(String request);

    GenericResponse forgotPassword(String request);

    GenericResponse logout(String autoAddaId);

    GenericResponse resetPassword(ResetPasswordRequest request);

    GenericResponse verifyResetPasswordOtp(VerifyOtpRequest request);

    UserResponse getUser(Integer id);

    User getCurrentUser();

    GenericResponse updateRole(Integer id, ERole role);

    GenericResponse removeRole(Integer id, ERole role);

    UserResponse getUserDetailByPhoneNumber(String phoneNumber);

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

package com.platform.auth.controller;

import com.platform.auth.reqest.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.platform.auth.response.GenericResponse;
import com.platform.auth.service.PlatformAuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/auth")
public class PlatformAuthenticationController {
	
	@Autowired
	private PlatformAuthenticationService platformAuthenticationService;
	
	
	@Operation(summary = "Get a welcome message")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "If service is running fine, shows welcome message", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "Welcome api not found", 
	    content = @Content) })
	@GetMapping("welcome")
	public GenericResponse getGreetingMessage() {
		return GenericResponse.builder().status("sucess").code("200").message("success").body("Welcome to platform auth service..!").build();
	}
	
	@Operation(summary = "Customer Sign Up with basic details of the customer")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "201", description = "Successfull sign up process with return success message and registered customer details", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "registerCustomer api not found", 
	    content = @Content) })
	@PostMapping("customer")
	public GenericResponse registerCustomer(@RequestBody CustomerRegisterRequest request) {
		GenericResponse response = null;
		try {
			response = platformAuthenticationService.registerCustomerDetails(request);
			return response;
		} catch (Exception e) {
			log.error("ERROR while register customer -> "+e.getMessage());
			return GenericResponse.builder().status("failed").code("500").message(e.getMessage()).build();
		}
	}
	
	@Operation(summary = "Garage Sign Up with basic details of the service center")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "201", description = "Successfull sign up process with return success message and registered service center details", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "registerCustomer api not found", 
	    content = @Content) })
	@PostMapping("garage")
	public GenericResponse registerGarage(@RequestBody GarageRegisterRequest request) {
		GenericResponse response = null;
		try {
			response = platformAuthenticationService.registerGarageDetails(request);
			return response;
		} catch (Exception e) {
			log.error("ERROR while register garage -> "+e.getMessage());
			return GenericResponse.builder().status("failed").code("500").message(e.getMessage()).build();
		}
	}
	
	@Operation(summary = "Generic Sign in with username/password")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "201", description = "Successfull sign in process with return success message and registered details", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "registerCustomer api not found", 
	    content = @Content) })
	@PostMapping("signIn")
	public GenericResponse genericSignin(@RequestBody SignInRequest request) {
		GenericResponse response = null;
		try {
			response = platformAuthenticationService.genericSignIn(request);
			return response;
		} catch (Exception e) {
			log.error("ERROR while signin -> "+e.getMessage());
			return GenericResponse.builder().status("failed").code("500").message(e.getMessage()).build();
		}
	}
	
	@Operation(summary = "To send the verification email to the registered customer")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "successfull email delivery", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "sendVerificationEmail api not found", 
	    content = @Content) })
	@PostMapping("verifyEmail")
	public GenericResponse sendVerificationEmail(@RequestBody SendVerifyEmailRequest request) {
		GenericResponse response = null;
		try {
			response = platformAuthenticationService.sendVerifyEmail(request);
			return response;
		} catch (Exception e) {
			log.error("ERROR while sendVerificationEmail -> "+e.getMessage());
		}
		return GenericResponse.builder().status("failed").code("500").message("Unable to send verify email").build();
	}
	
	
	@Operation(summary = "To update the password")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "successfull password update", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "updatePassword api not found", 
	    content = @Content) })
	@PostMapping("updatePassword")
	public GenericResponse updatePassword(@RequestBody UpdatePasswordRequest request) {
		GenericResponse response = null;
		try {
			response = platformAuthenticationService.updateMemberPassword(request);
			return response;
		} catch (Exception e) {
			log.error("ERROR while updatePassword -> "+e.getMessage());
		}
		return GenericResponse.builder().status("failed").code("500").message("Unable to update Password").build();
	}
	
	
	@Operation(summary = "Verifying the customer email by clicking on the url shared via email to the registered customer")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "successfull email verification and it activates the customer account", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "sendVerificationEmail api not found", 
	    content = @Content) })
	@GetMapping("email/confirm")
	public GenericResponse verifyEmailAddress(@RequestParam String token) {
		GenericResponse response = null;
		try {
			response = platformAuthenticationService.verifyEmailAddress(token);
			return response;
		} catch (Exception e) {
			log.error("ERROR while register customer -> "+e.getMessage());
		}
		return GenericResponse.builder().status("failed").code("500").message("Unable to register").build();
	}

	@PostMapping("userSignUp")
	public GenericResponse registerUser(@RequestBody UserRegisterRequest request) {
		try {
			return platformAuthenticationService.registerUserDetails(request);
		} catch (Exception e) {
			log.error("ERROR while register user -> "+e.getMessage());
			return GenericResponse.builder().status("failed").code("500").message(e.getMessage()).build();
		}
	}

	@PostMapping("verifyOtp")
	public GenericResponse verifyOtp(@RequestBody VerifyOtpRequest request) {
		try {
			return platformAuthenticationService.verifyOtp(request);
		} catch (Exception e) {
			log.error("ERROR while verify otp -> "+e.getMessage());
			return GenericResponse.builder().status("failed").code("500").message(e.getMessage()).build();
		}
	}
	
	
	/*
	@Operation(summary = "Encrypting the string and resulting encryted string")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "successfull string encryption", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "encrypt api not found", 
	    content = @Content) })
	@GetMapping("encrypt")
	public GenericResponse encrypt(@RequestParam String input) {
		String encryptedString = platformAuthenticationService.encryptString(input);
		return GenericResponse.builder().status("sucess").code("200").message("customer created successfully..!").body(encryptedString).build();
	}
	
	
	@Operation(summary = "Decrypting the string and resulting decryted string")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "successfull string decryption", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = GenericResponse .class)) }),
	  @ApiResponse(responseCode = "404", description = "decrypt api not found", 
	    content = @Content) })
	@GetMapping("decrypt")
	public GenericResponse decrypt(@RequestParam String input) {
		String decryptedString = platformAuthenticationService.decryptString(input);
		return GenericResponse.builder().status("sucess").code("200").message("customer created successfully..!").body(decryptedString).build();
	}*/

}

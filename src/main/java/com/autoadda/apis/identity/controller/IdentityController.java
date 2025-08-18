package com.autoadda.apis.identity.controller;

import com.autoadda.apis.identity.request.ResetPasswordRequest;
import com.autoadda.apis.identity.request.SignInRequest;
import com.autoadda.apis.identity.request.UserRegisterRequest;
import com.autoadda.apis.identity.request.VerifyOtpRequest;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.response.UserResponse;
import com.autoadda.apis.identity.service.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/auth")
public class IdentityController {

    @Autowired
    private IdentityService identityService;


    @Operation(summary = "Get a welcome message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "If service is running fine, shows welcome message",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = GenericResponse.class))}),
            @ApiResponse(responseCode = "404", description = "Welcome api not found",
                    content = @Content)})
    @GetMapping("welcome")
    public GenericResponse getGreetingMessage() {
        return GenericResponse.builder ( ).status ( "sucess" ).code ( "200" ).message ( "success" ).body ( "Welcome to platform auth service..!" ).build ( );
    }

//    @Operation(summary = "Customer Sign Up with basic details of the customer")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "201", description = "Successfull sign up process with return success message and registered customer details",
//                    content = {@Content(mediaType = "application/json",
//                            schema = @Schema(implementation = GenericResponse.class))}),
//            @ApiResponse(responseCode = "404", description = "registerCustomer api not found",
//                    content = @Content)})
//    @PostMapping("customer")
//    public GenericResponse registerCustomer(@RequestBody CustomerRegisterRequest request) {
//        GenericResponse response = null;
//        try {
//            response = identityService.registerCustomerDetails ( request );
//            return response;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while register customer -> " + e.getMessage ( ) );
//            return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( e.getMessage ( ) ).build ( );
//        }
//    }

//    @Operation(summary = "Garage Sign Up with basic details of the service center")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "201", description = "Successfull sign up process with return success message and registered service center details",
//                    content = {@Content(mediaType = "application/json",
//                            schema = @Schema(implementation = GenericResponse.class))}),
//            @ApiResponse(responseCode = "404", description = "registerCustomer api not found",
//                    content = @Content)})
//    @PostMapping("garage")
//    public GenericResponse registerGarage(@RequestBody GarageRegisterRequest request) {
//        GenericResponse response = null;
//        try {
//            response = identityService.registerGarageDetails ( request );
//            return response;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while register garage -> " + e.getMessage ( ) );
//            return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( e.getMessage ( ) ).build ( );
//        }
//    }

//    @Operation(summary = "Generic Sign in with username/password")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "201", description = "Successfull sign in process with return success message and registered details",
//                    content = {@Content(mediaType = "application/json",
//                            schema = @Schema(implementation = GenericResponse.class))}),
//            @ApiResponse(responseCode = "404", description = "registerCustomer api not found",
//                    content = @Content)})
//    @PostMapping("signIn")
//    public GenericResponse genericSignin(@RequestBody SignInRequest request) {
//        GenericResponse response = null;
//        try {
//            response = identityService.genericSignIn ( request );
//            return response;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while signin -> " + e.getMessage ( ) );
//            return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( e.getMessage ( ) ).build ( );
//        }
//    }

//    @Operation(summary = "To send the verification email to the registered customer")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "successfull email delivery",
//                    content = {@Content(mediaType = "application/json",
//                            schema = @Schema(implementation = GenericResponse.class))}),
//            @ApiResponse(responseCode = "404", description = "sendVerificationEmail api not found",
//                    content = @Content)})
//    @PostMapping("verifyEmail")
//    public GenericResponse sendVerificationEmail(@RequestBody SendVerifyEmailRequest request) {
//        GenericResponse response = null;
//        try {
//            response = identityService.sendVerifyEmail ( request );
//            return response;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while sendVerificationEmail -> " + e.getMessage ( ) );
//        }
//        return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( "Unable to send verify email" ).build ( );
//    }


//    @Operation(summary = "To update the password")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "successfull password update",
//                    content = {@Content(mediaType = "application/json",
//                            schema = @Schema(implementation = GenericResponse.class))}),
//            @ApiResponse(responseCode = "404", description = "updatePassword api not found",
//                    content = @Content)})
//    @PostMapping("updatePassword")
//    public GenericResponse updatePassword(@RequestBody UpdatePasswordRequest request) {
//        GenericResponse response = null;
//        try {
//            response = identityService.updateMemberPassword ( request );
//            return response;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while updatePassword -> " + e.getMessage ( ) );
//        }
//        return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( "Unable to update Password" ).build ( );
//    }


//    @Operation(summary = "Verifying the customer email by clicking on the url shared via email to the registered customer")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "successfull email verification and it activates the customer account",
//                    content = {@Content(mediaType = "application/json",
//                            schema = @Schema(implementation = GenericResponse.class))}),
//            @ApiResponse(responseCode = "404", description = "sendVerificationEmail api not found",
//                    content = @Content)})
//    @GetMapping("email/confirm")
//    public GenericResponse verifyEmailAddress(@RequestParam String token) {
//        GenericResponse response = null;
//        try {
//            response = identityService.verifyEmailAddress ( token );
//            return response;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while register customer -> " + e.getMessage ( ) );
//        }
//        return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( "Unable to register" ).build ( );
//    }

    @PostMapping("user/signup")
    public GenericResponse registerUser(@RequestBody UserRegisterRequest request) {
        return identityService.registerUserDetails ( request );
    }

    @PostMapping("/user/verify-otp")
    public GenericResponse verifyOtp(@RequestBody VerifyOtpRequest request) {
        return identityService.verifyOtp ( request );
    }

    @PostMapping("/user/signin")
    public GenericResponse signin(@RequestBody SignInRequest loginRequest) {
        return identityService.signIn ( loginRequest );
    }

    @PostMapping("/user/refresh-token")
    public GenericResponse getAccessTokenViaRefreshToken(@RequestParam(value = "refreshToken") String refreshToken) {
        return identityService.getAccessTokenViaRefreshToken ( refreshToken );
    }

    @PostMapping("/user/resend-otp")
    public GenericResponse resendOtp(@RequestParam("mobileNumber") String request) {
        return identityService.resendOtp ( "+971" + request );
    }

    @PostMapping("/user/forgot-password")
    public GenericResponse forgotPassword(@RequestParam("mobileNumber") String request) {
        return identityService.forgotPassword ( "+971" + request );
    }

    @PostMapping("user/verify-reset-password-otp")
    public GenericResponse verifyResetPasswordOtp(@RequestBody VerifyOtpRequest request) {
        return identityService.verifyResetPasswordOtp ( request );
    }

    @PostMapping("reset-password")
    public GenericResponse resetPassword( @RequestHeader(value = "X-Auto-Adda-Id") String autoAddaId,
            @RequestBody ResetPasswordRequest request) {
        return identityService.resetPassword ( request,autoAddaId );
    }

    @PostMapping("/logout")
    public GenericResponse logout(@RequestHeader(value = "X-Auto-Adda-Id") String autoAddaId) {
        return identityService.logout ( autoAddaId );
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Integer id) {
        return identityService.getUser ( id );
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

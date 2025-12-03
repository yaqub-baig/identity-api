package com.autoadda.apis.identity.controller;

import com.autoadda.apis.identity.adopter.NotificationAdaptor;
import com.autoadda.apis.identity.request.*;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.response.UserResponse;
import com.autoadda.apis.identity.service.IdentityService;
import com.autoadda.apis.identity.util.ERole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class IdentityController {


    private final IdentityService identityService;
    private final NotificationAdaptor notificationAdaptor;


    @Operation(summary = "Get a welcome message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "If service is running fine, shows welcome message",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = GenericResponse.class))}),
            @ApiResponse(responseCode = "404", description = "Welcome api not found",
                    content = @Content)})
    @GetMapping("welcome")
    public GenericResponse getGreetingMessage() {
        return GenericResponse.builder().status("sucess").code("200").message("success").body("Welcome to platform auth service..!").build();
    }

    @PostMapping("user/signup")
    public GenericResponse registerUser(@RequestBody UserRegisterRequest request) {
        return identityService.registerUserDetails(request);
    }

    @PostMapping("/user/verify-otp")
    public GenericResponse verifyOtp(@RequestBody VerifyOtpRequest request) {
        return identityService.verifyOtp(request);
    }

    @PostMapping("/user/signin")
    public GenericResponse signin(@RequestBody SignInRequest loginRequest) {
        return identityService.signIn(loginRequest);
    }

    @PostMapping("/user/refresh-token")
    public GenericResponse getAccessTokenViaRefreshToken(@RequestParam(value = "refreshToken") String refreshToken) {
        return identityService.getAccessTokenViaRefreshToken(refreshToken);
    }

    @PostMapping("/user/resend-otp")
    public GenericResponse resendOtp(@RequestParam("mobileNumber") String request) {
        return identityService.resendOtp("+971" + request);
    }

    @PostMapping("/user/forgot-password")
    public GenericResponse forgotPassword(@RequestParam("mobileNumber") String request) {
        return identityService.forgotPassword("+971" + request);
    }

    @PostMapping("user/verify-reset-password-otp")
    public GenericResponse verifyResetPasswordOtp(@RequestBody VerifyOtpRequest request) {
        return identityService.verifyResetPasswordOtp(request);
    }

    @PostMapping("reset-password")
    public GenericResponse resetPassword(
            @RequestBody ResetPasswordRequest request) {
        return identityService.resetPassword(request);
    }

    @PostMapping("/logout")
    public GenericResponse logout(@RequestHeader(value = "X-Auto-Adda-Id") String autoAddaId) {
        return identityService.logout(autoAddaId);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Integer id) {
        return identityService.getUser(id);
    }

    @PutMapping("/update/role/{id}")
    public GenericResponse updateRole(@PathVariable Integer id, @RequestParam ERole role) {
        return identityService.updateRole(id, role);
    }

    @DeleteMapping("/remove/role/{id}")
    public GenericResponse removeRole(@PathVariable Integer id, @RequestParam ERole role) {
        return identityService.removeRole(id, role);
    }

    @GetMapping("/byNumber/{phoneNumber}")
    public ResponseEntity<UserResponse> getUserDetails(@PathVariable String phoneNumber) {
        return new ResponseEntity<>(identityService.getUserDetailByPhoneNumber(phoneNumber), HttpStatus.OK);
    }

    @PutMapping("/update/profile")
    public GenericResponse updateProfile(@RequestBody UserUpdateRequest request) {
        return identityService.updateUserProfile(request);
    }

    @PostMapping("/send-sms")
    public GenericResponse sendSms(@RequestBody SmsRequest request) {
        return notificationAdaptor.sendSms(request);
    }

    @PostMapping("/send-sms/{userId}")
    public GenericResponse sendSms(@PathVariable Integer userId, @RequestBody SmsRequest request) {
        return notificationAdaptor.sendSms(userId, request);
    }

}

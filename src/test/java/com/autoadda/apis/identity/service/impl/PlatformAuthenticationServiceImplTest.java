package com.autoadda.apis.identity.service.impl;

import com.autoadda.apis.identity.adopter.NotificationAdaptor;
import com.autoadda.apis.identity.dao.RoleRepository;
import com.autoadda.apis.identity.dao.UserRepository;
import com.autoadda.apis.identity.entity.RefreshToken;
import com.autoadda.apis.identity.entity.Role;
import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.exception.MemberAlreadyPresentException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.request.ResetPasswordRequest;
import com.autoadda.apis.identity.request.SignInRequest;
import com.autoadda.apis.identity.request.UserRegisterRequest;
import com.autoadda.apis.identity.request.VerifyOtpRequest;
import com.autoadda.apis.identity.response.AuthResponse;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.security.JwtUtil;
import com.autoadda.apis.identity.security.UserSecurityDetails;
import com.autoadda.apis.identity.service.RefreshTokenService;
import com.autoadda.apis.identity.mapper.UserMapper;
import com.autoadda.apis.identity.util.ERole;
import com.autoadda.apis.identity.validators.DataValidator;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlatformAuthenticationServiceImplTest {

    @Mock
    private DataValidator platformDataValidator;

    @Mock
    private NotificationAdaptor platformNotificationAdaptor;

    @Mock
    private UserMapper platformObjectTransformer;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;


    @Mock
    private Authentication authentication;

    @InjectMocks
    private IdentityServiceImpl platformAuthenticationService;
    private String autoAddaId ="1";

    @Order(1)
    @Test
    void registerUserDetails_success() {
        // Arrange
        UserRegisterRequest request = new UserRegisterRequest ( );
        request.setMobileNumber ( "0000000" );
        request.setEmailId ( "test@gmail.com" );
        request.setPassword ( "testPassword" );
        request.setFirstName ( "first" );
        request.setLastName ( "last" );

        User mockUser = new User ( );
        mockUser.setMobileNumber ( "+9710000000" );
        mockUser.setEmail ( "test@gmail.com" );
        mockUser.setFirstName ( "first" );
        mockUser.setLastName ( "last" );

        User savedUser = new User ( );
        savedUser.setId ( 1 );
        savedUser.setMobileNumber ( "+9710000000" );
        savedUser.setEmail ( "test@gmail.com" );
        savedUser.setFirstName ( "first" );
        savedUser.setLastName ( "last" );

        Role customerRole = new Role ( );
        customerRole.setName ( ERole.CUSTOMER );

        GenericResponse expectedResponse = new GenericResponse ( "success", "200", "Otp sent successfully", null );

        // Mock behaviors
        when ( platformDataValidator.checkUserAlreadyExists ( "+9710000000", request.getEmailId ( ) ) ).thenReturn ( false );
        when ( platformObjectTransformer.tranformUserRequestToUser ( request ) ).thenReturn ( mockUser );
        when ( passwordEncoder.encode ( "testPassword" ) ).thenReturn ( "encodedPassword" );
        when ( roleRepository.findByName ( ERole.CUSTOMER ) ).thenReturn ( Optional.of ( customerRole ) );
        when ( userRepository.save ( any ( User.class ) ) ).thenReturn ( savedUser );
        when ( platformNotificationAdaptor.callSMSServiceToSendOtp ( "+9710000000" ) ).thenReturn ( expectedResponse );

        // Act
        GenericResponse actualResponse = platformAuthenticationService.registerUserDetails ( request );

        // Assert
        assertThat ( actualResponse ).isNotNull ( );
        assertThat ( actualResponse.getStatus ( ) ).isEqualTo ( "success" );
        assertThat ( actualResponse.getCode ( ) ).isEqualTo ( "200" );
        assertThat ( actualResponse.getMessage ( ) ).isEqualTo ( "Otp sent successfully" );

        // Verify interactions
        verify ( platformDataValidator ).checkUserAlreadyExists ( "+9710000000", "test@gmail.com" );
        verify ( platformObjectTransformer ).tranformUserRequestToUser ( request );
        verify ( passwordEncoder ).encode ( "testPassword" );
        verify ( roleRepository ).findByName ( ERole.CUSTOMER );
        verify ( userRepository ).save ( argThat ( user -> {
            return user.getMobileNumber ( ).equals ( "+9710000000" ) &&
                    user.getPassword ( ).equals ( "encodedPassword" ) &&
                    user.getRoles ( ).contains ( customerRole );
        } ) );
        verify ( platformNotificationAdaptor ).callSMSServiceToSendOtp ( "+9710000000" );
    }

    @Order(2)
    @Test
    void registerUserDetails_userExistsButNotVerified_shouldSendOtp() {
        // Arrange
        UserRegisterRequest request = new UserRegisterRequest ( );
        request.setMobileNumber ( "0000000" );
        request.setEmailId ( "test@gmail.com" );
        request.setPassword ( "testPassword" );

        GenericResponse expectedResponse = new GenericResponse ( "success", "200", "Otp sent successfully", null );

        // Mock behaviors
        when ( platformDataValidator.checkUserAlreadyExists ( "+9710000000", request.getEmailId ( ) ) ).thenReturn ( true );
        when ( platformDataValidator.checkUserIsVerified ( "+9710000000" ) ).thenReturn ( false );
        when ( platformNotificationAdaptor.callSMSServiceToSendOtp ( "+9710000000" ) ).thenReturn ( expectedResponse );

        // Act
        GenericResponse actualResponse = platformAuthenticationService.registerUserDetails ( request );

        // Assert
        assertThat ( actualResponse ).isNotNull ( );
        assertThat ( actualResponse.getStatus ( ) ).isEqualTo ( "success" );
        assertThat ( actualResponse.getCode ( ) ).isEqualTo ( "200" );
        assertThat ( actualResponse.getMessage ( ) ).isEqualTo ( "Otp sent successfully" );

        // Verify interactions
        verify ( platformDataValidator ).checkUserAlreadyExists ( "+9710000000", "test@gmail.com" );
        verify ( platformDataValidator ).checkUserIsVerified ( "+9710000000" );
        verify ( platformNotificationAdaptor ).callSMSServiceToSendOtp ( "+9710000000" );

        // Verify that user creation methods are NOT called
        verify ( platformObjectTransformer, never ( ) ).tranformUserRequestToUser ( any ( ) );
        verify ( passwordEncoder, never ( ) ).encode ( any ( ) );
        verify ( roleRepository, never ( ) ).findByName ( any ( ) );
        verify ( userRepository, never ( ) ).save ( any ( ) );
    }

    @Order(3)
    @Test
    void registerUserDetails_userExistsAndVerified_shouldThrowException() {
        // Arrange
        UserRegisterRequest request = new UserRegisterRequest ( );
        request.setMobileNumber ( "0000000" );
        request.setEmailId ( "test@gmail.com" );
        request.setPassword ( "testPassword" );

        // Mock behaviors
        when ( platformDataValidator.checkUserAlreadyExists ( "+9710000000", request.getEmailId ( ) ) ).thenReturn ( true );
        when ( platformDataValidator.checkUserIsVerified ( "+9710000000" ) ).thenReturn ( true );

        // Act & Assert
        assertThatThrownBy ( () -> platformAuthenticationService.registerUserDetails ( request ) )
                .isInstanceOf ( MemberAlreadyPresentException.class )
                .hasMessage ( "user already exists" );

        // Verify interactions
        verify ( platformDataValidator ).checkUserAlreadyExists ( "+9710000000", "test@gmail.com" );
        verify ( platformDataValidator ).checkUserIsVerified ( "+9710000000" );

        // Verify that user creation and OTP methods are NOT called
        verify ( platformObjectTransformer, never ( ) ).tranformUserRequestToUser ( any ( ) );
        verify ( passwordEncoder, never ( ) ).encode ( any ( ) );
        verify ( roleRepository, never ( ) ).findByName ( any ( ) );
        verify ( userRepository, never ( ) ).save ( any ( ) );
        verify ( platformNotificationAdaptor, never ( ) ).callSMSServiceToSendOtp ( any ( ) );
    }

    @Order(4)
    @Test
    void registerUserDetails_roleNotFound_shouldThrowException() {
        // Arrange
        UserRegisterRequest request = new UserRegisterRequest ( );
        request.setMobileNumber ( "0000000" );
        request.setEmailId ( "test@gmail.com" );
        request.setPassword ( "testPassword" );
        request.setFirstName ( "first" );
        request.setLastName ( "last" );

        User mockUser = new User ( );
        mockUser.setMobileNumber ( "+9710000000" );

        // Mock behaviors
        when ( platformDataValidator.checkUserAlreadyExists ( "+9710000000", request.getEmailId ( ) ) ).thenReturn ( false );
        when ( platformObjectTransformer.tranformUserRequestToUser ( request ) ).thenReturn ( mockUser );
        when ( passwordEncoder.encode ( "testPassword" ) ).thenReturn ( "encodedPassword" );
        when ( roleRepository.findByName ( ERole.CUSTOMER ) ).thenReturn ( Optional.empty ( ) );

        // Act & Assert
        assertThatThrownBy ( () -> platformAuthenticationService.registerUserDetails ( request ) )
                .isInstanceOf ( RuntimeException.class )
                .hasMessage ( "Error: Role is not found." );

        // Verify interactions
        verify ( platformDataValidator ).checkUserAlreadyExists ( "+9710000000", "test@gmail.com" );
        verify ( platformObjectTransformer ).tranformUserRequestToUser ( request );
        verify ( passwordEncoder ).encode ( "testPassword" );
        verify ( roleRepository ).findByName ( ERole.CUSTOMER );

        // Verify that user save and OTP methods are NOT called
        verify ( userRepository, never ( ) ).save ( any ( ) );
        verify ( platformNotificationAdaptor, never ( ) ).callSMSServiceToSendOtp ( any ( ) );
    }




    @Order(5)
    @Test
    void verifyOtp_success_userExists() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setMobileNumber("0000000");
        request.setOtp("123456");

        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setMobileNumber("+9710000000");
        existingUser.setIsVerified(false);

        User updatedUser = new User();
        updatedUser.setId(1);
        updatedUser.setMobileNumber("+9710000000");
        updatedUser.setIsVerified(true);

        // Mock behaviors
        when(platformNotificationAdaptor.verifyOtp(request)).thenReturn(true);
        when(userRepository.findByMobileNumber("+9710000000")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.verifyOtp(request);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("otp verified");

        // Verify interactions
        verify(platformNotificationAdaptor).verifyOtp(request);
        verify(userRepository).findByMobileNumber("+9710000000");
        verify(userRepository).save(argThat(user -> {
            return user.getMobileNumber().equals("+9710000000") &&
                    user.getIsVerified() == true;
        }));
    }

    @Order(6)
    @Test
    void verifyOtp_success_userNotExists() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setMobileNumber("0000000");
        request.setOtp("123456");

        // Mock behaviors
        when(platformNotificationAdaptor.verifyOtp(request)).thenReturn(true);
        when(userRepository.findByMobileNumber("+9710000000")).thenReturn(Optional.empty());

        // Act
        GenericResponse actualResponse = platformAuthenticationService.verifyOtp(request);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("otp verified");

        // Verify interactions
        verify(platformNotificationAdaptor).verifyOtp(request);
        verify(userRepository).findByMobileNumber("+9710000000");
        verify(userRepository, never()).save(any());
    }

    @Order(7)
    @Test
    void verifyOtp_invalidOtp_shouldReturnFailedResponse() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setMobileNumber("0000000");
        request.setOtp("999999");

        // Mock behaviors
        when(platformNotificationAdaptor.verifyOtp(request)).thenReturn(false);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.verifyOtp(request);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("failed");
        assertThat(actualResponse.getCode()).isEqualTo("500");
        assertThat(actualResponse.getMessage()).isEqualTo("invalid otp");

        // Verify interactions
        verify(platformNotificationAdaptor).verifyOtp(request);
        verify(userRepository, never()).findByMobileNumber(any());
        verify(userRepository, never()).save(any());
    }

    @Order(8)
    @Test
    void verifyOtp_mobileNumberTransformation() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setMobileNumber("1234567"); // This should become +9711234567
        request.setOtp("123456");

        // Mock behaviors
        when(platformNotificationAdaptor.verifyOtp(request)).thenReturn(true);
        when(userRepository.findByMobileNumber("+9711234567")).thenReturn(Optional.empty());

        // Act
        GenericResponse actualResponse = platformAuthenticationService.verifyOtp(request);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(request.getMobileNumber()).isEqualTo("+9711234567"); // Verify transformation

        // Verify interactions
        verify(platformNotificationAdaptor).verifyOtp(request);
        verify(userRepository).findByMobileNumber("+9711234567");
    }

    @Order(9)
    @Test
    void resendOtp_success() {
        // Arrange
        String mobileNumber = "+9710000000";
        GenericResponse expectedResponse = new GenericResponse("success", "200", "Otp sent successfully", null);

        // Mock behaviors
        when(platformNotificationAdaptor.callSMSServiceToSendOtp(mobileNumber)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.resendOtp(mobileNumber);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Otp sent successfully");

        // Verify interactions
        verify(platformNotificationAdaptor).callSMSServiceToSendOtp(mobileNumber);
    }

    @Order(10)
    @Test
    void resendOtp_failure() {
        // Arrange
        String mobileNumber = "+9710000000";
        GenericResponse expectedResponse = new GenericResponse("failed", "500", "Failed to send OTP", null);

        // Mock behaviors
        when(platformNotificationAdaptor.callSMSServiceToSendOtp(mobileNumber)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.resendOtp(mobileNumber);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("failed");
        assertThat(actualResponse.getCode()).isEqualTo("500");
        assertThat(actualResponse.getMessage()).isEqualTo("Failed to send OTP");

        // Verify interactions
        verify(platformNotificationAdaptor).callSMSServiceToSendOtp(mobileNumber);
    }

    @Order(11)
    @Test
    void resendOtp_withNullMobileNumber() {
        // Arrange
        String mobileNumber = null;
        GenericResponse expectedResponse = new GenericResponse("failed", "400", "Invalid mobile number", null);

        // Mock behaviors
        when(platformNotificationAdaptor.callSMSServiceToSendOtp(mobileNumber)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.resendOtp(mobileNumber);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("failed");
        assertThat(actualResponse.getCode()).isEqualTo("400");
        assertThat(actualResponse.getMessage()).isEqualTo("Invalid mobile number");

        // Verify interactions
        verify(platformNotificationAdaptor).callSMSServiceToSendOtp(mobileNumber);
    }

    @Order(12)
    @Test
    void resendOtp_withEmptyMobileNumber() {
        // Arrange
        String mobileNumber = "";
        GenericResponse expectedResponse = new GenericResponse("failed", "400", "Invalid mobile number", null);

        // Mock behaviors
        when(platformNotificationAdaptor.callSMSServiceToSendOtp(mobileNumber)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.resendOtp(mobileNumber);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("failed");
        assertThat(actualResponse.getCode()).isEqualTo("400");
        assertThat(actualResponse.getMessage()).isEqualTo("Invalid mobile number");

        // Verify interactions
        verify(platformNotificationAdaptor).callSMSServiceToSendOtp(mobileNumber);
    }

    @Order(13)
    @Test
    void resendOtp_withDifferentMobileNumberFormats() {
        // Arrange
        String mobileNumber = "971501234567";
        GenericResponse expectedResponse = new GenericResponse("success", "200", "Otp sent successfully", null);

        // Mock behaviors
        when(platformNotificationAdaptor.callSMSServiceToSendOtp(mobileNumber)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.resendOtp(mobileNumber);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Otp sent successfully");

        // Verify interactions
        verify(platformNotificationAdaptor).callSMSServiceToSendOtp(mobileNumber);
    }

    @Order(14)
    @Test
    void signIn_success_withMobileNumber() {
        // Arrange
        SignInRequest request = new SignInRequest ();
        request.setMobileNumber("0000000");
        request.setPassword("testPassword");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setMobileNumber("+9710000000");
        mockUser.setIsVerified(true);

        UserSecurityDetails userDetails = new UserSecurityDetails(mockUser);
        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-456";


        // Mock behaviors
        when(userRepository.findByMobileNumberAndIsVerified("+9710000000")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn(accessToken);
        when(refreshTokenService.generateRefreshToken(mockUser)).thenReturn(refreshToken);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.signIn(request);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Login successful");
        assertThat(actualResponse.getBody()).isInstanceOf(AuthResponse.class);

        AuthResponse authResponse = (AuthResponse) actualResponse.getBody();
        assertThat(authResponse.getAccessToken()).isEqualTo(accessToken);
        assertThat(authResponse.getRefreshToken()).isEqualTo(refreshToken);

        // Verify interactions
        verify(userRepository).findByMobileNumberAndIsVerified("+9710000000");
        verify(authenticationManager).authenticate(argThat(auth -> {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth;
            return token.getPrincipal().equals("+9710000000") &&
                    token.getCredentials().equals("testPassword");
        }));
        verify(jwtUtil).generateAccessToken(userDetails);
        verify(refreshTokenService).generateRefreshToken(mockUser);
    }

    @Order(15)
    @Test
    void signIn_success_withEmail() {
        // Arrange
        SignInRequest request = new SignInRequest ();
        request.setEmail("test@gmail.com");
        request.setPassword("testPassword");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail("test@gmail.com");
        mockUser.setMobileNumber("+9710000000");
        mockUser.setIsVerified(true);

        UserSecurityDetails userDetails = new UserSecurityDetails(mockUser);
        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-456";

        // Mock behaviors
        when(userRepository.findByEmailAndIsVerified("test@gmail.com")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn(accessToken);
        when(refreshTokenService.generateRefreshToken(mockUser)).thenReturn(refreshToken);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.signIn(request);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Login successful");

        // Verify interactions
        verify(userRepository).findByEmailAndIsVerified("test@gmail.com");
        verify(authenticationManager).authenticate(argThat(auth -> {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth;
            return token.getPrincipal().equals("+9710000000") &&
                    token.getCredentials().equals("testPassword");
        }));
        verify(jwtUtil).generateAccessToken(userDetails);
        verify(refreshTokenService).generateRefreshToken(mockUser);
    }

    @Order(16)
    @Test
    void signIn_userNotFound_withMobileNumber() {
        // Arrange
        SignInRequest request = new SignInRequest ();
        request.setMobileNumber("0000000");
        request.setPassword("testPassword");

        // Mock behaviors
        when(userRepository.findByMobileNumberAndIsVerified("+9710000000")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.signIn(request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(" User not found with phone number: +9710000000");

        // Verify interactions
        verify(userRepository).findByMobileNumberAndIsVerified("+9710000000");
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtUtil, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).generateRefreshToken(any());
    }

    @Order(17)
    @Test
    void signIn_userNotFound_withEmail() {
        // Arrange
        SignInRequest request = new SignInRequest ();
        request.setEmail("test@gmail.com");
        request.setPassword("testPassword");

        // Mock behaviors
        when(userRepository.findByEmailAndIsVerified("test@gmail.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.signIn(request))
                .isInstanceOf( UsernameNotFoundException.class)
                .hasMessage("user not found with email: test@gmail.com");

        // Verify interactions
        verify(userRepository).findByEmailAndIsVerified("test@gmail.com");
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtUtil, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).generateRefreshToken(any());
    }

    @Order(18)
    @Test
    void signIn_authenticationFailed() {
        // Arrange
        SignInRequest request = new SignInRequest ();
        request.setMobileNumber("0000000");
        request.setPassword("wrongPassword");

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setMobileNumber("+9710000000");
        mockUser.setIsVerified(true);

        // Mock behaviors
        when(userRepository.findByMobileNumberAndIsVerified("+9710000000")).thenReturn(Optional.of(mockUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.signIn(request))
                .isInstanceOf( BadCredentialsException.class)
                .hasMessage("Bad credentials");

        // Verify interactions
        verify(userRepository).findByMobileNumberAndIsVerified("+9710000000");
        verify(authenticationManager).authenticate(any( UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).generateRefreshToken(any());
    }

// ================ LOGOUT TEST CASES ================

    @Order(19)
    @Test
    void logout_success() {
        // Arrange
        Integer currentUserId = 1;
        GenericResponse expectedResponse = new GenericResponse("success", "200", "Logged out ", null);

        // Mock behaviors
        setupSecurityContext(null);
        when(refreshTokenService.deleteRefreshTokenByUserId(currentUserId)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.logout( autoAddaId );

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Logged out ");

        // Verify interactions
        verify(refreshTokenService).deleteRefreshTokenByUserId(currentUserId);
    }

    @Order(20)
    @Test
    void logout_failure() {
        // Arrange
        Integer currentUserId = 1;
        GenericResponse expectedResponse = new GenericResponse("failed", "500", "Failed to logout", null);


        setupSecurityContext(null); // Uses default mock user
        when(refreshTokenService.deleteRefreshTokenByUserId(currentUserId)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.logout( autoAddaId );

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("failed");
        assertThat(actualResponse.getCode()).isEqualTo("500");
        assertThat(actualResponse.getMessage()).isEqualTo("Failed to logout");

        // Verify interactions
        verify(refreshTokenService).deleteRefreshTokenByUserId(currentUserId);
    }

// ================ GET ACCESS TOKEN VIA REFRESH TOKEN TEST CASES ================

    @Order(21)
    @Test
    void getAccessTokenViaRefreshToken_success() {
        // Arrange
        String refreshTokenValue = "refresh-token-123";
        String newAccessToken = "new-access-token-456";

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setMobileNumber("+9710000000");

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken(refreshTokenValue);
        mockRefreshToken.setUser(mockUser);

        // Mock behaviors
        when(refreshTokenService.validateRefreshToken(refreshTokenValue)).thenReturn(mockRefreshToken);
        when(jwtUtil.generateAccessToken(any(UserSecurityDetails.class))).thenReturn(newAccessToken);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.getAccessTokenViaRefreshToken(refreshTokenValue);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Access token generated successfully");
        assertThat(actualResponse.getBody()).isInstanceOf(AuthResponse.class);

        AuthResponse authResponse = (AuthResponse) actualResponse.getBody();
        assertThat(authResponse.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(authResponse.getRefreshToken()).isEqualTo(refreshTokenValue);

        // Verify interactions
        verify(refreshTokenService).validateRefreshToken(refreshTokenValue);
        verify(jwtUtil).generateAccessToken(any(UserSecurityDetails.class));
    }

    @Order(22)
    @Test
    void getAccessTokenViaRefreshToken_invalidToken() {
        // Arrange
        String invalidRefreshToken = "invalid-refresh-token";

        // Mock behaviors
        when(refreshTokenService.validateRefreshToken(invalidRefreshToken))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.getAccessTokenViaRefreshToken(invalidRefreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid refresh token");

        // Verify interactions
        verify(refreshTokenService).validateRefreshToken(invalidRefreshToken);
        verify(jwtUtil, never()).generateAccessToken(any());
    }

// ================ FORGOT PASSWORD TEST CASES ================

    @Order(23)
    @Test
    void forgotPassword_success() {
        // Arrange
        String phoneNumber = "+9710000000";

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setMobileNumber(phoneNumber);

        GenericResponse expectedResponse = new GenericResponse("success", "200", "Otp sent successfully", null);

        // Mock behaviors
        when(platformDataValidator.checkUserExistWithPhoneNumber(phoneNumber)).thenReturn(Optional.of(mockUser));
        when(platformNotificationAdaptor.callSMSServiceToSendOtp(phoneNumber)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.forgotPassword(phoneNumber);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Otp sent successfully for reset password");

        // Verify interactions
        verify(platformDataValidator).checkUserExistWithPhoneNumber(phoneNumber);
        verify(platformNotificationAdaptor).callSMSServiceToSendOtp(phoneNumber);
    }

    @Order(24)
    @Test
    void forgotPassword_userNotFound() {
        // Arrange
        String phoneNumber = "+9710000000";

        // Mock behaviors
        when(platformDataValidator.checkUserExistWithPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.forgotPassword(phoneNumber))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("user not found with phone Number");

        // Verify interactions
        verify(platformDataValidator).checkUserExistWithPhoneNumber(phoneNumber);
        verify(platformNotificationAdaptor, never()).callSMSServiceToSendOtp(any());
    }

    @Order(25)
    @Test
    void forgotPassword_smsServiceFailure() {
        // Arrange
        String phoneNumber = "+9710000000";

        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setMobileNumber(phoneNumber);

        GenericResponse expectedResponse = new GenericResponse("failed", "500", "Failed to send OTP", null);

        // Mock behaviors
        when(platformDataValidator.checkUserExistWithPhoneNumber(phoneNumber)).thenReturn(Optional.of(mockUser));
        when(platformNotificationAdaptor.callSMSServiceToSendOtp(phoneNumber)).thenReturn(expectedResponse);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.forgotPassword(phoneNumber);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("failed");
        assertThat(actualResponse.getCode()).isEqualTo("500");
        assertThat(actualResponse.getMessage()).isEqualTo("Otp sent successfully for reset password");

        // Verify interactions
        verify(platformDataValidator).checkUserExistWithPhoneNumber(phoneNumber);
        verify(platformNotificationAdaptor).callSMSServiceToSendOtp(phoneNumber);
    }

// ================ VERIFY RESET PASSWORD OTP TEST CASES ================


    @Order(26)
    @Test
    void verifyResetPasswordOtp_success() {
        // Arrange
        String otp = "123456";
        String mobileNumber = "10000000";
        String fullMobileNumber = "+971" + mobileNumber;
        User mockUser = createMockUser();
        mockUser.setMobileNumber(mobileNumber);

        VerifyOtpRequest request = new VerifyOtpRequest ();
        request.setMobileNumber(mobileNumber);
        request.setOtp(otp);

        // Mock behaviors

        when(platformNotificationAdaptor.verifyOtp(request)).thenReturn(true);
        when(userRepository.findByMobileNumber(fullMobileNumber)).thenReturn(Optional.of(mockUser));

        // Mock the JWT token generation with the actual expected arguments
        when(jwtUtil.generateAccessToken(any(UserSecurityDetails.class), eq(300000L)))
                .thenReturn("mocked-access-token");

        // Act
        GenericResponse response = platformAuthenticationService.verifyResetPasswordOtp(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getCode()).isEqualTo("200");
        assertThat(response.getMessage()).isEqualTo("otp verified successfully");
        assertThat(response.getBody()).isNotNull();


        // Verify interactions
        verify(platformNotificationAdaptor).verifyOtp(request);
        verify(userRepository).findByMobileNumber(fullMobileNumber);
        verify(jwtUtil).generateAccessToken(any(UserSecurityDetails.class), eq(300000L));
    }

    @Order(27)
    @Test
    void verifyResetPasswordOtp_invalidOtp() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setMobileNumber("0000000");
        request.setOtp("999999");

        // Mock behaviors
        when(platformNotificationAdaptor.verifyOtp(request)).thenReturn(false);

        // Act
        GenericResponse actualResponse = platformAuthenticationService.verifyResetPasswordOtp(request);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("failed");
        assertThat(actualResponse.getCode()).isEqualTo("500");
        assertThat(actualResponse.getMessage()).isEqualTo("invalid otp");

        // Verify interactions
        verify(platformNotificationAdaptor).verifyOtp(request);
        verify(userRepository, never()).findByMobileNumber(any());
        verify(jwtUtil, never()).generateAccessToken(any(UserSecurityDetails.class), anyInt());
    }

    @Order(28)
    @Test
    void verifyResetPasswordOtp_userNotFound() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setMobileNumber("0000000");
        request.setOtp("123456");

        // Mock behaviors
        when(platformNotificationAdaptor.verifyOtp(request)).thenReturn(true);
        when(userRepository.findByMobileNumber("+9710000000")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.verifyResetPasswordOtp(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("user not found with mobile number");

        // Verify interactions
        verify(platformNotificationAdaptor).verifyOtp(request);
        verify(userRepository).findByMobileNumber("+9710000000");
        verify(jwtUtil, never()).generateAccessToken(any(UserSecurityDetails.class), anyInt());
    }

// ================ RESET PASSWORD TEST CASES ================


    @Order(29)
    @Test
    void resetPassword_success() {
        // Setup test data
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("newPassword123");

        // Create a test user with roles
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setMobileNumber("+9710000000");
        mockUser.setPassword("oldEncodedPassword");
        mockUser.setRoles(new HashSet<>());

        // Mock the repository to find user by mobile number
        when(userRepository.findByMobileNumber(mockUser.getMobileNumber()))
                .thenReturn(Optional.of(mockUser));

        // Setup security context with the mock user
        setupSecurityContext(mockUser);

        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setPassword("newEncodedPassword");

        // Mock behaviors
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Execute the method
        GenericResponse response = platformAuthenticationService.resetPassword(request );

        // Verify the results
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getCode()).isEqualTo("200");
        assertThat(response.getMessage()).isEqualTo("Password reset successfully");

        // Verify interactions
        verify(userRepository).findByMobileNumber("+9710000000");
        verify(userRepository).findById(1);
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Order(30)
    @Test
    void resetPassword_userNotFound() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("newPassword123");

        Integer currentUserId = 1;

        // Mock behaviors
        setupSecurityContext(null);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.resetPassword(request ))
                .isInstanceOf( ResourceNotFoundException.class)
                .hasMessage("user not found with phoneNumber");

        // Verify interactions
        verify(userRepository).findById(currentUserId);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Order(31)
    @Test
    void resetPassword_nullPassword() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword(null);

        Integer currentUserId = 1;
        User mockUser = new User();
        mockUser.setId(currentUserId);
        mockUser.setMobileNumber("+9710000000");

        // Mock behaviors
        setupSecurityContext(null);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode(null)).thenReturn("encodedNull");

        // Act
        GenericResponse actualResponse = platformAuthenticationService.resetPassword(request );

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Password reset successfully");

        // Verify interactions
        verify(userRepository).findById(currentUserId);
        verify(passwordEncoder).encode(null);
        verify(userRepository).save(any(User.class));
    }

    @Order(32)
    @Test
    void resetPassword_emptyPassword() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("");

        Integer currentUserId = 1;
        User mockUser = new User();
        mockUser.setId(currentUserId);
        mockUser.setMobileNumber("+9710000000");

        // Mock behaviors
        setupSecurityContext(null);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("")).thenReturn("encodedEmpty");

        // Act
        GenericResponse actualResponse = platformAuthenticationService.resetPassword(request );

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getStatus()).isEqualTo("success");
        assertThat(actualResponse.getCode()).isEqualTo("200");
        assertThat(actualResponse.getMessage()).isEqualTo("Password reset successfully");

        // Verify interactions
        verify(userRepository).findById(currentUserId);
        verify(passwordEncoder).encode("");
        verify(userRepository).save(any(User.class));
    }
    @Order(33)
    @Test
    void getCurrentUserId_success() {
        // Arrange
        String mobileNumber = "+9710000000";
        Integer expectedUserId = 1;

        User mockUser = new User();
        mockUser.setId(expectedUserId);
        mockUser.setMobileNumber(mobileNumber);

        Authentication mockAuthentication = mock(Authentication.class);
        SecurityContext mockSecurityContext = mock(SecurityContext.class);

        // Mock behaviors
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getName()).thenReturn(mobileNumber);
        when(userRepository.findByMobileNumber(mobileNumber)).thenReturn(Optional.of(mockUser));

        // Set up SecurityContextHolder
        SecurityContextHolder.setContext(mockSecurityContext);

        // Act
        Integer actualUserId = platformAuthenticationService.getCurrentUserId();

        // Assert
        assertThat(actualUserId).isNotNull();
        assertThat(actualUserId).isEqualTo(expectedUserId);

        // Verify interactions
        verify(mockSecurityContext).getAuthentication();
        verify(mockAuthentication).isAuthenticated();
        verify(userRepository).findByMobileNumber(mobileNumber);

        // Clean up
        SecurityContextHolder.clearContext();
    }

    @Order(34)
    @Test
    void getCurrentUserId_nullAuthentication() {
        // Arrange
        SecurityContext mockSecurityContext = mock(SecurityContext.class);

        // Mock behaviors
        when(mockSecurityContext.getAuthentication()).thenReturn(null);

        // Set up SecurityContextHolder
        SecurityContextHolder.setContext(mockSecurityContext);

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.getCurrentUserId())
                .isInstanceOf(JwtException.class)
                .hasMessage("User not authenticated");

        // Verify interactions
        verify(mockSecurityContext).getAuthentication();
        verify(userRepository, never()).findByMobileNumber(any());

        // Clean up
        SecurityContextHolder.clearContext();
    }

    @Order(35)
    @Test
    void getCurrentUserId_notAuthenticated() {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        SecurityContext mockSecurityContext = mock(SecurityContext.class);

        // Mock behaviors
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(false);

        // Set up SecurityContextHolder
        SecurityContextHolder.setContext(mockSecurityContext);

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.getCurrentUserId())
                .isInstanceOf(JwtException.class)
                .hasMessage("User not authenticated");

        // Verify interactions
        verify(mockSecurityContext).getAuthentication();
        verify(mockAuthentication).isAuthenticated();
        verify(mockAuthentication, never()).getName();
        verify(userRepository, never()).findByMobileNumber(any());

        // Clean up
        SecurityContextHolder.clearContext();
    }

    @Order(36)
    @Test
    void getCurrentUserId_userNotFoundInRepository() {
        // Arrange
        String mobileNumber = "+9710000000";

        Authentication mockAuthentication = mock(Authentication.class);
        SecurityContext mockSecurityContext = mock(SecurityContext.class);

        // Mock behaviors
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getName()).thenReturn(mobileNumber);
        when(userRepository.findByMobileNumber(mobileNumber)).thenReturn(Optional.empty());

        // Set up SecurityContextHolder
        SecurityContextHolder.setContext(mockSecurityContext);

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.getCurrentUserId())
                .isInstanceOf(JwtException.class)
                .hasMessage("User not found");

        // Verify interactions
        verify(mockSecurityContext).getAuthentication();
        verify(mockAuthentication).isAuthenticated();

        verify(userRepository).findByMobileNumber(mobileNumber);

        // Clean up
        SecurityContextHolder.clearContext();
    }

    @Order(37)
    @Test
    void getCurrentUserId_emptyMobileNumber() {
        // Arrange
        String emptyMobileNumber = "";

        Authentication mockAuthentication = mock(Authentication.class);
        SecurityContext mockSecurityContext = mock(SecurityContext.class);

        // Mock behaviors
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getName()).thenReturn(emptyMobileNumber);
        when(userRepository.findByMobileNumber(emptyMobileNumber)).thenReturn(Optional.empty());

        // Set up SecurityContextHolder
        SecurityContextHolder.setContext(mockSecurityContext);

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.getCurrentUserId())
                .isInstanceOf( JwtException.class)
                .hasMessage("User not found");

        // Verify interactions
        verify(mockSecurityContext).getAuthentication();
        verify(mockAuthentication).isAuthenticated();
        verify(userRepository).findByMobileNumber(emptyMobileNumber);

        // Clean up
        SecurityContextHolder.clearContext();
    }

    @Order(38)
    @Test
    void getCurrentUserId_nullMobileNumber() {
        // Arrange
        Authentication mockAuthentication = mock(Authentication.class);
        SecurityContext mockSecurityContext = mock( SecurityContext.class);

        // Mock behaviors
        when(mockSecurityContext.getAuthentication()).thenReturn(mockAuthentication);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(mockAuthentication.getName()).thenReturn(null);
        when(userRepository.findByMobileNumber(null)).thenReturn(Optional.empty());

        // Set up SecurityContextHolder
        SecurityContextHolder.setContext(mockSecurityContext);

        // Act & Assert
        assertThatThrownBy(() -> platformAuthenticationService.getCurrentUserId())
                .isInstanceOf(JwtException.class)
                .hasMessage("User not found");

        // Verify interactions
        verify(mockSecurityContext).getAuthentication();
        verify(mockAuthentication).isAuthenticated();

        verify(userRepository).findByMobileNumber(null);

        // Clean up
        SecurityContextHolder.clearContext();
    }
    private void setupSecurityContext(User user) {
        if (user == null) {
            user = createMockUser(); // Use default mock user if none provided
        }
        UserSecurityDetails userDetails = new UserSecurityDetails(user);
        Authentication authentication1 = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication1);

        // Also mock the repository to return this user when queried by mobile number
        when(userRepository.findByMobileNumber(user.getMobileNumber()))
                .thenReturn(Optional.of(user));
    }

    private User createMockUser() {
        return createMockUser(1, "+9710000000", new HashSet<>());
    }

    private User createMockUser(Integer id, String mobileNumber, Set<Role> roles) {
        User user = new User();
        user.setId(id);
        user.setMobileNumber(mobileNumber);
        user.setRoles(roles);
        user.setPassword("encodedPassword"); // Add any other required default fields
        return user;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}


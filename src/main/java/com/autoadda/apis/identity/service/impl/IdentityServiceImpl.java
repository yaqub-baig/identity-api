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
import com.autoadda.apis.identity.response.UserResponse;
import com.autoadda.apis.identity.security.JwtUtil;
import com.autoadda.apis.identity.security.UserSecurityDetails;
import com.autoadda.apis.identity.service.IdentityService;
import com.autoadda.apis.identity.service.RefreshTokenService;
import com.autoadda.apis.identity.mapper.UserMapper;
import com.autoadda.apis.identity.util.ERole;
import com.autoadda.apis.identity.validators.DataValidator;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {

    private final DataValidator dataValidator;
    private final UserMapper userMapper;
    private final NotificationAdaptor notificationAdaptor;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

//    private final CustomerRepository customerRepository;
//    private final AuthenticationDetailsRepository authenticationDetailsRepository;
//    private final CustomerAddressRepository customerAddressRepository;
//    private final AccountVerificationTrackerRepository accountVerificationTrackerRepository;
//    private final GarageRepository garageRepository;
//    private final GarageAddressRepository garageAddressRepository;
//    private final GaragePendingActionsRepository garagePendingActionsRepository;
//    private final GarageSubscriptionRepository garageSubscriptionRepository;
//    private final GarageTechnicianRepository garageTechnicianRepository;
//    @Value("${authentication.encrypt.secret}")
//    private String encryptSecret;
//    @Value("${auth.service.email.confirm.url}")
//    private String emailVerifyUrl;


    @Override
    public GenericResponse registerUserDetails(UserRegisterRequest request) {
        String mobileNumber = "+971" + request.getMobileNumber ( );
        request.setMobileNumber ( mobileNumber );
        boolean userExists = dataValidator.checkUserAlreadyExists ( mobileNumber, request.getEmailId ( ) );
        if ( userExists ) {
            if ( ! dataValidator.checkUserIsVerified ( request.getMobileNumber ( ) ) ) {
                notificationAdaptor.callSMSServiceToSendOtp ( request.getMobileNumber ( ) );
                return new GenericResponse ( "success", "200", "Otp sent successfully", null );
            }
            throw new MemberAlreadyPresentException ( "user already exists" );
        }
        User user = userMapper.tranformUserRequestToUser ( request );
        user.setPassword ( passwordEncoder.encode ( request.getPassword ( ) ) );
        Set<Role> roles = new HashSet<> ( );
        Role userRole = roleRepository.findByName ( ERole.CUSTOMER )
                .orElseThrow ( () -> new RuntimeException ( "Error: Role is not found." ) );
        roles.add ( userRole );
        user.setRoles ( roles );
        user = userRepository.save ( user );
        return notificationAdaptor.callSMSServiceToSendOtp ( user.getMobileNumber ( ) );
    }

    @Override
    public GenericResponse verifyOtp(VerifyOtpRequest request) {
        request.setMobileNumber ( "+971" + request.getMobileNumber ( ) );

        boolean isVerified = notificationAdaptor.verifyOtp ( request );
        if ( isVerified ) {
            userRepository.findByMobileNumber ( request.getMobileNumber ( ) )
                    .ifPresent ( user -> {
                        user.setIsVerified ( true );
                        userRepository.save ( user );
                    } )
            ;
            return GenericResponse.builder ( ).status ( "success" ).code ( "200" ).message ( "otp verified" ).build ( );
        } else {
            return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( "invalid otp" ).build ( );
        }
    }


    @Override
    public GenericResponse signIn(SignInRequest loginRequest) {
        log.info ( "request -1> " + loginRequest.getMobileNumber ( ) + "   " + loginRequest.getPassword ( ) );
        log.info ( "request -2> " + loginRequest.getEmail ( ) + "   " + loginRequest.getPassword ( ) );
        User user = null;
        if ( loginRequest.getMobileNumber ( ) != null && ! loginRequest.getMobileNumber ( ).isEmpty ( ) ) {
            loginRequest.setMobileNumber ( "+971" + loginRequest.getMobileNumber ( ) );
            user = userRepository.findByMobileNumberAndIsVerified ( loginRequest.getMobileNumber ( ) ).orElseThrow ( () ->
                    new UsernameNotFoundException ( " User not found with phone number: " + loginRequest.getMobileNumber ( ) ) );
        } else if ( loginRequest.getEmail ( ) != null && ! loginRequest.getEmail ( ).isEmpty ( ) ) {
            user = userRepository.findByEmailAndIsVerified ( loginRequest.getEmail ( ) ).orElseThrow ( () ->
                    new UsernameNotFoundException ( "user not found with email: " + loginRequest.getEmail ( ) ) );
        }
        Authentication authentication = authenticationManager.authenticate (
                new UsernamePasswordAuthenticationToken ( user.getMobileNumber ( ), loginRequest.getPassword ( ) )
        );
        // 2. Set authentication in security context
        SecurityContextHolder.getContext ( ).setAuthentication ( authentication );
        // 3. Generate JWT token
        UserSecurityDetails userDetails = (UserSecurityDetails) authentication.getPrincipal ( );
        String token = jwtUtil.generateAccessToken ( userDetails );
        String refreshToken = refreshTokenService.generateRefreshToken ( user );
        // 4. Prepare success response
        return GenericResponse.builder ( )
                .status ( "success" )
                .code ( "200" )
                .message ( "Login successful" )
                .body ( AuthResponse.builder ( )
                        .accessToken ( token )
                        .refreshToken ( refreshToken )
                        .build ( ) )
                .build ( );
    }

    @Override
    public GenericResponse getAccessTokenViaRefreshToken(String refreshToken) {
        RefreshToken refreshToken1 = refreshTokenService.validateRefreshToken ( refreshToken );
        String accessToken = jwtUtil.generateAccessToken ( new UserSecurityDetails ( refreshToken1.getUser ( ) ) );
        return GenericResponse.builder ( )
                .status ( "success" )
                .code ( "200" )
                .message ( "Access token generated successfully" )
                .body ( new AuthResponse ( accessToken, refreshToken ) )
                .build ( );
    }

    @Override
    public GenericResponse resendOtp(String request) {
        return notificationAdaptor.callSMSServiceToSendOtp ( request );
    }

    @Override
    public GenericResponse forgotPassword(String phoneNumber) {
        User user = dataValidator.checkUserExistWithPhoneNumber ( phoneNumber ).orElseThrow ( () -> new ResourceNotFoundException ( "user not found with phone Number" ) );
        GenericResponse genericResponse = notificationAdaptor.callSMSServiceToSendOtp ( user.getMobileNumber ( ) );
        genericResponse.setMessage ( "Otp sent successfully for reset password" );
        return genericResponse;
    }

    @Override
    public GenericResponse logout() {
        Integer currentUserId = getCurrentUserId ( );
        return refreshTokenService.deleteRefreshTokenByUserId ( currentUserId );

    }


    @Override
    public GenericResponse resetPassword(ResetPasswordRequest request) {
        Integer currentUserId = getCurrentUserId ( );
        User user = userRepository.findById ( currentUserId ).orElseThrow ( () -> new ResourceNotFoundException ( "user not found with phoneNumber" ) );
        user.setPassword ( passwordEncoder.encode ( request.getPassword ( ) ) );
        userRepository.save ( user );
        return new GenericResponse ( "success", "200", "Password reset successfully", null );
    }

    @Override
    public GenericResponse verifyResetPasswordOtp(VerifyOtpRequest request) {
        request.setMobileNumber ( "+971" + request.getMobileNumber ( ) );
        boolean b = notificationAdaptor.verifyOtp ( request );
        if ( b ) {
            User user = userRepository.findByMobileNumber ( request.getMobileNumber ( ) ).orElseThrow ( () -> new ResourceNotFoundException ( "user not found with mobile number" ) );
            String tempAccessToken = jwtUtil.generateAccessToken ( new UserSecurityDetails ( user ), 300000 );// 5 minutes
            return GenericResponse.builder ( ).status ( "success" ).code ( "200" ).body ( tempAccessToken ).message ( "otp verified successfully" ).build ( );
        } else {
            return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( "invalid otp" ).build ( );
        }
    }

    @Override
    public UserResponse getUser(Integer id) {
        return userRepository.findById ( id ).map ( user -> {
            return UserResponse.builder ( )
                    .id ( user.getId ( ) )
                    .name ( user.getFirstName ( ).concat ( user.getLastName ( ) ) )
                    .email ( user.getEmail ( ) )
                    .mobileNumber ( user.getMobileNumber ( ) )
                    .isVerified ( user.getIsVerified ( ) )
                    .build ( );
        } ).orElseThrow ( () -> new ResourceNotFoundException ( "user not found with id: " + id ) );

    }


    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext ( ).getAuthentication ( );
        log.info ( "authentication - > " + authentication );
        if ( authentication == null || ! authentication.isAuthenticated ( ) ) {
            throw new JwtException ( "User not authenticated" );
        }
        log.info ( "authentication - > " + authentication.getName ( ) );
        String number = authentication.getName ( );
        return userRepository.findByMobileNumber ( number )
                .map ( User::getId )
                .orElseThrow ( () -> new JwtException ( "User not found" ) );
    }
@Override
public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext ( ).getAuthentication ( );
        log.info ( "authentication - > " + authentication );
        if ( authentication == null || ! authentication.isAuthenticated ( ) ) {
            throw new JwtException ( "User not authenticated" );
        }
        log.info ( "authentication - > " + authentication.getName ( ) );
        String number = authentication.getName ( );
        return userRepository.findByMobileNumber ( number )
                .orElseThrow ( () -> new JwtException ( "User not found" ) );
    }
//    @Override
//    public Customer createCustomerDetails(Customer customer) {
//        Customer savedCustomer = customerRepository.save ( customer );
//        return savedCustomer;
//    }

//    @Override
//    public String encryptString(String inputString) {
//        return AESUtil.encrypt ( inputString, "secret" );
//    }
//
//    @Override
//    public String decryptString(String inputString) {
//        return AESUtil.decrypt ( inputString, "secret" );
//    }

//    @Override
//    public GenericResponse registerCustomerDetails(CustomerRegisterRequest request) throws MemberAlreadyExistException {
//        try {
//            //username validation
//            log.info ( "Performing input validations" );
//            platformDataValidator.validateUsername ( request.getEmailId ( ) );
//            //save customer
//            Customer customer = platformObjectTransformer.transformRegisterRequestToCustomer ( request );
//            customerRepository.save ( customer );
//            log.info ( "customer saved successfully" );
//            //save address
//            CustomerAddress address = platformObjectTransformer.transformRegisterRequestToCustomerAddress ( request, customer );
//            customerAddressRepository.save ( address );
//            log.info ( "customer address saved" );
//            //save auth
//            String encryptedPwd = AESUtil.encrypt ( request.getPassword ( ), encryptSecret );
//            AuthenticationDetails authData = platformObjectTransformer.transformToAuthenticationDetails ( request, customer, encryptedPwd );
//            authenticationDetailsRepository.save ( authData );
//            log.info ( "Authentication details saved" );
//            //send verify email
//            updateAndsendEmailverificationEmail ( customer.getEmailId ( ), customer.getId ( ), customer.getCustomerType ( ) );
//            log.info ( "Triggered verification email to the registered customer" );
//            //return response
//            return GenericResponse.builder ( ).status ( "success" ).code ( "201" ).message ( "customer created" ).body ( customer ).build ( );
//        }
//        catch ( MemberAlreadyExistException exc ) {
//            throw exc;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while customer register..!" + e.getMessage ( ) );
//        }
//        return GenericResponse.builder ( ).status ( "failed" ).code ( "" ).message ( "Issue while creating customer registration" ).build ( );
//    }

//    private void updateAndsendEmailverificationEmail(String emailId, Integer memberId, String memberType) {
//        try {
//            String token = UUID.randomUUID ( ).toString ( );
//            String verificationUrl = emailVerifyUrl + "?token=" + token;
//            NotificationRequest emailRequest = prepareVerificationEmailRequest ( memberId, memberType, 0, emailId, verificationUrl );
//            GenericResponse response = notificationAdaptor.callNotificationService ( emailRequest );
//            if ( response.getStatus ( ).equals ( "success" ) ) {
//                AccountVerificationTracker verifyTracker = platformObjectTransformer.prepareAccountVerificationTracker ( memberId, memberType, token );
//                accountVerificationTrackerRepository.save ( verifyTracker );
//            }
//            log.info ( "Triggering email through SMTP successful" );
//        }
//        catch ( Exception e ) {
//            e.printStackTrace ( );
//            log.error ( "ERROR while sending the verification email" + memberId );
//        }
//    }
//

//    private NotificationRequest prepareVerificationEmailRequest(Integer memberId, String memberType, int templateId, String emailId,
//                                                                String verificationUrl) {
//        return NotificationRequest.builder ( )
//                .memberId ( memberId )
//                .memberType ( memberType )
//                .templateId ( templateId )
//                .verifyUrl ( verificationUrl )
//                .toEmailId ( emailId )
//                .build ( );
//    }

//    @Override
//    public GenericResponse verifyEmailAddress(String token) {
//        try {
//            AccountVerificationTracker tracker = accountVerificationTrackerRepository.findByToken ( token );
//            if ( null == tracker ) {
//                log.info ( "Unable to find token" );
//                return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( "Token not found" ).build ( );
//            }
//            if ( tracker.getStatus ( ).equals ( "VERIFIED" ) ) {
//                log.info ( "Token already verified" );
//                return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( "Token already verified" ).build ( );
//            }
//            if ( tracker.getVerifyEndDate ( ).before ( DateHelper.dbTimeStampp ( ) ) ) {
//                log.info ( "Token Expired" );
//                return GenericResponse.builder ( ).status ( "failed" ).code ( "500" ).message ( "Token expired" ).build ( );
//            }
//            tracker.setStatus ( "VERIFIED" );
//            tracker.setVerifiedDate ( DateHelper.dbTimeStampp ( ) );
//            accountVerificationTrackerRepository.save ( tracker );
//
//            log.info ( "Token successfully verified" );
//            if ( tracker.getMemberType ( ).equals ( "CUSTOMER" ) || tracker.getMemberType ( ).equals ( "CORPORATE" ) ) {
//                Optional<Customer> optCust = customerRepository.findById ( tracker.getMemberId ( ) );
//                Customer customer = optCust.get ( );
//                customer.setAccountStatus ( "ACTIVE" );
//                customerRepository.save ( customer );
//                log.info ( "customer successfully activated" );
//            }
//
//            if ( tracker.getMemberType ( ).equals ( Constants.GARAGE ) ) {
//                GaragePendingActions pendingAction = garagePendingActionsRepository.findByGarageId ( tracker.getMemberId ( ) );
//                pendingAction.setEmailVerifyInd ( "Y" );
//                garagePendingActionsRepository.save ( pendingAction );
//                log.info ( "Garage email successfully verified" );
//            }
//
//            if ( tracker.getMemberType ( ).equals ( Constants.GARAGE_TECH ) ) {
//                Optional<GarageTechnicians> optTech = garageTechnicianRepository.findById ( tracker.getMemberId ( ) );
//                GarageTechnicians technician = optTech.get ( );
//                technician.setAccountStatus ( "ACTIVE" );
//                garageTechnicianRepository.save ( technician );
//                log.info ( "Garage technician email/account successfully verified/activated" );
//            }
//            return GenericResponse.builder ( ).status ( "success" ).code ( "200" ).message ( "Token verified" ).build ( );
//        }
//        catch ( Exception e ) {
//            e.printStackTrace ( );
//        }
//        return null;
//    }

//    @Override
//    public GenericResponse sendVerifyEmail(SendVerifyEmailRequest request) {
//        updateAndsendEmailverificationEmail ( request.getEmailId ( ), request.getCustomerId ( ), request.getCustomerType ( ) );
//        return GenericResponse.builder ( ).status ( "success" ).code ( "200" ).message ( "Verify email sent" ).build ( );
//    }

//    @Override
//    public GenericResponse registerGarageDetails(GarageRegisterRequest request) throws MemberAlreadyExistException {
//        try {
//            //username validation
//            log.info ( "perform garage signup input validation" );
//            platformDataValidator.validateUsername ( request.getEmailId ( ) );
//            //registering garage with all required details
//            Garage garageDetails = registerGarageWithRequiredDetails ( request );
//            //send verify email
//            updateAndsendEmailverificationEmail ( garageDetails.getEmailId ( ), garageDetails.getId ( ), Constants.GARAGE );
//            //return response
//            log.info ( "verification email triggered to registered email" );
//            return GenericResponse.builder ( ).status ( "success" ).code ( "201" ).message ( "Garage registered" ).body ( garageDetails ).build ( );
//        }
//        catch ( MemberAlreadyExistException exc ) {
//            throw exc;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while garage register..!" + e.getMessage ( ) );
//        }
//        return GenericResponse.builder ( ).status ( "failed" ).code ( "" ).message ( "Issue while creating garage registration" ).build ( );
//    }

//    private Garage registerGarageWithRequiredDetails(GarageRegisterRequest request) {
//        //save Service center
//        Garage garageDetails = platformObjectTransformer.transformRegisterRequestToGarage ( request );
//        garageRepository.save ( garageDetails );
//        log.info ( "garage details saved" );
//        //save address
//        GarageAddress address = platformObjectTransformer.transformRegisterRequestToGarageAddress ( request, garageDetails );
//        garageAddressRepository.save ( address );
//        log.info ( "garage address saved" );
//        //save auth
//        String encryptedPwd = AESUtil.encrypt ( request.getPassword ( ), encryptSecret );
//        AuthenticationDetails authData = platformObjectTransformer.transformGarageToAuthenticationDetails ( request, garageDetails, encryptedPwd );
//        authenticationDetailsRepository.save ( authData );
//        log.info ( "Auth details saved" );
//        //save default pending actions
//        GaragePendingActions pendingActions = platformObjectTransformer.transformGarageToPendingActions ( request, garageDetails );
//        garagePendingActionsRepository.save ( pendingActions );
//        log.info ( "Pending actions saved" );
//        return garageDetails;
//    }

//    @Override
//    public GenericResponse genericSignIn(SignInRequest request) throws MemberNotExistException, InvalidLoginException {
//        try {
//            //check username existance
//            platformDataValidator.checkUsernameExist ( request.getUsername ( ) );
//
//            AuthenticationDetails authDetails = authenticationDetailsRepository.findByUserName ( request.getUsername ( ) );
//
//            String decryptedPwd = AESUtil.decrypt ( authDetails.getEncryptedPassword ( ), encryptSecret );
//
//            if ( ! authDetails.getUserName ( ).equalsIgnoreCase ( request.getUsername ( ) ) || ! decryptedPwd.equals ( request.getPassword ( ) ) ) {
//                log.error ( "Invalid username/password" );
//                throw new InvalidLoginException ( "Invalid username/password" );
//            }
//
//            if ( authDetails.getMemberType ( ).equals ( Constants.CUSTOMER ) ) {
//                log.info ( "Login successfull" );
//                if ( isEmailVerificationCompleted ( authDetails ) ) {
//                    Customer customerDetails = customerRepository.findById ( authDetails.getMemberId ( ) ).get ( );
//                    return GenericResponse.builder ( ).status ( "success" ).code ( "200" ).message ( "login successfull" ).body ( customerDetails ).build ( );
//                }
//                return GenericResponse.builder ( ).status ( "failed" ).code ( "200" ).message ( "Email not verified, plese verify" ).body ( null ).build ( );
//            }
//
//            if ( authDetails.getMemberType ( ).equals ( Constants.GARAGE_TECH ) ) {
//                log.info ( "Login successfully" );
//                if ( isEmailVerificationCompleted ( authDetails ) ) {
//                    GarageTechnicians technicianDetails = garageTechnicianRepository.findById ( authDetails.getMemberId ( ) ).get ( );
//                    return GenericResponse.builder ( ).status ( "success" ).code ( "200" ).message ( "login successfull" ).body ( technicianDetails ).build ( );
//                }
//                return GenericResponse.builder ( ).status ( "failed" ).code ( "200" ).message ( "Email not verified, plese verify" ).body ( null ).build ( );
//            }
//
//            if ( authDetails.getMemberType ( ).equals ( Constants.GARAGE ) ) {
//                log.info ( "Login successfully" );
//                if ( isEmailVerificationCompleted ( authDetails ) ) {
//                    Garage garageDetails = garageRepository.findById ( authDetails.getMemberId ( ) ).get ( );
//                    GarageAddress address = garageAddressRepository.findByGarageId ( garageDetails.getId ( ) );
//                    GarageSubscription subscription = garageSubscriptionRepository.findByGarageId ( garageDetails.getId ( ) );
//                    GaragePendingActions pendingActions = garagePendingActionsRepository.findByGarageId ( garageDetails.getId ( ) );
//                    return GenericResponse.builder ( ).status ( "success" ).code ( "200" ).message ( "login successfull" )
//                            .body ( GarageLoginResponse.builder ( )
//                                    .garageDetails ( garageDetails )
//                                    .address ( address )
//                                    .subscription ( subscription )
//                                    .pendingActions ( pendingActions ).build ( ) )
//                            .build ( );
//                }
//                return GenericResponse.builder ( ).status ( "failed" ).code ( "200" ).message ( "Email not verified, plese verify" ).body ( null ).build ( );
//            }
//        }
//        catch ( MemberNotExistException e ) {
//            throw e;
//        }
//        catch ( InvalidLoginException exc ) {
//            throw exc;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while customer register..!" + e.getMessage ( ) );
//        }
//        return GenericResponse.builder ( ).status ( "failed" ).code ( "" ).message ( "Issue while login" ).build ( );
//    }

//    private boolean isEmailVerificationCompleted(AuthenticationDetails authDetails) {
//        AccountVerificationTracker verifyRecord = accountVerificationTrackerRepository.findByMemberIdAndMemberTypeAndStatus ( authDetails.getMemberId ( ), authDetails.getMemberType ( ), "VERIFIED" );
//        return verifyRecord != null ? true : false;
//    }

//    @Override
//    public GenericResponse updateMemberPassword(UpdatePasswordRequest request) throws MemberNotExistException {
//        try {
//            //check auth existance
//            platformDataValidator.validateAuthenticationDetails ( request.getMemberId ( ), request.getMemberType ( ) );
//            if ( ! request.getPassword ( ).equals ( request.getConfirmPassword ( ) ) ) {
//                return GenericResponse.builder ( ).status ( "failed" ).code ( "" ).message ( "Password/Confirm password not matched" ).build ( );
//            }
//            AuthenticationDetails authDetails = authenticationDetailsRepository.findByMemberIdAndMemberType ( request.getMemberId ( ), request.getMemberType ( ) );
//            String encryptedPassword = AESUtil.encrypt ( request.getPassword ( ), encryptSecret );
//            authDetails.setCredType ( "PERM" );
//            authDetails.setEncryptedPassword ( encryptedPassword );
//            authDetails.setEffectiveDate ( DateHelper.dbTimeStampp ( ) );
//            authDetails.setExpDate ( DateHelper.getDateWith90Days ( ) );
//            authenticationDetailsRepository.save ( authDetails );
//            return GenericResponse.builder ( ).status ( "success" ).code ( "200" ).message ( "Password updated successfully" ).body ( authDetails ).build ( );
//        }
//        catch ( MemberNotExistException e ) {
//            throw e;
//        }
//        catch ( Exception e ) {
//            log.error ( "ERROR while updating password..!" + e.getMessage ( ) );
//        }
//        return GenericResponse.builder ( ).status ( "failed" ).code ( "" ).message ( "Issue while login" ).build ( );
//    }


}

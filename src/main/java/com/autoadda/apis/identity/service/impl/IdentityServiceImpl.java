package com.autoadda.apis.identity.service.impl;

import com.autoadda.apis.identity.adopter.NotificationAdaptor;
import com.autoadda.apis.identity.adopter.RecaptchaAdaptor;
import com.autoadda.apis.identity.dao.CommunicationPreferenceRepository;
import com.autoadda.apis.identity.dao.MediaFileRepository;
import com.autoadda.apis.identity.dao.RoleRepository;
import com.autoadda.apis.identity.dao.UserRepository;
import com.autoadda.apis.identity.entity.*;
import com.autoadda.apis.identity.exception.BadRequestException;
import com.autoadda.apis.identity.exception.MemberAlreadyPresentException;
import com.autoadda.apis.identity.exception.ResourceNotFoundException;
import com.autoadda.apis.identity.mapper.UserMapper;
import com.autoadda.apis.identity.request.*;
import com.autoadda.apis.identity.response.AuthResponse;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.response.UserResponse;
import com.autoadda.apis.identity.security.JwtUtil;
import com.autoadda.apis.identity.security.UserSecurityDetails;
import com.autoadda.apis.identity.service.IdentityService;
import com.autoadda.apis.identity.service.NotificationDispatcher;
import com.autoadda.apis.identity.service.RefreshTokenService;
import com.autoadda.apis.identity.util.ERole;
import com.autoadda.apis.identity.util.Status;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {

    public static final String FAILED = "failed";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String SUCCESS = "success";
    public static final String USER_NOT_FOUND_WITH_ID = "user not found with id: ";
    public static final String AUTHENTICATION = "authentication - > ";
    private final DataValidator dataValidator;
    private final UserMapper userMapper;
    private final NotificationAdaptor notificationAdaptor;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final MediaFileRepository mediafileRepository;
    private final CommunicationPreferenceRepository communicationPreferenceRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final RecaptchaAdaptor recaptchaAdaptor;


    @Override
    public GenericResponse registerUserDetails(UserRegisterRequest request) {
        if (!recaptchaAdaptor.verifyToken(request.getCaptchaToken())) {
            throw new BadRequestException("Captcha verification failed");
        }
        String mobileNumber = "+971" + request.getMobileNumber();
        log.info("1 " + mobileNumber);
        request.setMobileNumber(mobileNumber);
        boolean userExists = dataValidator.checkUserAlreadyExists(mobileNumber, request.getEmailId());
        log.info("2 " + userExists);
        if (userExists) {
            if (!dataValidator.checkUserIsVerified(request.getMobileNumber())) {
                log.info("3 user exist and the otp is send again to this user " + userExists);
                return sendOtpViaSmsAndEmail(request.getMobileNumber(), request.getEmailId());
            }
            throw new MemberAlreadyPresentException("user already exists");
        }
        User user = userMapper.tranformUserRequestToUser(request);
        log.info("4 user not exist and new user is create " + user.getMobileNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);
        user = userRepository.save(user);
        log.info("5 user  is save to db " + user.getId());
        log.info("User registered successfully: " + user.getMobileNumber());
        GenericResponse otpResponse = sendOtpViaSmsAndEmail(user.getMobileNumber(), user.getEmail());
        if (FAILED.equals(otpResponse.getStatus())) {
            return new GenericResponse(FAILED, "503",
                    "Registration successful but OTP delivery failed. Please use resend OTP.", null);
        }
        return otpResponse;
    }

    /**
     * Sends a single OTP to both the mobile number and email so OTP delivery doesn't
     * depend entirely on SMS; succeeds if at least one channel gets through.
     */
    private GenericResponse sendOtpViaSmsAndEmail(String mobileNumber, String email) {
        String otp = notificationAdaptor.generateAndStoreOtp(mobileNumber, email);
        boolean smsSent = false;
        boolean emailSent = false;
        try {
            notificationAdaptor.sendSmsOtpMessage(mobileNumber, otp);
            smsSent = true;
        } catch (Exception e) {
            log.error("Failed to send OTP via SMS to: " + mobileNumber, e);
        }
        if (email != null && !email.isBlank()) {
            try {
                notificationAdaptor.callEmailServiceToSendOtp(email, otp);
                emailSent = true;
            } catch (Exception e) {
                log.error("Failed to send OTP via Email to: " + email, e);
            }
        }
        if (!smsSent && !emailSent) {
            return new GenericResponse(FAILED, "503",
                    "OTP delivery failed via SMS and Email. Please try again or contact support.", null);
        }
        String channel = smsSent && emailSent ? "SMS and Email" : smsSent ? "SMS" : "Email";
        return new GenericResponse(SUCCESS, "200", "OTP sent successfully via " + channel + ".", null);
    }

    @Override
    @Transactional
    public GenericResponse verifyOtp(VerifyOtpRequest request) {
        request.setMobileNumber("+971" + request.getMobileNumber());
        log.info("6 user  send the verification otp for verifyinying against this number " + request.getMobileNumber());
        AtomicReference<String> userId = new AtomicReference<>("");
        boolean isVerified = notificationAdaptor.verifyOtp(request);
        log.info("7 otp for verification against this number is  " + isVerified);
        if (isVerified) {
            userRepository.findByMobileNumber(request.getMobileNumber())
                    .ifPresent(user -> {
                        user.setIsVerified(true);
                        User savedUser = userRepository.save(user);
                        userId.set(savedUser.getId().toString());
                        log.info("8 user is saved in db with verified true " + savedUser.getId());

                        // Create default communication preferences if they don't exist
                        createDefaultCommunicationPreferences(savedUser);

                        // Send welcome push notification
                        try {
                            notificationDispatcher.notifyUser(
                                    savedUser.getId().toString(),
                                    "Welcome to AutoAdda!",
                                    "Your account is verified. Start exploring our services.",
                                    "https://autoadda.mylstech.com"
                            );
                            log.info("Welcome push notification sent to user: {}", savedUser.getId());
                        } catch (Exception e) {
                            log.error("Failed to send welcome push notification to user {}: {}",
                                    savedUser.getId(), e.getMessage());
                            // Don't fail verification if notification fails
                        }
                    })
            ;
            return GenericResponse.builder().status(SUCCESS).code("200").message("otp verified Your auto adda id is " + userId).build();
        } else {
            return GenericResponse.builder().status(FAILED).code("500").message("invalid otp").build();
        }
    }


    @Override
    public GenericResponse signIn(SignInRequest loginRequest) {
        log.info("request -1> " + loginRequest.getMobileNumber() + "   " + loginRequest.getPassword());
        log.info("request -2> " + loginRequest.getEmail() + "   " + loginRequest.getPassword());
        User user = null;
        if (loginRequest.getMobileNumber() != null && !loginRequest.getMobileNumber().isEmpty()) {
            loginRequest.setMobileNumber("+971" + loginRequest.getMobileNumber());
            user = userRepository.findByMobileNumberAndIsVerified(loginRequest.getMobileNumber()).orElseGet(() -> {
                if (userRepository.findByMobileNumber(loginRequest.getMobileNumber()).isPresent()) {
                    throw new BadRequestException("Account not verified. Please verify the OTP sent to your phone number before signing in.");
                }
                throw new UsernameNotFoundException(" User not found with phone number: " + loginRequest.getMobileNumber());
            });
        } else if (loginRequest.getEmail() != null && !loginRequest.getEmail().isEmpty()) {
            user = userRepository.findByEmailAndIsVerified(loginRequest.getEmail()).orElseGet(() -> {
                if (userRepository.findByEmail(loginRequest.getEmail()).isPresent()) {
                    throw new BadRequestException("Account not verified. Please verify the OTP sent to your phone number before signing in.");
                }
                throw new UsernameNotFoundException("user not found with email: " + loginRequest.getEmail());
            });
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getMobileNumber(), loginRequest.getPassword())
        );
        // 2. Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // 3. Generate JWT token
        UserSecurityDetails userDetails = (UserSecurityDetails) authentication.getPrincipal();
        String token = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.generateRefreshToken(user);
        // 4. Prepare success response
        return GenericResponse.builder()
                .status(SUCCESS)
                .code("200")
                .message("Login successful")
                .body(AuthResponse.builder()
                        .accessToken(token)
                        .refreshToken(refreshToken)
                        .build())
                .build();
    }

    @Override
    public GenericResponse getAccessTokenViaRefreshToken(String refreshToken) {
        RefreshToken refreshToken1 = refreshTokenService.validateRefreshToken(refreshToken);
        String accessToken = jwtUtil.generateAccessToken(new UserSecurityDetails(refreshToken1.getUser()));
        return GenericResponse.builder()
                .status(SUCCESS)
                .code("200")
                .message("Access token generated successfully")
                .body(new AuthResponse(accessToken, refreshToken))
                .build();
    }

    @Override
    public GenericResponse resendOtp(String request) {
        String email = userRepository.findByMobileNumber(request).map(User::getEmail).orElse(null);
        return sendOtpViaSmsAndEmail(request, email);
    }

    @Override
    public GenericResponse forgotPassword(String phoneNumber) {
        User user = dataValidator.checkUserExistWithPhoneNumber(phoneNumber).orElseThrow(() -> new ResourceNotFoundException("user not found with phone Number"));
        GenericResponse genericResponse = sendOtpViaSmsAndEmail(user.getMobileNumber(), user.getEmail());
        if (SUCCESS.equals(genericResponse.getStatus())) {
            genericResponse.setMessage("OTP sent successfully for password reset.");
        }
        return genericResponse;
    }

    @Override
    public GenericResponse logout(String autoAddaId) {
        Integer currentUserId = Integer.parseInt(autoAddaId);
        return refreshTokenService.deleteRefreshTokenByUserId(currentUserId);

    }


    @Override
    public GenericResponse resetPassword(ResetPasswordRequest request) {
        log.info(getCurrentUserId().toString());
        Integer currentUserId = getCurrentUserId();
        User user = userRepository.findById(currentUserId).orElseThrow(() -> new ResourceNotFoundException("user not found with phoneNumber"));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return new GenericResponse(SUCCESS, "200", "Password reset successfully", null);
    }

    @Override
    public GenericResponse verifyResetPasswordOtp(VerifyOtpRequest request) {
        request.setMobileNumber("+971" + request.getMobileNumber());
        boolean b = notificationAdaptor.verifyOtp(request);
        if (b) {
            User user = userRepository.findByMobileNumber(request.getMobileNumber()).orElseThrow(() -> new ResourceNotFoundException("user not found with mobile number"));
            String tempAccessToken = jwtUtil.generateAccessToken(new UserSecurityDetails(user), 300000);// 5 minutes
            return GenericResponse.builder().status(SUCCESS).code("200").body(tempAccessToken).message("otp verified successfully").build();
        } else {
            return GenericResponse.builder().status(FAILED).code("500").message("invalid otp").build();
        }
    }

    @Override
    public UserResponse getUser(Integer id) {
        return userRepository.findById(id).map(user ->
             UserResponse.builder()
                    .id(user.getId())
                    .name(user.getFirstName().concat(" " + user.getLastName()))
                    .email(user.getEmail())
                    .mobileNumber(user.getMobileNumber())
                    .isVerified(user.getIsVerified())
                    .profileImage(user.getProfileImage().isEmpty() ? null : user.getProfileImage().get(0))
                    .build()
        ).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_WITH_ID + id));
    }


    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info(AUTHENTICATION + authentication);
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new JwtException("User not authenticated");
        }
        if (authentication.getPrincipal() instanceof UserSecurityDetails userDetails) {
            return userDetails.getUser().getId();
        }
        log.info(AUTHENTICATION + authentication.getName());
        String number = authentication.getName();
        return userRepository.findByMobileNumber(number)
                .map(User::getId)
                .orElseThrow(() -> new JwtException(USER_NOT_FOUND));
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info(AUTHENTICATION + authentication);
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new JwtException("User not authenticated");
        }
        if (authentication.getPrincipal() instanceof UserSecurityDetails userDetails) {
            return userDetails.getUser();
        }
        log.info(AUTHENTICATION + authentication.getName());
        String number = authentication.getName();
        return userRepository.findByMobileNumber(number)
                .orElseThrow(() -> new JwtException(USER_NOT_FOUND));
    }

    @Override
    public GenericResponse updateRole(Integer id, ERole role) {
        Role role1 = roleRepository.findByName(role).orElseThrow(() -> new ResourceNotFoundException("role not found with name: " + role));
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_WITH_ID + id));
        Set<Role> roles = user.getRoles();
        roles.add(role1);
        user.setRoles(roles);
        User save = userRepository.save(user);
        return GenericResponse.builder()
                .status(SUCCESS)
                .code("200")
                .message("role added successfully")
                .body(save.getRoles())
                .build();
    }

    @Override
    public GenericResponse removeRole(Integer id, ERole role) {
        Role role1 = roleRepository.findByName(role).orElseThrow(() -> new ResourceNotFoundException("role not found with name: " + role));
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_WITH_ID + id));
        Set<Role> roles = user.getRoles();
        roles.remove(role1);
        user.setRoles(roles);
        User save = userRepository.save(user);
        return GenericResponse.builder()
                .status(SUCCESS)
                .code("200")
                .message("role added successfully")
                .body(save.getRoles())
                .build();
    }

    @Override
    public UserResponse getUserDetailByPhoneNumber(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber).orElseThrow(() -> new ResourceNotFoundException("user not found with mobile number: " + mobileNumber));
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getFirstName().concat(" " + user.getLastName()))
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .profileImage(user.getProfileImage().isEmpty() ? null : user.getProfileImage().get(0))
                .isVerified(user.getIsVerified())
                .build();
    }

    @Override
    @Transactional
    public GenericResponse updateUserProfile(UserUpdateRequest request) {
        // Fetch fresh User from database to avoid LazyInitializationException
        Integer userId = getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (request.getFirstName() != null) {
            currentUser.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            currentUser.setLastName(request.getLastName());
        }
        if (request.getProfileImageId() != null) {
            MediaFile profile = mediafileRepository.findById(request.getProfileImageId()).orElseThrow(() -> new ResourceNotFoundException("image not found"));
            currentUser.getProfileImage().clear();
            currentUser.getProfileImage().add(profile);
        }
        User user = userRepository.save(currentUser);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .name(user.getFirstName().concat(" " + user.getLastName()))
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .isVerified(user.getIsVerified())
                .profileImage(user.getProfileImage().isEmpty() ? null : user.getProfileImage().get(0))
                .build();

        return GenericResponse
                .builder()
                .status(SUCCESS)
                .code("200")
                .body(userResponse)
                .build();
    }

    /**
     * Creates default communication preferences for a newly verified user
     *
     * @param user The user for whom to create preferences
     */
    private void createDefaultCommunicationPreferences(User user) {
        // Check if preferences already exist
        if (communicationPreferenceRepository.findByUserId(user.getId()).isPresent()) {
            log.info("Communication preferences already exist for user ID: {}", user.getId());
            return;
        }

        log.info("Creating default communication preferences for user ID: {}", user.getId());

        // Build default preferences using builder pattern
        CommunicationPreference defaultPreferences = CommunicationPreference.builder()
                .user(user)
                // Critical notifications - enabled by default
                .emailOnServiceUpdates(true)
                .smsOnServiceUpdates(true)
                .emailOnAppointments(true)
                .smsOnAppointments(true)
                .emailOnReminders(true)
                .smsOnReminders(true)
                .emailOnQuotationRequests(true)
                .smsOnQuotationRequests(true)
                // Marketing - disabled by default (opt-in)
                .emailOnOffers(false)
                .smsOnOffers(false)
                // Status
                .status(Status.ACTIVE)
                .build();

        communicationPreferenceRepository.save(defaultPreferences);
        log.info("Successfully created default communication preferences for user ID: {}", user.getId());
    }

}

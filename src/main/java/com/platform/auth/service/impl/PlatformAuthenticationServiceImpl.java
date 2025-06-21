package com.platform.auth.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.platform.auth.adopter.PlatformNotificationAdaptor;
import com.platform.auth.dao.AccountVerificationTrackerRepository;
import com.platform.auth.dao.AuthenticationDetailsRepository;
import com.platform.auth.dao.CustomerAddressRepository;
import com.platform.auth.dao.CustomerRepository;
import com.platform.auth.dao.GarageAddressRepository;
import com.platform.auth.dao.GaragePendingActionsRepository;
import com.platform.auth.dao.GarageRepository;
import com.platform.auth.dao.GarageSubscriptionRepository;
import com.platform.auth.dao.GarageTechnicianRepository;
import com.platform.auth.entity.AccountVerificationTracker;
import com.platform.auth.entity.AuthenticationDetails;
import com.platform.auth.entity.Customer;
import com.platform.auth.entity.CustomerAddress;
import com.platform.auth.entity.Garage;
import com.platform.auth.entity.GarageAddress;
import com.platform.auth.entity.GaragePendingActions;
import com.platform.auth.entity.GarageSubscription;
import com.platform.auth.entity.GarageTechnicians;
import com.platform.auth.exception.InvalidLoginException;
import com.platform.auth.exception.MemberAlreadyExistException;
import com.platform.auth.exception.MemberNotExistException;
import com.platform.auth.reqest.CustomerRegisterRequest;
import com.platform.auth.reqest.GarageRegisterRequest;
import com.platform.auth.reqest.NotificationRequest;
import com.platform.auth.reqest.SendVerifyEmailRequest;
import com.platform.auth.reqest.SignInRequest;
import com.platform.auth.reqest.UpdatePasswordRequest;
import com.platform.auth.response.GarageLoginResponse;
import com.platform.auth.response.GenericResponse;
import com.platform.auth.service.PlatformAuthenticationService;
import com.platform.auth.transformers.PlatformObjectTransformer;
import com.platform.auth.util.AESUtil;
import com.platform.auth.util.Constants;
import com.platform.auth.util.DateHelper;
import com.platform.auth.validators.PlatformDataValidator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PlatformAuthenticationServiceImpl implements PlatformAuthenticationService {

	@Autowired
	private CustomerRepository customerRepository;
	
	@Autowired
	private PlatformDataValidator platformDataValidator;
	
	@Autowired
	private PlatformObjectTransformer platformObjectTransformer;
	
	@Autowired
	private CustomerAddressRepository customerAddressRepository;
	
	@Value("${authentication.encrypt.secret}")
	private String encryptSecret;
	
	@Value("${platform.auth.service.email.confirm.url}")
	private String emailVerifyUrl;
	
	@Autowired
	private PlatformNotificationAdaptor platformNotificationAdaptor;
	
	@Autowired
	private AuthenticationDetailsRepository authenticationDetailsRepository;
	
	@Autowired
	private AccountVerificationTrackerRepository accountVerificationTrackerRepository;

	
	@Autowired
	private GarageRepository garageRepository;
	
	@Autowired
	private GarageAddressRepository garageAddressRepository;
	
	@Autowired
	private GaragePendingActionsRepository garagePendingActionsRepository;
	
	@Autowired
	private GarageSubscriptionRepository garageSubscriptionRepository;
	
	@Autowired
	private GarageTechnicianRepository garageTechnicianRepository; 
	
	
	@Override
	public Customer createCustomerDetails(Customer customer) {
		Customer savedCustomer = customerRepository.save(customer);
		return savedCustomer;
	}

	@Override
	public String encryptString(String inputString) {
		return AESUtil.encrypt(inputString, "secret");
	}

	@Override
	public String decryptString(String inputString) {
		return AESUtil.decrypt(inputString, "secret");
	}

	@Override
	public GenericResponse registerCustomerDetails(CustomerRegisterRequest request) throws MemberAlreadyExistException {
		try {
			//username validation
			log.info("Performing input validations");
			platformDataValidator.validateUsername(request.getEmailId());
			//save customer
			Customer customer = platformObjectTransformer.transformRegisterRequestToCustomer(request);
			customerRepository.save(customer);
			log.info("customer saved successfully");
			//save address
			CustomerAddress address = platformObjectTransformer.transformRegisterRequestToCustomerAddress(request, customer);
			customerAddressRepository.save(address);
			log.info("customer address saved");
			//save auth
			String encryptedPwd = AESUtil.encrypt(request.getPassword(), encryptSecret);
			AuthenticationDetails authData = platformObjectTransformer.transformToAuthenticationDetails(request, customer, encryptedPwd);
			authenticationDetailsRepository.save(authData);
			log.info("Authentication details saved");
			//send verify email
			updateAndsendEmailverificationEmail(customer.getEmailId(), customer.getId(), customer.getCustomerType());
			log.info("Triggered verification email to the registered customer");
			//return response
			return GenericResponse.builder().status("success").code("201").message("customer created").body(customer).build();
		}catch (MemberAlreadyExistException exc) {
			throw exc;
		} catch (Exception e) {
			log.error("ERROR while customer register..!"+e.getMessage());
		}
		return GenericResponse.builder().status("failed").code("").message("Issue while creating customer registration").build();
	}

	private void updateAndsendEmailverificationEmail(String emailId, Integer memberId, String memberType) {
		try {
			String token = UUID.randomUUID().toString();
			String verificationUrl = emailVerifyUrl +"?token=" + token;
			NotificationRequest emailRequest = prepareVerificationEmailRequest(memberId, memberType, 0, emailId, verificationUrl);
			GenericResponse response = platformNotificationAdaptor.callNotificationService(emailRequest);
			if(response.getStatus().equals("success")) {
				AccountVerificationTracker verifyTracker = platformObjectTransformer.prepareAccountVerificationTracker(memberId, memberType, token);
				accountVerificationTrackerRepository.save(verifyTracker);
			}
			log.info("Triggering email through SMTP successful");
		} catch (Exception e) {
			e.printStackTrace();
			log.error("ERROR while sending the verification email"+memberId);
		}
	}

	
	private NotificationRequest prepareVerificationEmailRequest(Integer memberId, String memberType, int templateId, String emailId,
			String verificationUrl) {
		return NotificationRequest.builder()
				.memberId(memberId)
				.memberType(memberType)
				.templateId(templateId)
				.verifyUrl(verificationUrl)
				.toEmailId(emailId)
				.build();
	}

	@Override
	public GenericResponse verifyEmailAddress(String token) {
		try {
			AccountVerificationTracker tracker = accountVerificationTrackerRepository.findByToken(token);
			if(null == tracker) {
				log.info("Unable to find token");
				return GenericResponse.builder().status("failed").code("500").message("Token not found").build();
			}
			if(tracker.getStatus().equals("VERIFIED")) {
				log.info("Token already verified");
				return GenericResponse.builder().status("failed").code("500").message("Token already verified").build();
			}
			if(tracker.getVerifyEndDate().before(DateHelper.dbTimeStampp())) {
				log.info("Token Expired");
				return GenericResponse.builder().status("failed").code("500").message("Token expired").build();
			}
			tracker.setStatus("VERIFIED");
			tracker.setVerifiedDate(DateHelper.dbTimeStampp());
			accountVerificationTrackerRepository.save(tracker);
			
			log.info("Token successfully verified");
			if(tracker.getMemberType().equals("CUSTOMER") || tracker.getMemberType().equals("CORPORATE")) {
				Optional<Customer> optCust = customerRepository.findById(tracker.getMemberId());
				Customer customer = optCust.get();
				customer.setAccountStatus("ACTIVE");
				customerRepository.save(customer);
				log.info("customer successfully activated");
			}
			
			if(tracker.getMemberType().equals(Constants.GARAGE)) {
				GaragePendingActions pendingAction = garagePendingActionsRepository.findByGarageId(tracker.getMemberId());
				pendingAction.setEmailVerifyInd("Y");
				garagePendingActionsRepository.save(pendingAction);
				log.info("Garage email successfully verified");
			}
			
			if(tracker.getMemberType().equals(Constants.GARAGE_TECH)) {
				Optional<GarageTechnicians> optTech = garageTechnicianRepository.findById(tracker.getMemberId());
				GarageTechnicians technician = optTech.get();				technician.setAccountStatus("ACTIVE");
				garageTechnicianRepository.save(technician);
				log.info("Garage technician email/account successfully verified/activated");
			}
			return GenericResponse.builder().status("success").code("200").message("Token verified").build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public GenericResponse sendVerifyEmail(SendVerifyEmailRequest request) {
		updateAndsendEmailverificationEmail(request.getEmailId(), request.getCustomerId(), request.getCustomerType());
		return GenericResponse.builder().status("success").code("200").message("Verify email sent").build();
	}

	@Override
	public GenericResponse registerGarageDetails(GarageRegisterRequest request) throws MemberAlreadyExistException {
		try {
			//username validation
			log.info("perform garage signup input validation");
			platformDataValidator.validateUsername(request.getEmailId());
			//registering garage with all required details
			Garage garageDetails = registerGarageWithRequiredDetails(request);
			//send verify email
			updateAndsendEmailverificationEmail(garageDetails.getEmailId(), garageDetails.getId(), Constants.GARAGE);
			//return response
			log.info("verification email triggered to registered email");
			return GenericResponse.builder().status("success").code("201").message("Garage registered").body(garageDetails).build();
		}catch (MemberAlreadyExistException exc) {
			throw exc;
		} catch (Exception e) {
			log.error("ERROR while garage register..!"+e.getMessage());
		}
		return GenericResponse.builder().status("failed").code("").message("Issue while creating garage registration").build();
	}
	
	private Garage registerGarageWithRequiredDetails(GarageRegisterRequest request) {
		//save Service center
		Garage garageDetails = platformObjectTransformer.transformRegisterRequestToGarage(request);
		garageRepository.save(garageDetails);
		log.info("garage details saved");
		//save address
		GarageAddress address = platformObjectTransformer.transformRegisterRequestToGarageAddress(request, garageDetails);
		garageAddressRepository.save(address);
		log.info("garage address saved");
		//save auth
		String encryptedPwd = AESUtil.encrypt(request.getPassword(), encryptSecret);
		AuthenticationDetails authData = platformObjectTransformer.transformGarageToAuthenticationDetails(request, garageDetails, encryptedPwd);
		authenticationDetailsRepository.save(authData);
		log.info("Auth details saved");
		//save default pending actions
		GaragePendingActions pendingActions = platformObjectTransformer.transformGarageToPendingActions(request, garageDetails);
		garagePendingActionsRepository.save(pendingActions);
		log.info("Pending actions saved");
		return garageDetails;
	}

	@Override
	public GenericResponse genericSignIn(SignInRequest request) throws MemberNotExistException, InvalidLoginException{
		try {
			//check username existance
			platformDataValidator.checkUsernameExist(request.getUsername());
			
			AuthenticationDetails authDetails = authenticationDetailsRepository.findByUserName(request.getUsername());
			
			String decryptedPwd = AESUtil.decrypt(authDetails.getEncryptedPassword(), encryptSecret);
			
			if(!authDetails.getUserName().equalsIgnoreCase(request.getUsername()) || !decryptedPwd.equals(request.getPassword())) {
				log.error("Invalid username/password");
				throw new InvalidLoginException("Invalid username/password");
			}
			
			if(authDetails.getMemberType().equals(Constants.CUSTOMER)) {
				log.info("Login successfull");
				if(isEmailVerificationCompleted(authDetails)) {
					Customer customerDetails = customerRepository.findById(authDetails.getMemberId()).get();
					return GenericResponse.builder().status("success").code("200").message("login successfull").body(customerDetails).build();
				}
				return GenericResponse.builder().status("failed").code("200").message("Email not verified, plese verify").body(null).build();
			}
			
			if(authDetails.getMemberType().equals(Constants.GARAGE_TECH)) {
				log.info("Login successfull");
				if(isEmailVerificationCompleted(authDetails)) {
					GarageTechnicians technicianDetails = garageTechnicianRepository.findById(authDetails.getMemberId()).get();
					return GenericResponse.builder().status("success").code("200").message("login successfull").body(technicianDetails).build();
				}
				return GenericResponse.builder().status("failed").code("200").message("Email not verified, plese verify").body(null).build();
			}
			
			if(authDetails.getMemberType().equals(Constants.GARAGE)) {
				log.info("Login successfull");
				if(isEmailVerificationCompleted(authDetails)) {
					Garage garageDetails = garageRepository.findById(authDetails.getMemberId()).get();
					GarageAddress address = garageAddressRepository.findByGarageId(garageDetails.getId());
					GarageSubscription subscription = garageSubscriptionRepository.findByGarageId(garageDetails.getId());
					GaragePendingActions pendingActions = garagePendingActionsRepository.findByGarageId(garageDetails.getId());
					return GenericResponse.builder().status("success").code("200").message("login successfull")
							.body(GarageLoginResponse.builder()
									.garageDetails(garageDetails)
									.address(address)
									.subscription(subscription)
									.pendingActions(pendingActions).build())
							.build();
				}
				return GenericResponse.builder().status("failed").code("200").message("Email not verified, plese verify").body(null).build();
			}
		} catch (MemberNotExistException e) {
			throw e;
		}catch (InvalidLoginException exc) {
			throw exc;
		} catch (Exception e) {
			log.error("ERROR while customer register..!"+e.getMessage());
		}
		return GenericResponse.builder().status("failed").code("").message("Issue while login").build();
	}

	private boolean isEmailVerificationCompleted(AuthenticationDetails authDetails) {
		AccountVerificationTracker verifyRecord = accountVerificationTrackerRepository.findByMemberIdAndMemberTypeAndStatus(authDetails.getMemberId(), authDetails.getMemberType(), "VERIFIED");
		return verifyRecord != null ? true : false;
	}

	@Override
	public GenericResponse updateMemberPassword(UpdatePasswordRequest request) throws MemberNotExistException {
		try {
			//check auth existance
			platformDataValidator.validateAuthenticationDetails(request.getMemberId(), request.getMemberType());
			if(!request.getPassword().equals(request.getConfirmPassword())) {
				return GenericResponse.builder().status("failed").code("").message("Password/Confirm password not matched").build();
			}
			AuthenticationDetails authDetails = authenticationDetailsRepository.findByMemberIdAndMemberType(request.getMemberId(), request.getMemberType());
			String encryptedPassword = AESUtil.encrypt(request.getPassword(), encryptSecret);
			authDetails.setCredType("PERM");
			authDetails.setEncryptedPassword(encryptedPassword);
			authDetails.setEffectiveDate(DateHelper.dbTimeStampp());
			authDetails.setExpDate(DateHelper.getDateWith90Days());
			authenticationDetailsRepository.save(authDetails);
			return GenericResponse.builder().status("success").code("200").message("Password updated successfully").body(authDetails).build();
		} catch (MemberNotExistException e) {
			throw e;
		}catch (Exception e) {
			log.error("ERROR while updating password..!"+e.getMessage());
		}
		return GenericResponse.builder().status("failed").code("").message("Issue while login").build();
	}

}

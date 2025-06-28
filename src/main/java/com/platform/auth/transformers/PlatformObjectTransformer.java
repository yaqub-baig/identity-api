package com.platform.auth.transformers;

import com.platform.auth.entity.*;
import com.platform.auth.reqest.UserRegisterRequest;
import org.springframework.stereotype.Component;

import com.platform.auth.reqest.CustomerRegisterRequest;
import com.platform.auth.reqest.GarageRegisterRequest;
import com.platform.auth.util.Constants;
import com.platform.auth.util.DateHelper;

@Component
public class PlatformObjectTransformer {
	
	public Customer transformRegisterRequestToCustomer(CustomerRegisterRequest request) {
		return Customer.builder()
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.emiratesId(request.getEmiratesId())
				.emailId(request.getEmailId())
				.customerType(request.getCorporateInd().equals("Y") ? Constants.CORPORATE : Constants.CUSTOMER)
				.mobileNumber(request.getMobileNumber())
				.companyName(request.getCompanyName())
				.vatNumber(request.getVatNumber())
				.webSite(request.getWebSite())
				.accountStatus(Constants.INACTIVE)
				.insertTms(DateHelper.dbTimeStampp())
				.build();
	}
	
	public AuthenticationDetails transformToAuthenticationDetails(CustomerRegisterRequest request, Customer customer, String encryptedPwd) {
		return AuthenticationDetails.builder()
				.encryptedPassword(encryptedPwd)
				.memberId(customer.getId())
				.memberType(customer.getCustomerType())
				.userName(customer.getEmailId())
				.lastLoggedTms(DateHelper.dbTimeStampp())
				.effectiveDate(DateHelper.dbTimeStampp())
				.expDate(DateHelper.getDateWith90Days())
				.credType("PERMANANT")
				.build();
	}
	
	public CustomerAddress transformRegisterRequestToCustomerAddress(CustomerRegisterRequest request, Customer customer) {
		return CustomerAddress.builder()
				.streetAddress1(request.getAddress().getStreetAddress1())
				.streetAddress2(request.getAddress().getStreetAddress2())
				.city(request.getAddress().getCity())
				.county(request.getAddress().getCounty())
				.country(request.getAddress().getCountry())
				.postalCode(request.getAddress().getPostalCode())
				.state(request.getAddress().getState())
				.memberId(customer.getId())
				.memberType(customer.getCustomerType())
				.insertTms(DateHelper.dbTimeStampp())
				.build();
	}
	
	public AccountVerificationTracker prepareAccountVerificationTracker(int memberId, String memberType, String token) {
		return AccountVerificationTracker.builder()
				.memberId(memberId)
				.memberType(memberType)
				.token(token)
				.status(Constants.NOT_VERIFIED)
				.verifyStartDate(DateHelper.dbTimeStampp())
				.verifyEndDate(DateHelper.getVerifyEndDate())
				.build();
	}
	
	
	public Garage transformRegisterRequestToGarage(GarageRegisterRequest request) {
		return Garage.builder()
				.legalName(request.getLegalName())
				.garageName(request.getGarageName())
				.emailId(request.getEmailId())
				.garageType(request.getGarageType().equals("INDIVIDUAL") ? Constants.INDIVIDUAL : Constants.CHAIN)
				.contactNumber(request.getContactNumber())
				.webSite(request.getWebSite())
				.accountStatus(Constants.INACTIVE)
				.enableSearchInd("N")
				.insertTms(DateHelper.dbTimeStampp())
				.build();
	}
	
	public AuthenticationDetails transformGarageToAuthenticationDetails(GarageRegisterRequest request, Garage serviceCenter, String encryptedPwd) {
		return AuthenticationDetails.builder()
				.encryptedPassword(encryptedPwd)
				.memberId(serviceCenter.getId())
				.memberType(Constants.GARAGE)
				.userName(serviceCenter.getEmailId())
				.lastLoggedTms(DateHelper.dbTimeStampp())
				.build();
	}
	
	public GarageAddress transformRegisterRequestToGarageAddress(GarageRegisterRequest request, Garage serviceCenter) {
		return GarageAddress.builder()
				.streetAddress1(request.getAddress().getStreetAddress1())
				.streetAddress2(request.getAddress().getStreetAddress2())
				.city(request.getAddress().getCity())
				.county(request.getAddress().getCounty())
				.country(request.getAddress().getCountry())
				.postalCode(request.getAddress().getPostalCode())
				.state(request.getAddress().getState())
				.garageId(serviceCenter.getId())
				.googleMapLink(request.getAddress().getGoogleMapLink())
				.insertTms(DateHelper.dbTimeStampp())
				.build();
	}

	public GaragePendingActions transformGarageToPendingActions(GarageRegisterRequest request, Garage garageDetails) {
		return GaragePendingActions.builder()
				.garageId(garageDetails.getId())
				.licenseUpdateInd("N")
				.ownerUpdateInd("N")
				.paymentInd("N")
				.techUpdateInd("N")
				.serviceUpdateInd("N")
				.searchInd("N")
				.insertTms(DateHelper.dbTimeStampp())
				.build();
	}

    public User tranformUserRequestToUser(UserRegisterRequest request) {
		return User.builder ( )
				.firstName ( request.getFirstName ( ) )
				.lastName ( request.getLastName ( ) )
				.email( request.getEmailId ( ) )
				.mobileNumber ( request.getMobileNumber ( ) )
				.isVerified ( false )
				.build ( );
    }
}

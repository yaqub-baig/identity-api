package com.platform.auth.validators;

import com.platform.auth.dao.UserRepository;
import com.platform.auth.exception.MemberAlreadyPresentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.platform.auth.dao.AuthenticationDetailsRepository;
import com.platform.auth.entity.AuthenticationDetails;
import com.platform.auth.exception.MemberAlreadyExistException;
import com.platform.auth.exception.MemberNotExistException;

@Component
public class PlatformDataValidator {
	
	@Autowired
	private AuthenticationDetailsRepository authenticationDetailsRepository;

	@Autowired
	private UserRepository userRepository;
	
	public void validateUsername(String username) throws MemberAlreadyExistException {
		AuthenticationDetails authData = authenticationDetailsRepository.findByUserName(username);
		if(null != authData) {
			throw new MemberAlreadyExistException("Username already exist");
		}
	}
	
	public void checkUsernameExist(String username) throws MemberNotExistException {
		AuthenticationDetails authData = authenticationDetailsRepository.findByUserName(username);
		if(null == authData) {
			throw new MemberNotExistException("Username not exist");
		}
	}
	
	public void validateAuthenticationDetails(Integer memberId, String memberType) throws MemberNotExistException {
		AuthenticationDetails authData = authenticationDetailsRepository.findByMemberIdAndMemberType(memberId, memberType);
		if(null == authData) {
			throw new MemberNotExistException("Authentication details not exist");
		}
	}

	public void checkUserAlreadyExists(String mobileNumber){
		 if(userRepository.existsByMobileNumber ( mobileNumber )){
			 throw new MemberAlreadyPresentException ( "user already exists" );
		 }

	}

}

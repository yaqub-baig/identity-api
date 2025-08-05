package com.service.identity.validators;

import com.service.identity.dao.UserRepository;
import com.service.identity.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataValidator {
//	private final AuthenticationDetailsRepository authenticationDetailsRepository;
    private final UserRepository userRepository;

    public boolean checkUserAlreadyExists(String mobileNumber, String email) {
        return userRepository.existsByMobileNumber ( mobileNumber ) && userRepository.existsByEmail ( email );
    }

    public boolean checkUserIsVerified(String mobileNumber) {
        return userRepository.existsByMobileNumberAndIsVerified ( mobileNumber );
    }

    public Optional<User> checkUserExistWithPhoneNumber(String phoneNumber) {
        return userRepository.findByMobileNumber ( phoneNumber );
    }

//	public void validateUsername(String username) throws MemberAlreadyExistException {
//		AuthenticationDetails authData = authenticationDetailsRepository.findByUserName(username);
//		if(null != authData) {
//			throw new MemberAlreadyExistException("Username already exist");
//		}
//	}

//	public void checkUsernameExist(String username) throws MemberNotExistException {
//		AuthenticationDetails authData = authenticationDetailsRepository.findByUserName(username);
//		if(null == authData) {
//			throw new MemberNotExistException("Username not exist");
//		}
//	}

//	public void validateAuthenticationDetails(Integer memberId, String memberType) throws MemberNotExistException {
//		AuthenticationDetails authData = authenticationDetailsRepository.findByMemberIdAndMemberType(memberId, memberType);
//		if(null == authData) {
//			throw new MemberNotExistException("Authentication details not exist");
//		}
//	}

}

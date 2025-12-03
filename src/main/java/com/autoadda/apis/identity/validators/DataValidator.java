package com.autoadda.apis.identity.validators;

import com.autoadda.apis.identity.dao.UserRepository;
import com.autoadda.apis.identity.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataValidator {

    private final UserRepository userRepository;

    public boolean checkUserAlreadyExists(String mobileNumber, String email) {
        return userRepository.existsByMobileNumber(mobileNumber) || userRepository.existsByEmail(email);
    }

    public boolean checkUserIsVerified(String mobileNumber) {
        return userRepository.existsByMobileNumberAndIsVerified(mobileNumber);
    }

    public Optional<User> checkUserExistWithPhoneNumber(String phoneNumber) {
        return userRepository.findByMobileNumber(phoneNumber);
    }

}

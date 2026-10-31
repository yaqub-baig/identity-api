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

    public boolean checkUserExistsWithPhoneNumber(String mobileNumber) {
        return userRepository.existsByMobileNumber(mobileNumber);
    }

    public boolean checkUserExistsWithEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean checkUserIsVerified(String mobileNumber) {
        return userRepository.existsByMobileNumberAndIsVerified(mobileNumber);
    }

    public Optional<User> checkUserExistWithPhoneNumber(String phoneNumber) {
        return userRepository.findByMobileNumber(phoneNumber);
    }

}

package com.autoadda.apis.identity.mapper;


import com.autoadda.apis.identity.entity.User;
import com.autoadda.apis.identity.request.UserRegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User tranformUserRequestToUser(UserRegisterRequest request) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmailId())
                .mobileNumber(request.getMobileNumber())
                .isVerified(false)
                .build();
    }

}

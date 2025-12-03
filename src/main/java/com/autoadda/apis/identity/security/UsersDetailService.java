package com.autoadda.apis.identity.security;

import com.autoadda.apis.identity.dao.UserRepository;
import com.autoadda.apis.identity.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsersDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByMobileNumber(phone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone number: " + phone));

        return new UserSecurityDetails(user);
    }
}

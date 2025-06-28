package com.platform.auth.config;

import com.platform.auth.dao.RoleRepository;
import com.platform.auth.entity.Role;
import com.platform.auth.util.ERole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;


    @Override
    public void run(String... args) {
        // Initialize roles if they don't exist
        Arrays.stream ( ERole.values ( ) ).forEach ( role -> {
            if ( ! roleRepository.existsByName ( role ) ) {
                Role newRole = new Role ( );
                newRole.setName ( role );
                roleRepository.save ( newRole );
                log.info ( "Initialized role: " + role );
            }
        } );
    }
}

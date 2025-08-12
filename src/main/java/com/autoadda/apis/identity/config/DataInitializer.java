package com.autoadda.apis.identity.config;

import com.autoadda.apis.identity.dao.RoleRepository;
import com.autoadda.apis.identity.entity.Role;
import com.autoadda.apis.identity.util.ERole;
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

        if (roleRepository.findAll().isEmpty()) {
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
}

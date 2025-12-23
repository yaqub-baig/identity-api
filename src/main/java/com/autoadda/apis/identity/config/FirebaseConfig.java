package com.autoadda.apis.identity.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${app.firebase-config-file:google/firebase-service-account.json}")
    private String firebaseConfigPath;
    private FirebaseAuth firebaseAuth;

    @PostConstruct
    public void initialize() {
        try (InputStream serviceAccount = new ClassPathResource ( firebaseConfigPath ).getInputStream ( )) {
            FirebaseOptions options = FirebaseOptions.builder ( )
                    .setCredentials ( GoogleCredentials.fromStream ( serviceAccount ) )
                    .build ( );
            if ( FirebaseApp.getApps ( ).isEmpty ( ) ) {
                FirebaseApp.initializeApp ( options );
//                log.info ( "Firebase has been initialized" );
            }
            firebaseAuth = FirebaseAuth.getInstance();
        } catch ( IOException e) {
//            log.error("Could not initialize Firebase: {}", e.getMessage());
        }
    }
}

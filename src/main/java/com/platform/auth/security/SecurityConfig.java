package com.platform.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UsersDetailService usersDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf ( AbstractHttpConfigurer::disable )
                .cors ( cors -> cors.configurationSource ( corsConfigurationSource ( ) ) )

                // For Spring Boot 2.7.14 (Spring Security 5.7.x)
                .authorizeRequests ( auth -> auth
                        .antMatchers ( HttpMethod.POST,"/auth/user/**")
                        .permitAll ( )
                        .antMatchers (
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**").permitAll ()
                        .anyRequest ( )
                        .authenticated ( )
                )

                .exceptionHandling ( exception -> exception
                        .authenticationEntryPoint ( (request, response, authException) -> {
                            log.error ( "Unauthorized error: {}", authException.getMessage ( ) );
                            response.setStatus ( 401 );
                            response.setContentType ( "application/json" );
                            response.getWriter ( ).write ( "{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage ( ) + "\"}" );
                        } )
                        .accessDeniedHandler ( (request, response, accessDeniedException) -> {
                            log.error ( "Forbidden error: {}", accessDeniedException.getMessage ( ) );
                            response.setStatus ( 403 );
                            response.setContentType ( "application/json" );
                            response.getWriter ( ).write ( "{\"error\":\"Forbidden\",\"message\":\"" + accessDeniedException.getMessage ( ) + "\"}" );
                        } )
                )

                .sessionManagement ( session -> session
                        .sessionCreationPolicy ( SessionCreationPolicy.STATELESS )
                )

                .authenticationProvider ( authenticationProvider ( ) )
                .addFilterBefore ( jwtAuthFilter, UsernamePasswordAuthenticationFilter.class )
                .build ( );
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider ( );
        authProvider.setUserDetailsService ( usersDetailsService );
        authProvider.setPasswordEncoder ( passwordEncoder ( ) );
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder ( );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration ( );

        // More secure CORS configuration
        config.setAllowedOriginPatterns ( Arrays.asList (
                "http://localhost:5174",
                "http://localhost:5173"
        ) );

        config.setAllowedMethods ( Arrays.asList (
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ) );

        config.setAllowedHeaders ( Arrays.asList (
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "skip_zrok_interstitial"
        ) );

        config.setExposedHeaders ( Arrays.asList (
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ) );

        config.setAllowCredentials ( true );
        config.setMaxAge ( 3600L ); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource ( );
        source.registerCorsConfiguration ( "/**", config );
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager ( );
    }
}
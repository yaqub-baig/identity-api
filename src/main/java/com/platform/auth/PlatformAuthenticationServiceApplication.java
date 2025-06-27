package com.platform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class PlatformAuthenticationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlatformAuthenticationServiceApplication.class, args);
	}

}

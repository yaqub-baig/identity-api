package com.platform.auth.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenAPIConfig {

	@Value("${platform.auth.service.base.url}")
	private String devUrl;

	@Value("${platform.auth.service.base.prod.url}")
	private String prodUrl;

	@Bean
	public OpenAPI myOpenAPI() {
		final String securitySchemeName = "bearerAuth";
		Server devServer = new Server();
		devServer.setUrl(devUrl);
		devServer.setDescription("Server URL in Development environment");

		Server prodServer = new Server();
		prodServer.setUrl(prodUrl);
		prodServer.setDescription("Server URL in Production environment");

		Contact contact = new Contact();
		contact.setEmail("tharun.it05@gmail.com");
		contact.setName("Tharunkumar Bairoju");
		contact.setUrl("https://www.carservices.com");

		License mitLicense = new License().name("License").url("https://carservices.com/licenses/");

		Info info = new Info().title("Platform Authentication Management API").version("1.0").contact(contact)
				.description("This API exposes endpoints to manage authentication of platform.")
				.termsOfService("https://www.carservices.com/terms").license(mitLicense);

				// Apply security globally to all operations

		Components components = new Components ( )
				.addSecuritySchemes ( securitySchemeName,
						new SecurityScheme ( )
								.type ( SecurityScheme.Type.HTTP )
								.scheme ( "bearer" )
								.bearerFormat ( "JWT" )
								.description ( "Enter JWT token with Bearer prefix, e.g. 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'" )
				);
		SecurityRequirement securityItem = new SecurityRequirement ( ).addList ( securitySchemeName );
		return new OpenAPI().info(info).servers(List.of(devServer, prodServer)).components( components )
				.addSecurityItem( securityItem );
	}
}

package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI libraryOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Library Service")
						.version("1.0")
						.description("""
								Catalog, members, and lending.
								Sign in with HTTP Basic. Librarian: librarian / password. Member: member / password.
								Health is at GET /actuator/health and does not require a login.
								"""))
				.components(new Components().addSecuritySchemes("basicAuth",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
	}

}

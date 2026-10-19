package com.library.config;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	private static final String LIBRARIAN = "LIBRARIAN";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/books", "/api/v1/books/**").authenticated()
						.requestMatchers("/api/v1/books", "/api/v1/books/**").hasRole(LIBRARIAN)
						.requestMatchers(HttpMethod.GET, "/api/v1/members/*").authenticated()
						.requestMatchers("/api/v1/members", "/api/v1/members/**").hasRole(LIBRARIAN)
						.requestMatchers(HttpMethod.GET, "/api/v1/borrow-history").hasRole(LIBRARIAN)
						.requestMatchers(HttpMethod.GET, "/api/v1/borrowing-rules").authenticated()
						.requestMatchers("/api/v1/loans", "/api/v1/loans/**").authenticated()
						.requestMatchers("/actuator/metrics", "/actuator/metrics/**").authenticated()
						.anyRequest().authenticated())
				.httpBasic(Customizer.withDefaults())
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) -> write(response, request, 401, "UNAUTHORIZED", "Authentication is required"))
						.accessDeniedHandler((request, response, exception) -> write(response, request, 403, "FORBIDDEN", "You do not have access to this resource")));
		return http.build();
	}

	@Bean
	UserDetailsService users(SecurityUsersProperties properties, PasswordEncoder encoder) {
		Map<String, LibraryUser> accounts = properties.users().stream()
				.map(user -> new LibraryUser(user.username(), encoder.encode(user.password()), user.roles(), user.memberEmail()))
				.collect(Collectors.toMap(user -> user.getUsername().toLowerCase(), Function.identity()));
		return username -> {
			LibraryUser user = accounts.get(username.toLowerCase());
			if (user == null) {
				throw new UsernameNotFoundException(username);
			}
			return user;
		};
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private static void write(HttpServletResponse response, HttpServletRequest request, int status, String code, String message)
			throws java.io.IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("""
				{"timestamp":"%s","path":"%s","code":"%s","message":"%s","fieldErrors":[]}
				""".formatted(Instant.now(), escape(request.getRequestURI()), code, message));
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

}

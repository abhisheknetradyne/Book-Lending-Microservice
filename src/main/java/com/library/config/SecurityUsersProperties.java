package com.library.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(prefix = "library.security")
public record SecurityUsersProperties(@NotEmpty List<@Valid UserAccount> users) {

	public record UserAccount(
			@NotBlank String username,
			@NotBlank String password,
			@NotBlank String roles,
			String memberEmail) {
	}

}

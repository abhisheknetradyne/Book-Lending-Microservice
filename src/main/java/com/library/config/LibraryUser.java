package com.library.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class LibraryUser implements UserDetails {

	private final String username;
	private final String password;
	private final List<GrantedAuthority> authorities;
	private final String memberEmail;

	public LibraryUser(String username, String password, String roles, String memberEmail) {
		this.username = username;
		this.password = password;
		this.memberEmail = memberEmail == null || memberEmail.isBlank() ? null : memberEmail.trim();
		this.authorities = Arrays.stream(roles.split(","))
				.map(String::trim)
				.filter(role -> !role.isEmpty())
				.map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
				.map(SimpleGrantedAuthority::new)
				.map(GrantedAuthority.class::cast)
				.toList();
	}

	public String memberEmail() {
		return memberEmail;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

}

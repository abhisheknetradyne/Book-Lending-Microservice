package com.library.entity;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, unique = true, length = 200)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	protected Member() {
	}

	public Member(Long id, String name, String email) {
		this(id, name, email, "");
	}

	public Member(Long id, String name, String email, String passwordHash) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	public static Member register(String name, String email, String encodedPassword) {
		return new Member(null, name.trim(), email.trim().toLowerCase(Locale.ROOT), encodedPassword);
	}

	public void setName(String name) {
		this.name = name.trim();
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public boolean missingLoginPassword() {
		return passwordHash == null || passwordHash.isBlank();
	}

}

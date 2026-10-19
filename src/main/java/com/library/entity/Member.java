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

	protected Member() {
	}

	public Member(Long id, String name, String email) {
		this.id = id;
		this.name = name;
		this.email = email;
	}

	public static Member register(String name, String email) {
		return new Member(null, name.trim(), email.trim().toLowerCase(Locale.ROOT));
	}

	public void setName(String name) {
		this.name = name.trim();
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

}

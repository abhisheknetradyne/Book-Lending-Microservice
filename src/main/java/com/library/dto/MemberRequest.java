package com.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Email @Size(max = 200) String email,
		@NotBlank @Size(min = 8, max = 72) String password) {
}

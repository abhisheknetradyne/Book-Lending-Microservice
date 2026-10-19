package com.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 200) String author,
		@NotBlank @Size(max = 20) String isbn,
		@NotNull @Min(1) @Max(10000) Integer totalCopies) {
}

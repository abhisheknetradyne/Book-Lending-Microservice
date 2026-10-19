package com.library.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
		Instant timestamp,
		String path,
		String code,
		String message,
		List<FieldError> fieldErrors) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(String path, String code, String message, List<FieldError> fieldErrors) {
		return new ErrorResponse(Instant.now(), path, code, message, fieldErrors);
	}

}

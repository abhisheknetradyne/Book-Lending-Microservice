package com.library.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fields, request, exception);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
		return body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed", List.of(), request,
				exception);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ErrorResponse> parameterValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = exception.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ErrorResponse.FieldError(
								result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
		return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fields, request, exception);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> parameterType(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
		if ("memberId".equals(exception.getName())) {
			return body(HttpStatus.BAD_REQUEST, "INVALID_MEMBER", "invalid member id", List.of(), request, exception);
		}
		List<ErrorResponse.FieldError> fields = List.of(
				new ErrorResponse.FieldError(exception.getName(), "has an invalid type"));
		return body(HttpStatus.BAD_REQUEST, "MALFORMED_PARAMETER", "A path or query parameter has an invalid type", fields,
				request, exception);
	}

	@ExceptionHandler(BadRequestException.class)
	ResponseEntity<ErrorResponse> badRequest(BadRequestException exception, HttpServletRequest request) {
		return body(HttpStatus.BAD_REQUEST, "INVALID_MEMBER", exception.getMessage(), List.of(), request, exception);
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ErrorResponse> notFound(NotFoundException exception, HttpServletRequest request) {
		return body(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), List.of(), request, exception);
	}

	@ExceptionHandler(ForbiddenException.class)
	ResponseEntity<ErrorResponse> forbidden(ForbiddenException exception, HttpServletRequest request) {
		return body(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), List.of(), request, exception);
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ErrorResponse> conflict(ConflictException exception, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), List.of(), request, exception);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ErrorResponse> conflictedUpdate(OptimisticLockingFailureException exception, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "The record was updated by someone else. Retry the request",
				List.of(), request, exception);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException exception, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data", List.of(), request,
				exception);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
		return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", List.of(), request,
				exception);
	}

	private ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message,
			List<ErrorResponse.FieldError> fields, HttpServletRequest request, Throwable cause) {
		logApiError(status, code, message, fields, request, cause);
		return ResponseEntity.status(status).body(ErrorResponse.of(request.getRequestURI(), code, message, fields));
	}

	private void logApiError(HttpStatus status, String code, String message, List<ErrorResponse.FieldError> fields,
			HttpServletRequest request, Throwable cause) {
		String path = request.getRequestURI();
		if (status.is5xxServerError()) {
			log.error("api failed path={} status={} code={} message={}", path, status.value(), code, message, cause);
			return;
		}
		if ("VALIDATION_FAILED".equals(code) && !fields.isEmpty()) {
			String detail = fields.stream()
					.map(field -> field.field() + ": " + field.message())
					.collect(Collectors.joining(", "));
			log.warn("api validation failed path={} status={} fields={}", path, status.value(), detail);
			return;
		}
		log.warn("api failed path={} status={} code={} message={}", path, status.value(), code, message);
	}

}

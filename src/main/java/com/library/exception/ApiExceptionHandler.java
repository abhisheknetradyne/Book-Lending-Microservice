package com.library.exception;

import java.util.List;

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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fields, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
		return body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed", List.of(), request);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ErrorResponse> parameterValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = exception.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ErrorResponse.FieldError(
								result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
		return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fields, request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> parameterType(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = List.of(
				new ErrorResponse.FieldError(exception.getName(), "has an invalid type"));
		return body(HttpStatus.BAD_REQUEST, "MALFORMED_PARAMETER", "A path or query parameter has an invalid type", fields, request);
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ErrorResponse> notFound(NotFoundException exception, HttpServletRequest request) {
		return body(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), List.of(), request);
	}

	@ExceptionHandler(ForbiddenException.class)
	ResponseEntity<ErrorResponse> forbidden(ForbiddenException exception, HttpServletRequest request) {
		return body(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), List.of(), request);
	}

	@ExceptionHandler(BorrowingRejectedException.class)
	ResponseEntity<ErrorResponse> rejected(BorrowingRejectedException exception, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), List.of(), request);
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ErrorResponse> conflict(ConflictException exception, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), List.of(), request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<ErrorResponse> conflictedUpdate(OptimisticLockingFailureException exception, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", "The record was updated by someone else. Retry the request", List.of(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException exception, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data", List.of(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ErrorResponse> illegal(IllegalArgumentException exception, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, "COPIES_BELOW_LOANS", exception.getMessage(), List.of(), request);
	}

	private ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message,
			List<ErrorResponse.FieldError> fields, HttpServletRequest request) {
		return ResponseEntity.status(status).body(ErrorResponse.of(request.getRequestURI(), code, message, fields));
	}

}

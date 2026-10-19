package com.library.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void notFoundReturns404() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books/99");

		ResponseEntity<ErrorResponse> response = handler.notFound(new NotFoundException("Book not found"), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
		assertThat(response.getBody().path()).isEqualTo("/api/v1/books/99");
	}

	@Test
	void conflictUsesBusinessCode() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/loans");

		ResponseEntity<ErrorResponse> response = handler.conflict(
				new ConflictException("ALREADY_BORROWED", "This member already has an open loan for the book"), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().code()).isEqualTo("ALREADY_BORROWED");
	}

	@Test
	void badRequestUsesInvalidMemberCode() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/borrow-history");

		ResponseEntity<ErrorResponse> response = handler.badRequest(new BadRequestException("invalid member id"), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().code()).isEqualTo("INVALID_MEMBER");
		assertThat(response.getBody().message()).isEqualTo("invalid member id");
	}

	@Test
	void unexpectedExceptionReturns500() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");

		ResponseEntity<ErrorResponse> response = handler.unexpected(new IllegalStateException("boom"), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
	}

}

package com.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.LoanRequest;
import com.library.dto.LoanResponse;
import com.library.dto.PageResponse;
import com.library.service.LoanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans", description = "Borrow and return books")
@SecurityRequirement(name = "basicAuth")
public class LoanController {

	private final LoanService loans;

	public LoanController(LoanService loans) {
		this.loans = loans;
	}

	@GetMapping
	@Operation(summary = "List loans", description = "Librarians can filter every loan. Members see only their own.")
	public PageResponse<LoanResponse> list(
			@RequestParam(required = false) @Positive Long memberId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return PageResponse.from(loans.list(memberId, page, size));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one loan")
	public LoanResponse get(@PathVariable @Positive Long id) {
		return loans.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Borrow a book", description = "Applies the active borrowing rules and sets the due date from the configured loan duration.")
	public LoanResponse borrow(@Valid @RequestBody LoanRequest request) {
		return loans.borrow(request);
	}

	@PostMapping("/{id}/return")
	@Operation(summary = "Return a book", description = "Returning a book that is already returned leaves it unchanged.")
	public LoanResponse returnBook(@PathVariable @Positive Long id) {
		return loans.returnBook(id);
	}

}

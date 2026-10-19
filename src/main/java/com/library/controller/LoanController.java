package com.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.LoanRequest;
import com.library.dto.LoanResponse;
import com.library.service.LoanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans", description = "Borrow and return books")
@SecurityRequirement(name = "basicAuth")
public class LoanController {

	private final LoanService loans;

	public LoanController(LoanService loans) {
		this.loans = loans;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(operationId = "borrowBook", summary = "Borrow a book", description = "Applies the active borrowing rules and sets the due date from the configured loan duration.")
	public LoanResponse borrow(@Valid @RequestBody LoanRequest request) {
		return loans.borrow(request);
	}

	@PostMapping("/return")
	@Operation(operationId = "returnBook", summary = "Return a book", description = "Identify the copy by memberId and bookId. Returning a book that is already returned leaves it unchanged.")
	public LoanResponse returnBook(@Valid @RequestBody LoanRequest request) {
		return loans.returnBook(request);
	}

}

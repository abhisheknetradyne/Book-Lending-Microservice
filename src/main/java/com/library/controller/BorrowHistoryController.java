package com.library.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.LoanResponse;
import com.library.dto.PageResponse;
import com.library.service.LoanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/borrow-history")
@Tag(name = "Borrow history", description = "Borrow and return history for one member")
@SecurityRequirement(name = "basicAuth")
public class BorrowHistoryController {

	private final LoanService loans;

	public BorrowHistoryController(LoanService loans) {
		this.loans = loans;
	}

	@GetMapping
	@Operation(operationId = "borrowingHistory", summary = "Borrowing history", description = "Loans for one member, including returned books. memberId is required. A librarian can read any member. A member can read only their own.")
	public PageResponse<LoanResponse> borrowingHistory(
			@RequestParam(required = false) Long memberId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return PageResponse.from(loans.borrowingHistory(memberId, page, size));
	}

}

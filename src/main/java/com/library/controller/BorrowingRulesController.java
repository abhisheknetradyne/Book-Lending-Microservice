package com.library.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.config.BorrowingProperties;
import com.library.dto.BorrowingRulesResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/borrowing-rules")
@Tag(name = "Borrowing rules", description = "Limits read from application configuration, not from the database")
@SecurityRequirement(name = "basicAuth")
public class BorrowingRulesController {

	private final BorrowingProperties rules;

	public BorrowingRulesController(BorrowingProperties rules) {
		this.rules = rules;
	}

	@GetMapping
	@Operation(summary = "Read the configured borrowing rules")
	public BorrowingRulesResponse current() {
		return new BorrowingRulesResponse(rules.maxActiveLoans(), rules.blockWhenOverdue(), rules.loanDurationDays());
	}

}

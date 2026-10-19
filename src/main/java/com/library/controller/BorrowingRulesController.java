package com.library.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.BorrowingRulesResponse;
import com.library.service.BorrowingRulesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/borrowing-rules")
@Tag(name = "Borrowing rules", description = "Limits read from application.properties, not from the database")
@SecurityRequirement(name = "basicAuth")
public class BorrowingRulesController {

	private final BorrowingRulesService rules;

	public BorrowingRulesController(BorrowingRulesService rules) {
		this.rules = rules;
	}

	@GetMapping
	@Operation(operationId = "getBorrowingRules", summary = "Read the configured borrowing rules")
	public BorrowingRulesResponse current() {
		return rules.current();
	}

}

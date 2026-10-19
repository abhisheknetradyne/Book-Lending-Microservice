package com.library.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.BorrowingRulesRequest;
import com.library.dto.BorrowingRulesResponse;
import com.library.service.BorrowingRulesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/borrowing-rules")
@Tag(name = "Borrowing rules", description = "Limits held in memory for this process. Defaults come from application.properties on startup.")
@SecurityRequirement(name = "basicAuth")
public class BorrowingRulesController {

	private final BorrowingRulesService rules;

	public BorrowingRulesController(BorrowingRulesService rules) {
		this.rules = rules;
	}

	@GetMapping
	@Operation(operationId = "getBorrowingRules", summary = "Read the active borrowing rules")
	public BorrowingRulesResponse current() {
		return rules.current();
	}

	@PutMapping
	@Operation(operationId = "updateBorrowingRules", summary = "Update borrowing rules", description = "Librarian only. Changes apply to the running process and are lost on restart unless application.properties is updated too.")
	public BorrowingRulesResponse update(@Valid @RequestBody BorrowingRulesRequest request) {
		return rules.update(request);
	}

}

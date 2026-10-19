package com.library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BorrowingRulesRequest(
		@NotNull @Min(1) Integer maxActiveLoans,
		@NotNull Boolean blockWhenOverdue,
		@NotNull @Min(1) Integer loanDurationDays) {

	public com.library.config.BorrowingProperties toProperties() {
		return new com.library.config.BorrowingProperties(maxActiveLoans, blockWhenOverdue, loanDurationDays);
	}

}

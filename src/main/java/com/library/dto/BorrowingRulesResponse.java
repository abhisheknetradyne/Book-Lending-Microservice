package com.library.dto;

public record BorrowingRulesResponse(
		int maxActiveLoans,
		boolean blockWhenOverdue,
		int loanDurationDays) {
}

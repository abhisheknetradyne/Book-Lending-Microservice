package com.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "library.borrowing")
public record BorrowingProperties(
		@Min(1) int maxActiveLoans,
		boolean blockWhenOverdue,
		@Min(1) int loanDurationDays) {
}

package com.library.domain.rules;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

@Component
public class LoanDurationPolicy {

	public Instant dueDate(Instant borrowedAt, BorrowingPolicy rules) {
		return borrowedAt.plus(rules.loanDurationDays(), ChronoUnit.DAYS);
	}

}

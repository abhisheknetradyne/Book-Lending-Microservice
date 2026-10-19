package com.library.rules;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.library.config.BorrowingProperties;

@Component
public class BorrowingRules {

	public RuleResult evaluate(BorrowingContext context) {
		RuleResult result = checkBookAvailable(context);
		if (!result.allowed()) {
			return result;
		}
		result = checkNoDuplicateLoan(context);
		if (!result.allowed()) {
			return result;
		}
		result = checkActiveLoanLimit(context);
		if (!result.allowed()) {
			return result;
		}
		return checkOverdue(context);
	}

	public Instant dueDate(Instant borrowedAt, BorrowingProperties rules) {
		return borrowedAt.plus(rules.loanDurationDays(), ChronoUnit.DAYS);
	}

	private RuleResult checkOverdue(BorrowingContext context) {
		if (!context.rules().blockWhenOverdue()) {
			return RuleResult.allow();
		}
		boolean overdue = context.openLoans().stream()
				.anyMatch(loan -> loan.getDueDate().isBefore(context.requestedAt()));
		if (overdue) {
			return RuleResult.reject("MEMBER_HAS_OVERDUE", "Member has an overdue loan");
		}
		return RuleResult.allow();
	}

	private RuleResult checkActiveLoanLimit(BorrowingContext context) {
		int limit = context.rules().maxActiveLoans();
		if (context.openLoans().size() >= limit) {
			return RuleResult.reject("LOAN_LIMIT_REACHED", "Member already has " + limit + " active loans");
		}
		return RuleResult.allow();
	}

	private RuleResult checkBookAvailable(BorrowingContext context) {
		if (context.book().getAvailableCopies() < 1) {
			return RuleResult.reject("NO_COPIES_AVAILABLE", "No copies of this book are available");
		}
		return RuleResult.allow();
	}

	private RuleResult checkNoDuplicateLoan(BorrowingContext context) {
		boolean alreadyBorrowed = context.openLoans().stream()
				.anyMatch(loan -> loan.getBookId().equals(context.book().getId()));
		if (alreadyBorrowed) {
			return RuleResult.reject("ALREADY_BORROWED", "This member already has an open loan for the book");
		}
		return RuleResult.allow();
	}

}

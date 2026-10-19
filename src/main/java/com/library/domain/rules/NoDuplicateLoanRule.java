package com.library.domain.rules;

import org.springframework.stereotype.Component;

@Component
public class NoDuplicateLoanRule implements BorrowingRule {

	@Override
	public String code() {
		return "ALREADY_BORROWED";
	}

	@Override
	public int order() {
		return 30;
	}

	@Override
	public RuleResult evaluate(BorrowingContext context) {
		boolean alreadyBorrowed = context.openLoans().stream()
				.anyMatch(loan -> loan.getBook().getId().equals(context.book().getId()));
		if (alreadyBorrowed) {
			return RuleResult.reject(code(), "This member already has an open loan for the book");
		}
		return RuleResult.allow();
	}

}

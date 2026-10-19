package com.library.domain.rules;

import org.springframework.stereotype.Component;

@Component
public class NoOverdueLoansRule implements BorrowingRule {

	@Override
	public String code() {
		return "MEMBER_HAS_OVERDUE";
	}

	@Override
	public int order() {
		return 50;
	}

	@Override
	public RuleResult evaluate(BorrowingContext context) {
		if (!context.rules().blockWhenOverdue()) {
			return RuleResult.allow();
		}
		boolean overdue = context.openLoans().stream()
				.anyMatch(loan -> loan.getDueDate().isBefore(context.requestedAt()));
		if (overdue) {
			return RuleResult.reject(code(), "Member has an overdue loan");
		}
		return RuleResult.allow();
	}

}

package com.library.domain.rules;

import org.springframework.stereotype.Component;

@Component
public class MaxActiveLoansRule implements BorrowingRule {

	@Override
	public String code() {
		return "LOAN_LIMIT_REACHED";
	}

	@Override
	public int order() {
		return 40;
	}

	@Override
	public RuleResult evaluate(BorrowingContext context) {
		int limit = context.rules().maxActiveLoans();
		if (context.openLoans().size() >= limit) {
			return RuleResult.reject(code(), "Member already has " + limit + " active loans");
		}
		return RuleResult.allow();
	}

}

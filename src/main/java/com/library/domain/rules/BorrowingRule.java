package com.library.domain.rules;

public interface BorrowingRule {

	String code();

	int order();

	RuleResult evaluate(BorrowingContext context);

}

package com.library.domain.rules;

import org.springframework.stereotype.Component;

@Component
public class BookAvailableRule implements BorrowingRule {

	@Override
	public String code() {
		return "NO_COPIES_AVAILABLE";
	}

	@Override
	public int order() {
		return 20;
	}

	@Override
	public RuleResult evaluate(BorrowingContext context) {
		if (context.book().getAvailableCopies() < 1) {
			return RuleResult.reject(code(), "No copies of this book are available");
		}
		return RuleResult.allow();
	}

}

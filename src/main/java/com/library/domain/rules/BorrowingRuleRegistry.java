package com.library.domain.rules;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class BorrowingRuleRegistry {

	private final List<BorrowingRule> rules;

	public BorrowingRuleRegistry(List<BorrowingRule> rules) {
		this.rules = rules.stream()
				.sorted(Comparator.comparingInt(BorrowingRule::order))
				.toList();
	}

	public RuleResult evaluate(BorrowingContext context) {
		for (BorrowingRule rule : rules) {
			RuleResult result = rule.evaluate(context);
			if (!result.allowed()) {
				return result;
			}
		}
		return RuleResult.allow();
	}

	public List<BorrowingRule> rules() {
		return rules;
	}

}

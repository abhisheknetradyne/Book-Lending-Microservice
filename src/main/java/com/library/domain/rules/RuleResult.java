package com.library.domain.rules;

public record RuleResult(boolean allowed, String code, String message) {

	public static RuleResult allow() {
		return new RuleResult(true, null, null);
	}

	public static RuleResult reject(String code, String message) {
		return new RuleResult(false, code, message);
	}

}

package com.library.config;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class InMemoryBorrowingRules {

	private final AtomicReference<BorrowingProperties> rules;

	public InMemoryBorrowingRules(BorrowingProperties defaults) {
		this.rules = new AtomicReference<>(defaults);
	}

	public BorrowingProperties current() {
		return rules.get();
	}

	public void replace(BorrowingProperties next) {
		rules.set(next);
	}

}

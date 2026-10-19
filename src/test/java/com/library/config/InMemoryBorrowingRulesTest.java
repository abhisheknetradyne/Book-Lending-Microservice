package com.library.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryBorrowingRulesTest {

	@Test
	void startsWithDefaultsAndCanReplace() {
		InMemoryBorrowingRules rules = new InMemoryBorrowingRules(new BorrowingProperties(5, true, 14));

		assertThat(rules.current().maxActiveLoans()).isEqualTo(5);
		assertThat(rules.current().loanDurationDays()).isEqualTo(14);

		rules.replace(new BorrowingProperties(2, false, 7));

		assertThat(rules.current().maxActiveLoans()).isEqualTo(2);
		assertThat(rules.current().blockWhenOverdue()).isFalse();
		assertThat(rules.current().loanDurationDays()).isEqualTo(7);
	}

}

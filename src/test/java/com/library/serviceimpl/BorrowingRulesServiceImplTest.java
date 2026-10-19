package com.library.serviceimpl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.library.config.BorrowingProperties;

class BorrowingRulesServiceImplTest {

	@Test
	void currentReflectsConfiguredProperties() {
		BorrowingRulesServiceImpl rules = new BorrowingRulesServiceImpl(new BorrowingProperties(5, true, 14));

		assertThat(rules.current().maxActiveLoans()).isEqualTo(5);
		assertThat(rules.current().blockWhenOverdue()).isTrue();
		assertThat(rules.current().loanDurationDays()).isEqualTo(14);
	}

}

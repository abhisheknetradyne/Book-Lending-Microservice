package com.library.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleResultTest {

	@Test
	void allowHasNoCodeOrMessage() {
		RuleResult result = RuleResult.allow();

		assertThat(result.allowed()).isTrue();
		assertThat(result.code()).isNull();
		assertThat(result.message()).isNull();
	}

	@Test
	void rejectCarriesCodeAndMessage() {
		RuleResult result = RuleResult.reject("NO_COPIES_AVAILABLE", "No copies left");

		assertThat(result.allowed()).isFalse();
		assertThat(result.code()).isEqualTo("NO_COPIES_AVAILABLE");
		assertThat(result.message()).isEqualTo("No copies left");
	}

}

package com.library.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.library.config.BorrowingProperties;
import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.Member;

class BorrowingRuleTest {

	private final Instant now = Instant.parse("2026-10-18T00:00:00Z");
	private final Member member = new Member(1L, "Ada", "ada@library.local");
	private final Book book = new Book(10L, "Clean Code", "Robert Martin", "111", 2, 2);
	private final BorrowingProperties rules = new BorrowingProperties(2, true, 14);
	private final BorrowingRules borrowingRules = new BorrowingRules();

	@Test
	void allowsBorrowInsideTheConfiguredLimits() {
		RuleResult result = borrowingRules.evaluate(context(member, book, List.of(), rules));

		assertThat(result.allowed()).isTrue();
	}

	@Test
	void rejectsWhenNoCopiesRemain() {
		Book noneLeft = new Book(11L, "Empty", "Author", "222", 1, 0);

		RuleResult result = borrowingRules.evaluate(context(member, noneLeft, List.of(), rules));

		assertThat(result.code()).isEqualTo("NO_COPIES_AVAILABLE");
	}

	@Test
	void rejectsASecondOpenLoanForTheSameBook() {
		RuleResult result = borrowingRules.evaluate(context(member, book, List.of(openLoan(book, now.plus(2, ChronoUnit.DAYS))), rules));

		assertThat(result.code()).isEqualTo("ALREADY_BORROWED");
	}

	@Test
	void rejectsWhenActiveLoansReachTheConfiguredMaximum() {
		List<Loan> open = List.of(openLoan(otherBook(), now.plus(2, ChronoUnit.DAYS)), openLoan(anotherBook(), now.plus(2, ChronoUnit.DAYS)));

		RuleResult result = borrowingRules.evaluate(context(member, book, open, rules));

		assertThat(result.code()).isEqualTo("LOAN_LIMIT_REACHED");
	}

	@Test
	void rejectsWhenAnOpenLoanIsPastItsDueDate() {
		Loan overdue = openLoan(otherBook(), now.minus(1, ChronoUnit.DAYS));

		RuleResult result = borrowingRules.evaluate(context(member, book, List.of(overdue), rules));

		assertThat(result.code()).isEqualTo("MEMBER_HAS_OVERDUE");
	}

	@Test
	void allowsOverdueMemberWhenTheFlagIsOff() {
		BorrowingProperties openRules = new BorrowingProperties(5, false, 14);
		Loan overdue = openLoan(otherBook(), now.minus(1, ChronoUnit.DAYS));

		RuleResult result = borrowingRules.evaluate(context(member, book, List.of(overdue), openRules));

		assertThat(result.allowed()).isTrue();
	}

	@Test
	void dueDateUsesTheConfiguredDuration() {
		Instant dueDate = borrowingRules.dueDate(now, rules);

		assertThat(dueDate).isEqualTo(now.plus(14, ChronoUnit.DAYS));
	}

	private BorrowingContext context(Member person, Book requested, List<Loan> openLoans, BorrowingProperties activeRules) {
		return new BorrowingContext(person, requested, openLoans, now, activeRules);
	}

	private Loan openLoan(Book borrowed, Instant dueDate) {
		return new Loan(null, member, borrowed, now.minus(10, ChronoUnit.DAYS), dueDate, null);
	}

	private Book otherBook() {
		return new Book(12L, "Other", "Author", "333", 1, 0);
	}

	private Book anotherBook() {
		return new Book(13L, "Another", "Author", "444", 1, 0);
	}

}

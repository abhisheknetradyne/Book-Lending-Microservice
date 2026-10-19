package com.library.domain.rules;

import java.time.Instant;
import java.util.List;

import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.Member;

public record BorrowingContext(
		Member member,
		Book book,
		List<Loan> openLoans,
		Instant requestedAt,
		BorrowingPolicy rules) {
}

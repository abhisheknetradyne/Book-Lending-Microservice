package com.library.dto;

import java.time.Instant;

import com.library.entity.Loan;

public record LoanResponse(
		Long id,
		Long bookId,
		Long memberId,
		Instant borrowedAt,
		Instant dueDate,
		Instant returnedAt) {

	public LoanResponse(Loan loan) {
		this(loan.getId(), loan.getBookId(), loan.getMemberId(), loan.getBorrowedAt(), loan.getDueDate(), loan.getReturnedAt());
	}

}

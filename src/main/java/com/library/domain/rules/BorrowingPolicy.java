package com.library.domain.rules;

public record BorrowingPolicy(int maxActiveLoans, boolean blockWhenOverdue, int loanDurationDays) {
}

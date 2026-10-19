package com.library.service;

import org.springframework.data.domain.Page;

import com.library.dto.LoanRequest;
import com.library.dto.LoanResponse;

public interface LoanService {

	LoanResponse borrow(LoanRequest request);

	LoanResponse returnBook(LoanRequest request);

	Page<LoanResponse> borrowingHistory(Long memberId, int page, int size);

}

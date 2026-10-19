package com.library.service;

import com.library.dto.BorrowingRulesRequest;
import com.library.dto.BorrowingRulesResponse;

public interface BorrowingRulesService {

	BorrowingRulesResponse current();

	BorrowingRulesResponse update(BorrowingRulesRequest request);

}

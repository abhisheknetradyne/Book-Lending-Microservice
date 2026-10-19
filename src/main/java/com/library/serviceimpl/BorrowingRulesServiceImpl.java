package com.library.serviceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.library.config.InMemoryBorrowingRules;
import com.library.dto.BorrowingRulesRequest;
import com.library.dto.BorrowingRulesResponse;
import com.library.service.BorrowingRulesService;

@Service
public class BorrowingRulesServiceImpl implements BorrowingRulesService {

	private static final Logger log = LoggerFactory.getLogger(BorrowingRulesServiceImpl.class);

	private final InMemoryBorrowingRules rules;

	public BorrowingRulesServiceImpl(InMemoryBorrowingRules rules) {
		this.rules = rules;
	}

	@Override
	public BorrowingRulesResponse current() {
		return toResponse(rules.current());
	}

	@Override
	public BorrowingRulesResponse update(BorrowingRulesRequest request) {
		log.info("update borrowing rules maxActiveLoans={} blockWhenOverdue={} loanDurationDays={}",
				request.maxActiveLoans(), request.blockWhenOverdue(), request.loanDurationDays());
		rules.replace(request.toProperties());
		log.info("update borrowing rules succeeded");
		return toResponse(rules.current());
	}

	private static BorrowingRulesResponse toResponse(com.library.config.BorrowingProperties properties) {
		return new BorrowingRulesResponse(properties.maxActiveLoans(), properties.blockWhenOverdue(),
				properties.loanDurationDays());
	}

}

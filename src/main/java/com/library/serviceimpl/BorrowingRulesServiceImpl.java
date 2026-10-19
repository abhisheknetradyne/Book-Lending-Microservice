package com.library.serviceimpl;

import org.springframework.stereotype.Service;

import com.library.config.BorrowingProperties;
import com.library.dto.BorrowingRulesResponse;
import com.library.service.BorrowingRulesService;

@Service
public class BorrowingRulesServiceImpl implements BorrowingRulesService {

	private final BorrowingProperties properties;

	public BorrowingRulesServiceImpl(BorrowingProperties properties) {
		this.properties = properties;
	}

	@Override
	public BorrowingRulesResponse current() {
		return new BorrowingRulesResponse(properties.maxActiveLoans(), properties.blockWhenOverdue(),
				properties.loanDurationDays());
	}

}

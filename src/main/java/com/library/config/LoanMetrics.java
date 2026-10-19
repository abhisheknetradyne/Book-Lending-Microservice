package com.library.config;

import org.springframework.stereotype.Component;

import com.library.repository.SqlSession;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class LoanMetrics {

	public LoanMetrics(MeterRegistry registry, SqlSession sqlSession) {
		Gauge.builder("library.loans.active", sqlSession, SqlSession::countOpenLoans)
				.description("Open loans, including those past their due date")
				.register(registry);
	}

}

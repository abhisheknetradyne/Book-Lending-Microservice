package com.library.service;

public final class LibraryMetrics {

	public static final String METRIC_BORROWED = "library.loans.borrowed";
	public static final String METRIC_RETURNED = "library.loans.returned";
	public static final String METRIC_REJECTED = "library.loans.rejected";
	public static final String METRIC_BORROW_DURATION = "library.loans.borrow";
	public static final String METRIC_API_RESPONSE = "library.api.responses";
	public static final String METRIC_API_LATENCY = "library.api.request";

	private LibraryMetrics() {
	}

}

package com.library.dto;

import java.util.List;

public record LibraryMetricsResponse(
		long loansBorrowed,
		long loansReturned,
		long loansActive,
		List<RejectedMetric> loansRejected,
		BorrowDurationMetric borrowDuration,
		List<HttpStatusMetric> httpResponses,
		RequestDurationMetric apiRequestDuration) {

	public record RejectedMetric(String code, long count) {
	}

	public record BorrowDurationMetric(long sampleCount, double averageMs) {
	}

	public record HttpStatusMetric(int status, long count) {
	}

	public record RequestDurationMetric(long sampleCount, double averageMs) {
	}

}

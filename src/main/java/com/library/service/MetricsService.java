package com.library.service;

import com.library.dto.LibraryMetricsResponse;

public interface MetricsService {

	void recordBorrowed(long durationMs);

	void recordReturned();

	void recordRejected(String code);

	void recordApiRequest(int httpStatus, long durationMs);

	LibraryMetricsResponse snapshot();

}

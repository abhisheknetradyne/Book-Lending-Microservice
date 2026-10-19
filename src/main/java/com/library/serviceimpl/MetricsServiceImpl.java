package com.library.serviceimpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.library.dto.LibraryMetricsResponse;
import com.library.dto.LibraryMetricsResponse.BorrowDurationMetric;
import com.library.dto.LibraryMetricsResponse.HttpStatusMetric;
import com.library.dto.LibraryMetricsResponse.RejectedMetric;
import com.library.dto.LibraryMetricsResponse.RequestDurationMetric;
import com.library.dao.LoanDao;
import com.library.dao.MetricDao;
import com.library.service.LibraryMetrics;
import com.library.service.MetricsService;

@Service
public class MetricsServiceImpl implements MetricsService {

	private final MetricDao metrics;
	private final LoanDao loans;

	public MetricsServiceImpl(MetricDao metrics, LoanDao loans) {
		this.metrics = metrics;
		this.loans = loans;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordBorrowed(long durationMs) {
		metrics.incrementCounter(LibraryMetrics.METRIC_BORROWED, "");
		metrics.recordDuration(LibraryMetrics.METRIC_BORROW_DURATION, durationMs);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordReturned() {
		metrics.incrementCounter(LibraryMetrics.METRIC_RETURNED, "");
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordRejected(String code) {
		metrics.incrementCounter(LibraryMetrics.METRIC_REJECTED, code);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordApiRequest(int httpStatus, long durationMs) {
		metrics.incrementCounter(LibraryMetrics.METRIC_API_RESPONSE, Integer.toString(httpStatus));
		metrics.recordDuration(LibraryMetrics.METRIC_API_LATENCY, durationMs);
	}

	@Override
	@Transactional(readOnly = true)
	public LibraryMetricsResponse snapshot() {
		var borrowTimer = metrics.timerSummary(LibraryMetrics.METRIC_BORROW_DURATION);
		var apiTimer = metrics.timerSummary(LibraryMetrics.METRIC_API_LATENCY);
		return new LibraryMetricsResponse(
				metrics.counterTotal(LibraryMetrics.METRIC_BORROWED),
				metrics.counterTotal(LibraryMetrics.METRIC_RETURNED),
				loans.countOpenLoans(),
				metrics.rejectedCounts().stream().map(row -> new RejectedMetric(row.code(), row.count())).toList(),
				toBorrowDuration(borrowTimer),
				metrics.counterByCode(LibraryMetrics.METRIC_API_RESPONSE).stream()
						.map(row -> new HttpStatusMetric(Integer.parseInt(row.code()), row.count()))
						.toList(),
				toRequestDuration(apiTimer));
	}

	private static BorrowDurationMetric toBorrowDuration(com.library.dao.MetricDao.TimerSummary timer) {
		return new BorrowDurationMetric(timer.sampleCount(), averageMs(timer));
	}

	private static RequestDurationMetric toRequestDuration(com.library.dao.MetricDao.TimerSummary timer) {
		return new RequestDurationMetric(timer.sampleCount(), averageMs(timer));
	}

	private static double averageMs(com.library.dao.MetricDao.TimerSummary timer) {
		return timer.sampleCount() == 0 ? 0 : (double) timer.totalMs() / timer.sampleCount();
	}

}

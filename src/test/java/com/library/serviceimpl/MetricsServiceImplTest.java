package com.library.serviceimpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.library.dao.LoanDao;
import com.library.dao.MetricDao;
import com.library.dao.MetricDao.CodeCount;
import com.library.dao.MetricDao.RejectedCount;
import com.library.dao.MetricDao.TimerSummary;
import com.library.service.LibraryMetrics;

@ExtendWith(MockitoExtension.class)
class MetricsServiceImplTest {

	@Mock
	private MetricDao metrics;

	@Mock
	private LoanDao loans;

	@InjectMocks
	private MetricsServiceImpl service;

	@Test
	void recordApiRequestIncrementsStatusAndLatency() {
		service.recordApiRequest(404, 32);

		verify(metrics).incrementCounter(LibraryMetrics.METRIC_API_RESPONSE, "404");
		verify(metrics).recordDuration(LibraryMetrics.METRIC_API_LATENCY, 32);
	}

	@Test
	void recordBorrowedIncrementsLoanCounters() {
		service.recordBorrowed(120);

		verify(metrics).incrementCounter(LibraryMetrics.METRIC_BORROWED, "");
		verify(metrics).recordDuration(LibraryMetrics.METRIC_BORROW_DURATION, 120);
	}

	@Test
	void snapshotAggregatesLoanAndApiMetrics() {
		when(metrics.counterTotal(LibraryMetrics.METRIC_BORROWED)).thenReturn(3L);
		when(metrics.counterTotal(LibraryMetrics.METRIC_RETURNED)).thenReturn(2L);
		when(loans.countOpenLoans()).thenReturn(1L);
		when(metrics.rejectedCounts()).thenReturn(List.of(new RejectedCount("ALREADY_BORROWED", 4L)));
		when(metrics.timerSummary(LibraryMetrics.METRIC_BORROW_DURATION)).thenReturn(new TimerSummary(200, 2));
		when(metrics.timerSummary(LibraryMetrics.METRIC_API_LATENCY)).thenReturn(new TimerSummary(900, 3));
		when(metrics.counterByCode(LibraryMetrics.METRIC_API_RESPONSE))
				.thenReturn(List.of(new CodeCount("200", 10L), new CodeCount("409", 2L)));

		var snapshot = service.snapshot();

		assertThat(snapshot.loansBorrowed()).isEqualTo(3);
		assertThat(snapshot.loansReturned()).isEqualTo(2);
		assertThat(snapshot.loansActive()).isEqualTo(1);
		assertThat(snapshot.loansRejected()).hasSize(1);
		assertThat(snapshot.loansRejected().get(0).code()).isEqualTo("ALREADY_BORROWED");
		assertThat(snapshot.borrowDuration().sampleCount()).isEqualTo(2);
		assertThat(snapshot.borrowDuration().averageMs()).isEqualTo(100);
		assertThat(snapshot.httpResponses()).extracting("status").containsExactly(200, 409);
		assertThat(snapshot.httpResponses()).extracting("count").containsExactly(10L, 2L);
		assertThat(snapshot.apiRequestDuration().sampleCount()).isEqualTo(3);
		assertThat(snapshot.apiRequestDuration().averageMs()).isEqualTo(300);
	}

}

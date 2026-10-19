package com.library.dao;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.library.service.LibraryMetrics;
import com.library.session.SqlSession;

@Repository
public class MetricDao {

	private final SqlSession sqlSession;

	public MetricDao(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

	@Transactional
	public void incrementCounter(String metricName, String code) {
		String safeCode = code == null ? "" : code;
		sqlSession.session().createNativeQuery("""
				INSERT INTO library_metric_counters (metric_name, code, count)
				VALUES (:name, :code, 1)
				ON CONFLICT (metric_name, code) DO UPDATE
				SET count = library_metric_counters.count + 1
				""")
				.setParameter("name", metricName)
				.setParameter("code", safeCode)
				.executeUpdate();
	}

	@Transactional
	public void recordDuration(String metricName, long durationMs) {
		sqlSession.session().createNativeQuery("""
				INSERT INTO library_metric_timers (metric_name, total_ms, sample_count)
				VALUES (:name, :ms, 1)
				ON CONFLICT (metric_name) DO UPDATE
				SET total_ms = library_metric_timers.total_ms + EXCLUDED.total_ms,
				    sample_count = library_metric_timers.sample_count + 1
				""")
				.setParameter("name", metricName)
				.setParameter("ms", durationMs)
				.executeUpdate();
	}

	@Transactional(readOnly = true)
	public long counterTotal(String metricName) {
		Number total = (Number) sqlSession.session().createNativeQuery("""
				SELECT COALESCE(SUM(count), 0) FROM library_metric_counters WHERE metric_name = :name
				""")
				.setParameter("name", metricName)
				.getSingleResult();
		return total.longValue();
	}

	@Transactional(readOnly = true)
	public long counterTotal(String metricName, String code) {
		Number total = (Number) sqlSession.session().createNativeQuery("""
				SELECT COALESCE(MAX(count), 0) FROM library_metric_counters
				WHERE metric_name = :name AND code = :code
				""")
				.setParameter("name", metricName)
				.setParameter("code", code == null ? "" : code)
				.getSingleResult();
		return total.longValue();
	}

	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public List<RejectedCount> rejectedCounts() {
		List<Object[]> rows = sqlSession.session().createNativeQuery("""
				SELECT code, count FROM library_metric_counters
				WHERE metric_name = :name AND code <> ''
				ORDER BY code
				""")
				.setParameter("name", LibraryMetrics.METRIC_REJECTED)
				.getResultList();
		return rows.stream()
				.map(row -> new RejectedCount((String) row[0], ((Number) row[1]).longValue()))
				.toList();
	}

	@Transactional(readOnly = true)
	public TimerSummary timerSummary(String metricName) {
		@SuppressWarnings("unchecked")
		List<Object[]> rows = sqlSession.session().createNativeQuery("""
				SELECT total_ms, sample_count FROM library_metric_timers WHERE metric_name = :name
				""")
				.setParameter("name", metricName)
				.getResultList();
		if (rows.isEmpty()) {
			return new TimerSummary(0, 0);
		}
		Object[] row = rows.get(0);
		return new TimerSummary(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
	}

	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public List<CodeCount> counterByCode(String metricName) {
		List<Object[]> rows = sqlSession.session().createNativeQuery("""
				SELECT code, count FROM library_metric_counters
				WHERE metric_name = :name AND code <> ''
				ORDER BY code
				""")
				.setParameter("name", metricName)
				.getResultList();
		return rows.stream()
				.map(row -> new CodeCount((String) row[0], ((Number) row[1]).longValue()))
				.toList();
	}

	public record RejectedCount(String code, long count) {
	}

	public record CodeCount(String code, long count) {
	}

	public record TimerSummary(long totalMs, long sampleCount) {
	}

}

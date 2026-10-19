package com.library.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.library.service.MetricsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	private static final String HEADER = "X-Request-Id";

	private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

	private final MetricsService metrics;

	public RequestIdFilter(MetricsService metrics) {
		this.metrics = metrics;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = request.getHeader(HEADER);
		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString();
		}
		MDC.put("requestId", requestId);
		response.setHeader(HEADER, requestId);
		boolean apiCall = request.getRequestURI().startsWith("/api/");
		long started = apiCall ? System.nanoTime() : 0;
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			if (apiCall) {
				int status = response.getStatus();
				long durationMs = (System.nanoTime() - started) / 1_000_000;
				log.info("method={} path={} status={}", request.getMethod(), request.getRequestURI(), status);
				try {
					metrics.recordApiRequest(status, durationMs);
				}
				catch (RuntimeException exception) {
					log.error("Failed to record API metrics path={} status={}", request.getRequestURI(), status, exception);
				}
			}
			MDC.remove("requestId");
		}
	}

}

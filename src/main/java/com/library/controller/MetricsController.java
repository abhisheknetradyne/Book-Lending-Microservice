package com.library.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.LibraryMetricsResponse;
import com.library.service.MetricsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics", description = "Loan counters and timings stored in PostgreSQL")
@SecurityRequirement(name = "basicAuth")
public class MetricsController {

	private final MetricsService metrics;

	public MetricsController(MetricsService metrics) {
		this.metrics = metrics;
	}

	@GetMapping
	@Operation(operationId = "getLibraryMetrics", summary = "Read loan and API metrics from the database")
	public LibraryMetricsResponse snapshot() {
		return metrics.snapshot();
	}

}

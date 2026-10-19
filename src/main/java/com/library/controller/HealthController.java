package com.library.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.library.dto.HealthStatus;
import com.library.service.HealthService;

@RestController
public class HealthController {

	private final HealthService healthService;

	public HealthController(HealthService healthService) {
		this.healthService = healthService;
	}

	@GetMapping("/actuator/health/liveness")
	public HealthStatus liveness() {
		return healthService.liveness();
	}

	@GetMapping("/actuator/health/readiness")
	public ResponseEntity<HealthStatus> readiness() {
		HealthStatus status = healthService.readiness();
		HttpStatus httpStatus = status.healthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return ResponseEntity.status(httpStatus).body(status);
	}

}

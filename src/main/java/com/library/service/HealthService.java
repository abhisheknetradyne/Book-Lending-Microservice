package com.library.service;

import com.library.dto.HealthStatus;

public interface HealthService {

	HealthStatus liveness();

	HealthStatus readiness();

}

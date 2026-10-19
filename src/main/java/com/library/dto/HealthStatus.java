package com.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthStatus(boolean healthy, Boolean databaseCheck, Boolean diskCheck) {
}

package com.library.serviceimpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import com.library.dto.HealthStatus;
import com.library.dao.HealthDao;
import com.library.service.HealthService;

@Service
public class HealthServiceImpl implements HealthService {

	private static final Logger log = LoggerFactory.getLogger(HealthServiceImpl.class);

	private final HealthDao health;
	private final long minimumFreeDisk;

	public HealthServiceImpl(HealthDao health, @Value("${library.health.minimum-free-disk}") String minimumFreeDisk) {
		this.health = health;
		this.minimumFreeDisk = DataSize.parse(minimumFreeDisk).toBytes();
	}

	@Override
	public HealthStatus liveness() {
		return new HealthStatus(true, null, null);
	}

	@Override
	public HealthStatus readiness() {
		boolean databaseCheck = databaseUp();
		boolean diskCheck = diskUp();
		log.info("databaseCheck={} diskCheck={}", databaseCheck, diskCheck);
		return new HealthStatus(databaseCheck && diskCheck, databaseCheck, diskCheck);
	}

	private boolean databaseUp() {
		try {
			return health.selectOne() == 1;
		}
		catch (RuntimeException exception) {
			log.error("Database health check failed", exception);
			return false;
		}
	}

	private boolean diskUp() {
		try {
			long freeBytes = Files.getFileStore(Path.of(".")).getUsableSpace();
			return freeBytes >= minimumFreeDisk;
		}
		catch (IOException exception) {
			log.error("Disk health check failed", exception);
			return false;
		}
	}

}

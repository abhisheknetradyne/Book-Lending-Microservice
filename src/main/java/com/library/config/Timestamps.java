package com.library.config;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class Timestamps {

	public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

	private Timestamps() {
	}

	public static String format(Instant instant) {
		return FORMAT.format(instant);
	}

}

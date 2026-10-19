package com.library.config;

import java.time.Instant;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

	@Bean
	JsonMapperBuilderCustomizer utcInstantFormatting() {
		return builder -> builder.addModule(new SimpleModule().addSerializer(Instant.class, new UtcInstantSerializer()));
	}

	private static final class UtcInstantSerializer extends ValueSerializer<Instant> {

		@Override
		public void serialize(Instant value, JsonGenerator generator, SerializationContext context) throws JacksonException {
			if (value == null) {
				generator.writeNull();
				return;
			}
			generator.writeString(Timestamps.format(value));
		}
	}

}

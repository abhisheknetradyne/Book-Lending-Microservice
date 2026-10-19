package com.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {

	@Bean
	HikariDataSource dataSource(
			@Value("${spring.datasource.url}") String url,
			@Value("${spring.datasource.username}") String username,
			@Value("${spring.datasource.password}") String password,
			@Value("${spring.datasource.hikari.maximum-pool-size}") int maximumPoolSize,
			@Value("${spring.datasource.hikari.minimum-idle}") int minimumIdle,
			@Value("${spring.datasource.hikari.connection-timeout}") long connectionTimeout) {
		HikariConfig pool = new HikariConfig();
		pool.setPoolName("library-pool");
		pool.setJdbcUrl(url);
		pool.setUsername(username);
		pool.setPassword(password);
		pool.setMaximumPoolSize(maximumPoolSize);
		pool.setMinimumIdle(minimumIdle);
		pool.setConnectionTimeout(connectionTimeout);
		return new HikariDataSource(pool);
	}

}

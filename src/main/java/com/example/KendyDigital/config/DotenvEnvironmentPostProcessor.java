package com.example.KendyDigital.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Automatically loads .env file into Spring Environment on application startup.
 * Allows running `mvn spring-boot:run` directly without needing prior environment export.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);
    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = findEnvFile();
        if (envFile == null || !envFile.exists()) {
            log.debug("[Dotenv] No .env file found.");
            return;
        }

        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(envFile.getParent() != null ? envFile.getParent() : ".")
                    .filename(envFile.getName())
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> envMap = new HashMap<>();
            for (DotenvEntry entry : dotenv.entries()) {
                envMap.put(entry.getKey(), entry.getValue());
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            }

            if (!envMap.isEmpty()) {
                if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
                    environment.getPropertySources().addAfter(
                            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            new MapPropertySource(PROPERTY_SOURCE_NAME, envMap));
                } else {
                    environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, envMap));
                }
                log.info("[Dotenv] Loaded {} environment variables from {}", envMap.size(), envFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("[Dotenv] Failed to load .env file from {}: {}", envFile.getAbsolutePath(), e.getMessage());
        }
    }

    private File findEnvFile() {
        String customPath = System.getProperty("dotenv.path");
        if (customPath != null && !customPath.isBlank()) {
            File custom = new File(customPath);
            if (custom.isFile()) {
                return custom.getAbsoluteFile();
            }
        }

        File[] candidates = new File[] {
                new File(".env"),
                new File("../.env"),
                new File("../../.env")
        };

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                return candidate.getAbsoluteFile();
            }
        }
        return null;
    }
}

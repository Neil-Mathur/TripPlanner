package com.weather.api.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads a .env file from the project root before Spring resolves @Value placeholders.
 *
 * This runs earlier than @Configuration classes, so by the time AgentConfig
 * tries to inject ${openai.api.key}, the value from .env is already available.
 *
 * Equivalent to load_dotenv() in Python.
 *
 * Registration: META-INF/spring.factories (see that file).
 */
public class DotEnvPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()   // .env is optional; env vars work too
                    .load();

            Map<String, Object> props = new HashMap<>();
            dotenv.entries().forEach(e -> props.put(e.getKey(), e.getValue()));

            if (!props.isEmpty()) {
                environment.getPropertySources()
                        .addLast(new MapPropertySource("dotenv", props));
            }
        } catch (DotenvException ex) {
            // .env file malformed — surface as a clear message
            throw new IllegalStateException("Failed to load .env file: " + ex.getMessage(), ex);
        }
    }
}

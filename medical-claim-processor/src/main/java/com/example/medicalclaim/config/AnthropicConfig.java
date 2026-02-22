package com.example.medicalclaim.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link AnthropicClient} bean.
 *
 * <p>Reads the API key from the {@code ANTHROPIC_API_KEY} environment variable
 * (default) or from {@code anthropic.api-key} in application.properties.
 * The environment variable takes precedence when both are set.
 */
@Configuration
public class AnthropicConfig {

    @Value("${anthropic.api-key:}")
    private String apiKeyProperty;

    @Bean
    public AnthropicClient anthropicClient() {
        // Prefer the environment variable; fall back to application.properties
        String envKey = System.getenv("ANTHROPIC_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return AnthropicOkHttpClient.builder()
                    .apiKey(envKey)
                    .build();
        }
        if (!apiKeyProperty.isBlank()) {
            return AnthropicOkHttpClient.builder()
                    .apiKey(apiKeyProperty)
                    .build();
        }
        // fromEnv() will throw a clear error if ANTHROPIC_API_KEY is not set
        return AnthropicOkHttpClient.fromEnv();
    }
}

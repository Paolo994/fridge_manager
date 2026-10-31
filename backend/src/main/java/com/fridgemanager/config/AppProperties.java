package com.fridgemanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Expiry expiry, Ai ai) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Expiry(int warningDays, String cron) {}

    public record Ai(String apiKey, String model) {
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}

package com.example.inventory_api_springboot.properties;

import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NoArgsConstructor
public final class SecurityConfigProperties {

    @ConfigurationProperties(prefix = "spring.crypto.argon2")
    public record Argon2ConfigProperties (
            int saltLength,      // e.g., 16
            int hashLength,      // e.g., 32
            int parallelism,     // e.g., 1
            int memoryKb,        // e.g., 4096 (Note: Spring uses KiB)
            int iterations
    ){}
}


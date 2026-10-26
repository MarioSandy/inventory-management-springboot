package com.example.inventory_api_springboot.config;

import com.example.inventory_api_springboot.properties.SecurityConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PasswordConfig {
    @Bean
    PasswordEncoder passwordEncoder(SecurityConfigProperties.Argon2ConfigProperties securityConfigProperties) {
        Map<String, PasswordEncoder> encoders = new HashMap<>();

        Argon2PasswordEncoder argon2PasswordEncoder = new Argon2PasswordEncoder(
                securityConfigProperties.saltLength(),
                securityConfigProperties.hashLength(),
                securityConfigProperties.parallelism(),
                securityConfigProperties.memoryKb(),
                securityConfigProperties.iterations()
        );

        encoders.put("argon2", argon2PasswordEncoder);
        return new DelegatingPasswordEncoder("argon2", encoders);
    }
}

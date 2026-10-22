package com.example.inventory_api_springboot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.jwt")
public record JwtServiceProperties (
    String secretKey
){}

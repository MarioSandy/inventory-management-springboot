package com.example.inventory_api_springboot.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.UUID;

@Service
@ConfigurationPropertiesScan
public class JwtService {
    private final JwtEncoder encoder;

    public JwtService(@Value("${spring.jwt.secret}") String secret) {
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    public String generateToken(UUID userId, UUID sessionId, TemporalAmount expiryTime) {
        Instant now = Instant.now();

        // Build JWS header containing algorithm and Key ID (kid)
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        // Build Payload Claims
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("inventoryApiSpringboot")
                .issuedAt(now)
                .expiresAt(now.plus(expiryTime))
                .subject(userId.toString())
                .claim("session_id", sessionId)
//                .claim("scope", scope)
                .build();

        return this.encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}

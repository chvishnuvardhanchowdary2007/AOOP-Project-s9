package com.klu.casestudy5;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record VerificationToken(String token, Instant expiresAt) {
    public VerificationToken {
        Objects.requireNonNull(token, "Token string cannot be null");
        Objects.requireNonNull(expiresAt, "Expiry timestamp cannot be null");
    }

    public static VerificationToken createWithTtl(long validSeconds) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(validSeconds);
        return new VerificationToken(token, expiresAt);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}

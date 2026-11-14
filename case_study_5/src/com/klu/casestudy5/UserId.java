package com.klu.casestudy5;

import java.util.Objects;
import java.util.UUID;

public record UserId(String value) {
    public UserId {
        Objects.requireNonNull(value, "UserId cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("UserId cannot be blank");
    }

    public static UserId generate() {
        return new UserId("USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}

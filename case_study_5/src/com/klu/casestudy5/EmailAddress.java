package com.klu.casestudy5;

import java.util.Objects;
import java.util.regex.Pattern;

public final class EmailAddress {
    private static final Pattern RFC_5322_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private final String value;

    public EmailAddress(String rawEmail) {
        Objects.requireNonNull(rawEmail, "Email address cannot be null");
        String trimmed = rawEmail.trim();
        if (!RFC_5322_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email address format: " + rawEmail);
        }
        // Invariant: Normalized to lowercase to guarantee case-insensitive uniqueness
        this.value = trimmed.toLowerCase();
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailAddress other)) return false;
        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}

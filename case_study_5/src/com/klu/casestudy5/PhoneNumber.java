package com.klu.casestudy5;

import java.util.Objects;
import java.util.regex.Pattern;

public final class PhoneNumber {
    // E.164 international standard regex: starts with +, followed by 1 to 15 digits
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    private final String value;

    public PhoneNumber(String rawPhone) {
        Objects.requireNonNull(rawPhone, "Phone number cannot be null");
        String sanitized = rawPhone.replaceAll("[\\s\\-\\(\\)]", "");
        if (!E164_PATTERN.matcher(sanitized).matches()) {
            throw new IllegalArgumentException(
                "Phone number must conform to international E.164 format (e.g. +14155552671). Given: " + rawPhone);
        }
        this.value = sanitized;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber other)) return false;
        return value.equals(other.value);
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

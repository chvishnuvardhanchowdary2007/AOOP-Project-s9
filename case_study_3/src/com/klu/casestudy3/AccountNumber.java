package com.klu.casestudy3;

import java.util.Objects;

public record AccountNumber(String value) {
    public AccountNumber {
        Objects.requireNonNull(value, "Account number cannot be null");
        if (!value.matches("^[0-9]{10,16}$")) {
            throw new IllegalArgumentException("Account number must be 10 to 16 digits. Given: " + value);
        }
    }
}

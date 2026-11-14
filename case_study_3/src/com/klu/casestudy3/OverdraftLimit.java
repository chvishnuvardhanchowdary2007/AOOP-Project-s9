package com.klu.casestudy3;

import java.util.Objects;

public record OverdraftLimit(Money limit) {
    public OverdraftLimit {
        Objects.requireNonNull(limit, "Overdraft limit cannot be null");
        if (limit.isNegative()) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative: " + limit);
        }
    }

    public static OverdraftLimit zero(String currency) {
        return new OverdraftLimit(Money.zero(currency));
    }
}

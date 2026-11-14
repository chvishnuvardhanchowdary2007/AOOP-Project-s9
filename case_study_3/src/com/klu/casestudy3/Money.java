package com.klu.casestudy3;

import java.math.BigDecimal;
import java.util.Objects;

public final class Money implements Comparable<Money> {
    private final long minorUnits; // e.g. paisa / cents
    private final String currency;

    private Money(long minorUnits, String currency) {
        this.minorUnits = minorUnits;
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
    }

    public static Money of(long minorUnits, String currency) {
        return new Money(minorUnits, currency);
    }

    public static Money of(String decimal, String currency) {
        Objects.requireNonNull(decimal, "Amount cannot be null");
        BigDecimal bd = new BigDecimal(decimal);
        BigDecimal scaled = bd.movePointRight(2);
        return new Money(scaled.longValueExact(), currency);
    }

    public static Money zero(String currency) {
        return new Money(0, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(this.minorUnits, other.minorUnits), this.currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(this.minorUnits, other.minorUnits), this.currency);
    }

    public boolean isPositive() {
        return this.minorUnits > 0;
    }

    public boolean isNegative() {
        return this.minorUnits < 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.minorUnits > other.minorUnits;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "Money operand cannot be null");
        if (!this.currency.equalsIgnoreCase(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    public long minorUnits() {
        return minorUnits;
    }

    public String currency() {
        return currency;
    }

    @Override
    public int compareTo(Money o) {
        requireSameCurrency(o);
        return Long.compare(this.minorUnits, o.minorUnits);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money other)) return false;
        return this.minorUnits == other.minorUnits && this.currency.equalsIgnoreCase(other.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minorUnits, currency.toUpperCase());
    }

    @Override
    public String toString() {
        BigDecimal decimal = BigDecimal.valueOf(minorUnits).movePointLeft(2);
        return currency + " " + decimal.toPlainString();
    }
}

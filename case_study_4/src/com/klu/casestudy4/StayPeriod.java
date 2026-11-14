package com.klu.casestudy4;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value Object representing a hotel stay period.
 * Enforces half-open interval [checkIn, checkOut) where checkOut is strictly after checkIn.
 */
public final class StayPeriod {
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    public StayPeriod(LocalDate checkIn, LocalDate checkOut) {
        Objects.requireNonNull(checkIn, "Check-in date cannot be null");
        Objects.requireNonNull(checkOut, "Check-out date cannot be null");

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                "Check-out date (" + checkOut + ") must be strictly after check-in date (" + checkIn + ")");
        }
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public long numberOfNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Checks if this stay period overlaps with another stay period.
     * Consecutive reservations (one checking out on the same day the other checks in)
     * do NOT overlap because of the half-open interval [checkIn, checkOut).
     */
    public boolean overlapsWith(StayPeriod other) {
        Objects.requireNonNull(other, "Other stay period cannot be null");
        return this.checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(this.checkOut);
    }

    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StayPeriod other)) return false;
        return checkIn.equals(other.checkIn) && checkOut.equals(other.checkOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkIn, checkOut);
    }

    @Override
    public String toString() {
        return checkIn + " to " + checkOut + " (" + numberOfNights() + " nights)";
    }
}

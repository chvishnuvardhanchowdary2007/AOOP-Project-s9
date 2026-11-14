package com.klu.casestudy4;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record CancellationPolicy(int freeCancellationHours, int penaltyPercentage) {
    public CancellationPolicy {
        if (freeCancellationHours < 0) throw new IllegalArgumentException("Hours cannot be negative");
        if (penaltyPercentage < 0 || penaltyPercentage > 100) {
            throw new IllegalArgumentException("Penalty must be between 0 and 100 percent");
        }
    }

    public static CancellationPolicy standard() {
        return new CancellationPolicy(24, 50); // 24 hours free, 50% penalty if cancelled within 24h
    }

    public long calculateRefundMinor(long totalPaidMinor, LocalDateTime checkInDateTime, LocalDateTime cancellationDateTime) {
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(cancellationDateTime, checkInDateTime);
        if (hoursUntilCheckIn >= freeCancellationHours) {
            return totalPaidMinor; // 100% refund
        } else {
            long penalty = (totalPaidMinor * penaltyPercentage) / 100;
            return totalPaidMinor - penalty;
        }
    }
}

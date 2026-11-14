package com.klu.casestudy4;

import java.util.Objects;

public record PaymentGuarantee(String cardToken, long preAuthMinorUnits, boolean isVerified) {
    public PaymentGuarantee {
        Objects.requireNonNull(cardToken, "Card token cannot be null");
        if (preAuthMinorUnits <= 0) {
            throw new IllegalArgumentException("Pre-authorization amount must be positive");
        }
        if (!isVerified) {
            throw new IllegalArgumentException("Payment guarantee must be verified by payment gateway");
        }
    }
}

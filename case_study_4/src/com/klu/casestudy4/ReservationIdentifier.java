package com.klu.casestudy4;

import java.util.Objects;

public record ReservationIdentifier(String code) {
    public ReservationIdentifier {
        Objects.requireNonNull(code, "Reservation code cannot be null");
        if (code.isBlank()) throw new IllegalArgumentException("Reservation code cannot be blank");
    }
}

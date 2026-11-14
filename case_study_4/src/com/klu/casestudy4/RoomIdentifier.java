package com.klu.casestudy4;

import java.util.Objects;

public record RoomIdentifier(String roomNumber) {
    public RoomIdentifier {
        Objects.requireNonNull(roomNumber, "Room number cannot be null");
        if (roomNumber.isBlank()) throw new IllegalArgumentException("Room number cannot be blank");
    }
}

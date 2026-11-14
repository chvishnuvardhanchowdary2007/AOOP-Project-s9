package com.klu.casestudy4;

import java.util.Objects;

public final class Guest {
    private final String guestId;
    private final String fullName;
    private final String email;

    public Guest(String guestId, String fullName, String email) {
        this.guestId = Objects.requireNonNull(guestId, "Guest ID cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
    }

    public String guestId() { return guestId; }
    public String fullName() { return fullName; }
    public String email() { return email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guest other)) return false;
        return guestId.equals(other.guestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(guestId);
    }

    @Override
    public String toString() {
        return fullName + " (" + email + ")";
    }
}

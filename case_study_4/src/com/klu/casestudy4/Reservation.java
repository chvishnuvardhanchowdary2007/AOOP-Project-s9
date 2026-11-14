package com.klu.casestudy4;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public final class Reservation {
    private final ReservationIdentifier id;
    private final Guest guest;
    private final RoomIdentifier room;
    private final StayPeriod stayPeriod;
    private final long ratePerNightMinor;
    private final CancellationPolicy cancellationPolicy;

    private ReservationStatus status = ReservationStatus.REQUESTED;
    private Optional<PaymentGuarantee> paymentGuarantee = Optional.empty();
    private Optional<Long> refundIssuedMinor = Optional.empty();

    public Reservation(ReservationIdentifier id, Guest guest, RoomIdentifier room, 
                       StayPeriod stayPeriod, long ratePerNightMinor, CancellationPolicy cancellationPolicy) {
        this.id = Objects.requireNonNull(id, "Reservation ID cannot be null");
        this.guest = Objects.requireNonNull(guest, "Guest cannot be null");
        this.room = Objects.requireNonNull(room, "Room identifier cannot be null");
        this.stayPeriod = Objects.requireNonNull(stayPeriod, "Stay period cannot be null");
        if (ratePerNightMinor <= 0) throw new IllegalArgumentException("Nightly rate must be positive");
        this.ratePerNightMinor = ratePerNightMinor;
        this.cancellationPolicy = Objects.requireNonNull(cancellationPolicy, "Cancellation policy cannot be null");
    }

    public long totalCostMinor() {
        return Math.multiplyExact(ratePerNightMinor, stayPeriod.numberOfNights());
    }

    public void confirm(PaymentGuarantee guarantee) {
        requireStatus(ReservationStatus.REQUESTED, "confirm");
        Objects.requireNonNull(guarantee, "Payment guarantee is strictly required to confirm a booking");
        this.paymentGuarantee = Optional.of(guarantee);
        this.status = ReservationStatus.CONFIRMED;
    }

    public void checkIn(LocalDate today) {
        requireStatus(ReservationStatus.CONFIRMED, "check in");
        if (today.isBefore(stayPeriod.checkIn())) {
            throw new HotelReservationException("Cannot check in before scheduled check-in date: " + stayPeriod.checkIn());
        }
        if (today.isAfter(stayPeriod.checkOut())) {
            throw new HotelReservationException("Cannot check in after scheduled check-out date: " + stayPeriod.checkOut());
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        requireStatus(ReservationStatus.CHECKED_IN, "check out");
        this.status = ReservationStatus.COMPLETED;
    }

    public long cancel(LocalDateTime cancellationTime) {
        if (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.COMPLETED) {
            throw new HotelReservationException("Illegal cancellation: cannot cancel a reservation that is already " + status);
        }
        if (status == ReservationStatus.CANCELLED) {
            throw new HotelReservationException("Reservation is already cancelled");
        }

        LocalDateTime checkInDateTime = stayPeriod.checkIn().atTime(14, 0); // standard 2:00 PM check-in
        long refund = cancellationPolicy.calculateRefundMinor(totalCostMinor(), checkInDateTime, cancellationTime);
        this.refundIssuedMinor = Optional.of(refund);
        this.status = ReservationStatus.CANCELLED;
        return refund;
    }

    private void requireStatus(ReservationStatus expected, String action) {
        if (this.status != expected) {
            throw new HotelReservationException("Cannot " + action + ": reservation is in status " + this.status + " (expected " + expected + ")");
        }
    }

    public ReservationIdentifier id() { return id; }
    public Guest guest() { return guest; }
    public RoomIdentifier room() { return room; }
    public StayPeriod stayPeriod() { return stayPeriod; }
    public ReservationStatus status() { return status; }
    public Optional<PaymentGuarantee> paymentGuarantee() { return paymentGuarantee; }
    public Optional<Long> refundIssuedMinor() { return refundIssuedMinor; }
}

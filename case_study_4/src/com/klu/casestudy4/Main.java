package com.klu.casestudy4;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   CASE STUDY 4: Hotel Reservations & Dates       ");
        System.out.println("==================================================");

        // 1. Date Range Validation & Overlap Logic
        LocalDate dec20 = LocalDate.of(2026, 12, 20);
        LocalDate dec22 = LocalDate.of(2026, 12, 22);
        LocalDate dec25 = LocalDate.of(2026, 12, 25);
        LocalDate dec27 = LocalDate.of(2026, 12, 27);
        LocalDate dec30 = LocalDate.of(2026, 12, 30);

        StayPeriod stayA = new StayPeriod(dec20, dec25); // Dec 20 - Dec 25 (5 nights)
        StayPeriod stayB = new StayPeriod(dec25, dec30); // Dec 25 - Dec 30 (Consecutive, same checkout/in day)
        StayPeriod stayC = new StayPeriod(dec22, dec27); // Dec 22 - Dec 27 (Overlaps with Stay A!)

        System.out.println("Stay A: " + stayA);
        System.out.println("Stay B: " + stayB);
        System.out.println("Stay C: " + stayC);

        System.out.println("\nChecking Overlaps (Half-open intervals [checkIn, checkOut)):");
        System.out.println(" - Stay A & Stay B (Consecutive turnover day): Overlap? " + stayA.overlapsWith(stayB));
        System.out.println(" - Stay A & Stay C (Direct conflict):        Overlap? " + stayA.overlapsWith(stayC));

        // 2. Reservation Lifecycle
        Guest guest = new Guest("GUEST-101", "Alice Smith", "alice@example.com");
        RoomIdentifier room = new RoomIdentifier("ROOM-302");
        Reservation reservation = new Reservation(
            new ReservationIdentifier("RES-9988"),
            guest,
            room,
            stayA,
            12000, // $120.00/night
            CancellationPolicy.standard()
        );

        System.out.println("\nCreated Reservation: " + reservation.id().code() + " | Total: $" + (reservation.totalCostMinor() / 100.0));
        System.out.println("Status: " + reservation.status());

        // 3. Confirm with Payment Guarantee
        PaymentGuarantee guarantee = new PaymentGuarantee("CARD-TOKEN-VISA-4111", reservation.totalCostMinor(), true);
        reservation.confirm(guarantee);
        System.out.println("After confirm(paymentGuarantee): " + reservation.status());

        // 4. Test Invariant Guards
        System.out.println("\n--- Testing Invariant Guards ---");

        // (a) Invalid Date Range (checkOut before checkIn)
        try {
            System.out.print("Attempting to create Stay with checkout before checkin: ");
            new StayPeriod(LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 20));
        } catch (IllegalArgumentException e) {
            System.out.println("BLOCKED -> " + e.getMessage());
        }

        // (b) Check-in transitions
        reservation.checkIn(dec20);
        System.out.println("Checked in successfully! Status: " + reservation.status());

        // (c) Illegal cancellation after check-in
        try {
            System.out.print("Attempting to cancel reservation AFTER check-in: ");
            reservation.cancel(LocalDateTime.now());
        } catch (HotelReservationException e) {
            System.out.println("BLOCKED -> " + e.getMessage());
        }

        // (d) Complete stay
        reservation.checkOut();
        System.out.println("Checked out! Status: " + reservation.status());

        System.out.println("==================================================");
        System.out.println("  Case Study 4 Invariants Fully Enforced!         ");
        System.out.println("==================================================");
    }
}

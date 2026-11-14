# Case Study 4: Hotel Reservations and Date Ranges

## Problem Analysis
A hotel reservation system encountered serious operational breakdowns:
1. Invalid stay periods where check-out occurred before or on check-in.
2. Double-booking bugs where overlapping date ranges were reserved for the same room.
3. Unconfirmed bookings lacking payment guarantees leading to revenue loss on no-shows.
4. Illegal cancellations after check-in receiving unwarranted refunds.
5. Inability to distinguish consecutive turnover stays (guest checking out at 11 AM while another checks in at 2 PM) from genuine conflicts.

---

## Solutions to Assignment Questions

### Q1. Domain Invariants and Value Objects (6 Marks)
1. **Invariant 1: Chronological Stay Ordering**  
   *Check-in must strictly precede check-out by at least one night* (`checkOut > checkIn`). Same-day or reverse check-outs are illegal.
2. **Invariant 2: Mutual Exclusivity of Room Occupancy (No Double Bookings)**  
   *No two active reservations for the same room may overlap in their stay period:*  
   For any active $R_1, R_2$ on the same room: $R_1.\text{checkIn} < R_2.\text{checkOut} \land R_2.\text{checkIn} < R_1.\text{checkOut}$ must evaluate to false.
3. **Invariant 3: Half-Open Turnover Correctness**  
   A guest checking out on date $D$ does not conflict with a guest checking in on date $D$. The interval is modeled as half-open: $[\text{checkIn}, \text{checkOut})$.
4. **Invariant 4: Pre-Condition for Confirmation (Payment Guarantee)**  
   A reservation cannot enter `CONFIRMED` status without a validated `PaymentGuarantee` (verified token and pre-authorized funds).
5. **Invariant 5: Cancellation Preclusion After Check-in**  
   Cancellations are strictly prohibited once a guest has checked in (`status == CHECKED_IN`) or completed their stay.
6. **Invariant 6: Policy-Bounded Refund Calculation**  
   Refunds are calculated deterministically by evaluating the time delta between cancellation timestamp and scheduled check-in time against the `CancellationPolicy`.

* **Concepts represented as Value Objects:**
  * `StayPeriod`: Defined purely by its start and end dates; immutable with self-contained overlap comparison logic.
  * `RoomIdentifier` & `ReservationIdentifier`: Strongly typed identity wrappers preventing accidental parameter swapping.
  * `PaymentGuarantee`: Immutable proof of pre-authorization from the payment gateway.
  * `CancellationPolicy`: Encapsulates penalty rules and hours without identity or lifecycle.

---

### Q2. Designing DateRange and Reservation (8 Marks)
* **Half-Open Interval Representation:** Stays are modeled as $[\text{checkIn}, \text{checkOut})$.
* **Consecutive Reservation Handling:**  
  Standard overlap comparison:
  ```java
  public boolean overlapsWith(StayPeriod other) {
      return this.checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(this.checkOut);
  }
  ```
  If Guest 1 stays Dec 20–Dec 25 and Guest 2 stays Dec 25–Dec 30:  
  `Dec 20 < Dec 30 (true)` AND `Dec 25 < Dec 25 (false)` $\rightarrow$ **Result: false (No overlap)**. Turnovers on the same day are correctly accommodated!
* **Self-Enforcing Constructor:** The constructor validates `!checkOut.isAfter(checkIn)` and throws `IllegalArgumentException`, making inverted or zero-night ranges unrepresentable.

---

### Q3. Entity Classification and Aggregate Design (6 Marks)

| Concept | Kind | Justification |
| :--- | :--- | :--- |
| **Guest** | **Entity** | Identified by `guestId`; personal details and contact data evolve over time. |
| **Room** | **Entity** | Physical entity identified by `RoomIdentifier` (e.g., room number); status and amenities persist across bookings. |
| **Reservation** | **Entity (Aggregate Root)** | Has a unique lifecycle (`id`), tracking status transitions from Requested to Completed. |
| **RoomIdentifier** | **Value Object** | Immutable string wrapper with equality based on the room number string. |
| **ReservationIdentifier**| **Value Object** | Unique booking reference code without mutable state. |
| **StayPeriod** | **Value Object** | Immutable date range with mathematical interval behavior. |
| **CancellationPolicy** | **Value Object** | Reusable policy rule calculating refund percentages. |

* **Consistency Boundary for Room Availability:**  
  Enforcing room availability across overlapping dates cannot be encapsulated inside a single `Reservation` object because one reservation cannot see other reservations.  
  *Solution:* The consistency boundary belongs to the **Room Aggregate** (which maintains a list of reserved `StayPeriod` intervals) or a **Domain Service** (`ReservationBookingService`) backed by database exclusion constraints.

---

### Q4. Reservation Lifecycle and Making Illegal States Unrepresentable (6 Marks)
* **Permitted Transitions:**
  * `REQUESTED -> CONFIRMED` (Requires non-null `PaymentGuarantee`)
  * `REQUESTED -> CANCELLED`
  * `CONFIRMED -> CHECKED_IN` (Requires current date $\ge$ scheduled checkIn)
  * `CONFIRMED -> CANCELLED` (Triggers cancellation policy refund calculation)
  * `CHECKED_IN -> COMPLETED` (Guest checks out)
* **Prohibited Transitions:**
  * `CHECKED_IN -> CANCELLED` (Throws `HotelReservationException`)
  * `COMPLETED -> CANCELLED`
  * `CANCELLED -> Any State` (Terminal state)
* **Preventing Cancellation Post Check-In:**
  ```java
  public long cancel(LocalDateTime cancellationTime) {
      if (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.COMPLETED) {
          throw new HotelReservationException("Cannot cancel an order that is already " + status);
      }
      // calculate refund and transition to CANCELLED...
  }
  ```

---

### Q5. Concurrent Reservations and Cancellation Policies (4 Marks)
1. **Concurrent Collision Problem:** If two receptionists book Room 302 for Dec 20–25 simultaneously, both read the room as "available", write their bookings, and create a double-booking crisis.
2. **Protection Strategies:**
   * **Database Exclusion Constraints:** In PostgreSQL, use range types with exclusion constraints: `EXCLUDE USING gist (room_id WITH =, stay_range WITH &&)`.
   * **Pessimistic Locking:** Acquire a row-level lock on the `Room` row during booking creation: `SELECT * FROM rooms WHERE id = ? FOR UPDATE`.
3. **Refund Calculation Logic:** Compare `ChronoUnit.HOURS.between(cancellationTime, checkInDateTime)` against `freeCancellationHours`. If within the penalty window, deduct penalty percentage and refund remainder; otherwise issue a 100% refund.

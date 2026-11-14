# Case Study 3: Bank Account and Money Transfers

## Problem Analysis
A retail banking organization experienced operational failures:
1. Customers withdrawing beyond permitted overdraft limits due to missing domain invariant guards.
2. Money deducted from source accounts but never credited to destination accounts due to lack of atomic transactions and compensating workflows.
3. Frozen or closed accounts continuing to service withdrawals because status checks were decentralized in external service layers.
4. Account balances drifting away from transaction histories due to mutable primitives and separate, unsynchronized ledger updates.
5. Zero or negative transaction amounts being accepted due to reliance on unvalidated primitives (`double`).

---

## Solutions to Assignment Questions

### Q1. Identification of Domain Invariants (6 Marks)
1. **Invariant 1: Non-Negative Transaction Amounts**
   * *Rule:* Any transaction amount (deposit, withdrawal, transfer) must be strictly positive (`amount > 0`).
   * *Importance:* Money cannot move in negative or zero magnitudes.
   * *Consequences if Violated:* Depositing a negative amount acts as an unauthorized withdrawal; a zero amount corrupts audit trails and wastes processing cycles.
2. **Invariant 2: Overdraft Bounded Balance**
   * *Rule:* For any withdrawal or debit, `balance - amount >= -overdraftLimit`.
   * *Importance:* Protects the bank from credit default and unsecured customer debt.
   * *Consequences if Violated:* Customers overdraw accounts without pre-approved credit lines, causing financial loss to the institution.
3. **Invariant 3: Conservation of Value in Transfers**
   * *Rule:* For an internal fund transfer of amount $X$, the source debit of $X$ and destination credit of $X$ must occur atomically: $\Delta \text{Balance}_{\text{source}} + \Delta \text{Balance}_{\text{destination}} = 0$.
   * *Importance:* Money cannot be created or destroyed during a transfer.
   * *Consequences if Violated:* System crashes between operations cause money loss or double counting.
4. **Invariant 4: Account Lifecycle State Enforcement**
   * *Rule:* Debit and credit operations are permitted if and only if the account is in the `ACTIVE` status. `FROZEN` and `CLOSED` accounts must reject transactions.
   * *Importance:* Ensures regulatory compliance, anti-money laundering (AML) controls, and legal freezes.
   * *Consequences if Violated:* Fraudulent or sanctioned withdrawals proceed against frozen assets.
5. **Invariant 5: Immutable Append-Only Ledger Integrity**
   * *Rule:* Account balance must always equal the historical sum of all ledger entries: $\text{Balance} = \sum \text{LedgerEntries}$. Ledger entries can never be modified or deleted.
   * *Importance:* Guarantees non-repudiation, tamper-evidence, and regulatory audit compliance.
   * *Consequences if Violated:* Balances diverge from bank statements, leading to untraceable accounting discrepancies.
6. **Invariant 6: Currency Uniformity**
   * *Rule:* Debits, credits, and overdraft evaluations must strictly operate in the native currency of the account.
   * *Importance:* Prevents accidental addition or deduction across differing currency units.
   * *Consequences if Violated:* Cross-currency corruption (e.g. subtracting USD from INR amounts 1:1).

---

### Q2. Entities and Value Objects (6 Marks)

| Concept | Kind | Justification based on Identity, Equality, Lifecycle & Mutability |
| :--- | :--- | :--- |
| **Bank Account** | **Entity (Aggregate Root)** | Identified uniquely by `AccountNumber`. Its balance, overdraft limit, and status change over a long-lived lifecycle while retaining the same identity. |
| **Account Number** | **Value Object** | Immutable string wrapper with strict format rules (`10-16 digits`). Equality is based purely on the string digits. |
| **Money** | **Value Object** | Immutable quantity defined solely by its minor integer units and currency code. Two instances of `₹5,000` are interchangeable. |
| **Ledger Entry** | **Value Object / Domain Audit Entity** | Append-only, immutable record capturing a specific transaction snapshot. Equality is defined by its immutable transaction properties. |
| **Overdraft Limit** | **Value Object** | Encapsulates a non-negative `Money` limit. It has no independent lifecycle outside the account configuration. |
| **Fund Transfer** | **Domain Service / Process** | Represents an atomic coordination between two independent account aggregates rather than a persistent data structure. |

---

### Q3. Self-Enforcing Objects and Java Design (8 Marks)
* **Encapsulation:** The fields `balance`, `status`, and `ledgerEntries` in `BankAccount` are `private`. No public setters exist.
* **Invariant Guard Clauses:**
  * `deposit()` asserts `status == ACTIVE` and `amount.isPositive()`.
  * `withdraw()` asserts `status == ACTIVE`, `amount.isPositive()`, and `balance - amount >= -overdraftLimit`.
  * `close()` requires `balance == 0`.
* **Immutability of History:** The method `ledgerHistory()` returns `List.copyOf(ledgerEntries)`, preventing callers from clearing or tampering with history.

---

### Q4. Money Transfer and Aggregate Consistency (6 Marks)
1. **Problems Arising:** In a naive implementation where debit and credit are detached, a crash after debiting Account A results in customer funds disappearing into limbo.
2. **Invariants Violated:** Violates *Conservation of Money* and *Atomicity*.
3. **Design Solution (Local Boundary):** Wrap the operation in an atomic database transaction (`@Transactional` with serializable/read-committed isolation) or use double-entry bookkeeping with a compensating transaction:
   ```java
   source.recordTransferOut(amount, dest, ref);
   try {
       dest.recordTransferIn(amount, source, ref);
   } catch(Exception e) {
       source.deposit(amount, "COMPENSATION: Failed transfer refund");
   }
   ```
4. **Cross-System / Distributed Transfers:** If accounts belong to different banking systems (e.g., NEFT/RTGS/SWIFT), ACID transactions across systems are impossible. The transfer must be executed via the **Saga Pattern** or **Outbox Pattern** with eventual consistency:
   * Source bank places funds into an `ESCROW_HOLD` / `PENDING_TRANSFER` state.
   * Asynchronous message sent through interbank settlement rails.
   * Upon acknowledgement from the destination bank, source completes debit. If timeout/rejection occurs, compensating release unlocks the held funds.

---

### Q5. Ledger Management and Design Evaluation (4 Marks)
1. **Information in Ledger Entry:** Unique Entry ID, UTC Timestamp, Transaction Type (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT`), Transaction Amount, Resulting Balance snapshot, Reference/Counterparty ID, and Description.
2. **Modification / Deletion:** **Never.** Financial accounting mandates append-only ledgers. If an error occurs, an explicit reversing entry (compensating credit/debit) must be appended, preserving complete audit trails.
3. **Balance Consistency:** Balance updates occur only through methods that generate ledger entries within the exact same atomic aggregate method.
4. **Large History Challenges & Solutions:** Large histories degrade read performance and memory consumption. This is solved using **Snapshotting**: periodically checkpointing the account balance (e.g. at the close of every financial day) and archiving older ledger entries into cold storage, querying only transactions after the snapshot timestamp for active balance reconciliation.

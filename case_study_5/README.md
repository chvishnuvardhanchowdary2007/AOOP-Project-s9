# Case Study 5: User Accounts, Email Verification and Contact Details

## Problem Analysis
A SaaS user account management system experienced security and data integrity defects:
1. Malformed email addresses accepted due to lack of domain validation.
2. Contradictory states where accounts were marked verified without recorded timestamps, or verification tokens were accepted post-expiry.
3. Multiple accounts created for the same user due to case-sensitive capitalization differences (`User@example.com` vs `user@example.com`).
4. Inconsistent telephone formatting and partially null address fields.
5. Suspended users continuing to authenticate because access control checks were detached from user status.

---

## Solutions to Assignment Questions

### Q1. Identification of Invariants (6 Marks)
1. **Invariant 1: RFC-Compliant Normalized Email Address**  
   *Email addresses must adhere to RFC 5322 format and be canonicalized to lowercase upon construction.*  
   *Importance:* Guarantees deliverability and prevents duplicate account spoofing via capitalization tricks.
2. **Invariant 2: Temporal Validity of Verification Tokens**  
   *A verification token cannot be accepted if `currentInstant > token.expiresAt`.*  
   *Importance:* Prevents replay attacks and unauthorized activations using stale links.
3. **Invariant 3: Verification State Exclusivity**  
   *An account cannot be verified without a non-null `verifiedAt` timestamp, and a verified account cannot have an active verification token.*  
   *Importance:* Eliminates contradictory flags (`emailVerified = true` but `verifiedAt = null`).
4. **Invariant 4: E.164 Phone Number Standardization**  
   *Phone numbers must comply with international standard E.164 (`+[country code][subscriber number]`).*  
   *Importance:* Ensures automated SMS/MFA services operate reliably across international telecommunication carriers.
5. **Invariant 5: Complete Postal Address Atomicity**  
   *A postal address must have all required fields (street, city, postal code, country) populated; partial addresses are rejected.*  
   *Importance:* Prevents failed deliveries and tax calculation errors.
6. **Invariant 6: Active Lifecycle Requirement for Access**  
   *Only users in the `ACTIVE` status with a `Verified` email state may authenticate and execute operations.*  
   *Importance:* Suspended or soft-deleted accounts must be blocked immediately from tenant systems.
7. **Invariant 7: Reset on Email Mutation**  
   *Updating an email address automatically resets the verification state to `Unverified` and issues a new token.*  
   *Importance:* Prevents an account from retaining verified status while hijacking an unconfirmed address.

---

### Q2. Entities and Value Objects (6 Marks)

| Concept | Kind | Justification based on Identity, Equality, Lifecycle & Mutability |
| :--- | :--- | :--- |
| **User Account** | **Entity (Aggregate Root)** | Identified uniquely by `UserId`. Its status, contact details, and verification state change over time while preserving identity. |
| **User Identifier** | **Value Object** | Immutable unique identifier string without mutable state. Equality is based purely on the wrapped string. |
| **Email Address** | **Value Object** | Defined entirely by its normalized lowercase text. Two identical emails are interchangeable. |
| **Phone Number** | **Value Object** | Formatted string following E.164 rules without independent lifecycle or identity. |
| **Postal Address** | **Value Object** | Composite of street, city, code, and country. Has no lifecycle; if an address changes, the entire object is replaced. |
| **Verification Token**| **Value Object** | Immutable pairing of a cryptographically secure token string and an expiration timestamp. |

---

### Q3. Making Illegal States Unrepresentable (8 Marks)
* **Problems with Primitive/Nullable Design:**
  * Having separate boolean flags (`boolean emailVerified`), `Date verifiedAt`, and `String token` allows impossible combinations:
    1. `emailVerified = true` with `verifiedAt = null`.
    2. `emailVerified = false` with `verificationToken = null` (cannot verify).
    3. `emailVerified = true` with a lingering active token that could be exploited.
* **The Sealed Domain Solution (Java 17):**
  Instead of booleans, use a sealed interface:
  ```java
  public sealed interface EmailVerificationState permits 
      EmailVerificationState.Unverified, 
      EmailVerificationState.Verified {

      record Unverified(VerificationToken token) implements EmailVerificationState {}
      record Verified(Instant verifiedAt) implements EmailVerificationState {}
  }
  ```
  *Result:* In the `Unverified` record, the token is mandatory and guaranteed non-null; in the `Verified` record, `verifiedAt` is mandatory and no token exists. Contradictory states cannot compile or instantiate!
* **Optional Information:** `phone` and `address` are modeled using `Optional<PhoneNumber>` and `Optional<PostalAddress>`, guaranteeing that nulls cannot leak into business logic.

---

### Q4. Email Verification and Account Lifecycle (6 Marks)
1. **Registration Information:** Immutable `UserId`, validated `EmailAddress`, generated `VerificationToken` with a finite TTL (e.g. 24 hours), and status `ACTIVE`.
2. **Token Generation & Validation:** Use `UUID.randomUUID()` or `SecureRandom` combined with `Instant.now().plusSeconds(ttl)`. The method `verifyEmail(token, now)` checks equality and `now.isBefore(expiresAt)`.
3. **Expired Tokens:** If `now.isAfter(expiresAt)`, the method throws `UserAccountException("Verification token has expired")` and prompts regeneration.
4. **Email Change Workflow:** Calling `user.changeEmail(newEmail, newToken)` atomically updates the email field and transitions `verificationState` to `new Unverified(newToken)`, requiring the user to re-verify the new inbox.
5. **Suspension/Deletion Enforcement:** The aggregate encapsulates `attemptLogin()`:
   ```java
   public void attemptLogin() {
       if (status != AccountStatus.ACTIVE) throw new UserAccountException("Account is " + status);
       if (!verificationState.isVerified()) throw new UserAccountException("Email unverified");
   }
   ```

---

### Q5. Cross-Aggregate Rules and Design Evaluation (4 Marks)
1. **Is Email Uniqueness an Individual User Invariant?**  
   **No.** An individual `UserAccount` aggregate root only has visibility into its own internal state. It cannot query or know whether another user in the database possesses the same email. Cross-user uniqueness is a **system-wide boundary rule**.
2. **Enforcement:**  
   Enforced in a **Domain Service** (`UserRegistrationService`) before persisting, backed by a database unique constraint.
3. **Simultaneous Race Condition:**  
   If two users register with the same email concurrently, both application instances might validate the email as "unused" at the exact same millisecond.
4. **Database-Level Safeguard:**  
   A unique index on the normalized email column:
   ```sql
   CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email));
   ```
   If a race occurs, the database transaction serializes the writes, allowing the first to commit and causing the second to fail with a unique constraint violation (`DuplicateKeyException`), which is caught and translated to a friendly domain error.
5. **Trade-offs of Strongly Typed Verification Model:**
   * *Advantage:* Absolute compile-time type safety; impossible to represent or persist corrupt contradictory states.
   * *Disadvantage:* Requires custom ORM attribute converters or composite type mappers (e.g. `@Convert` or JPA embeddables) to serialize sealed types into relational tables.

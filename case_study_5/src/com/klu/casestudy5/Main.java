package com.klu.casestudy5;

import java.time.Instant;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   CASE STUDY 5: User Accounts & Email Verif.     ");
        System.out.println("==================================================");

        // 1. Email Normalization & Value Objects
        EmailAddress email1 = new EmailAddress("User.Name+Test@DOMAIN.com  ");
        System.out.println("Input: 'User.Name+Test@DOMAIN.com  ' -> Normalized: " + email1.value());

        // Value Object equality check
        EmailAddress email2 = new EmailAddress("user.name+test@domain.com");
        System.out.println("Email equality check (case-insensitive invariant): " + email1.equals(email2));

        // 2. Contact Details (E.164 and Complete Address)
        PhoneNumber phone = new PhoneNumber("+1 (415) 555-2671");
        System.out.println("Sanitized E.164 Phone: " + phone.value());

        PostalAddress address = new PostalAddress("123 Tech Park", "Bengaluru", "560001", "India");
        System.out.println("Postal Address: " + address);

        // 3. Register User (starts as Unverified)
        VerificationToken token = VerificationToken.createWithTtl(3600); // 1 hour TTL
        UserAccount user = new UserAccount(UserId.generate(), email1, token);
        user.updatePhone(phone);
        user.updateAddress(address);

        System.out.println("\nRegistered User: " + user.userId().value() + " | Email: " + user.email());
        System.out.println("Initial Verification State: Verified? " + user.verificationState().isVerified());

        // 4. Test Invariant Guards
        System.out.println("\n--- Testing Invariant Guards ---");

        // (a) Attempt login before verification
        try {
            System.out.print("Attempting to login before email verification: ");
            user.attemptLogin();
        } catch (UserAccountException e) {
            System.out.println("BLOCKED -> " + e.getMessage());
        }

        // (b) Test Expired Token rejection
        VerificationToken expiredToken = new VerificationToken("EXPIRED-TOKEN-123", Instant.now().minusSeconds(10));
        user.regenerateToken(expiredToken);
        try {
            System.out.print("Attempting to verify with expired token: ");
            user.verifyEmail("EXPIRED-TOKEN-123", Instant.now());
        } catch (UserAccountException e) {
            System.out.println("BLOCKED -> " + e.getMessage());
        }

        // (c) Successful verification
        VerificationToken validToken = VerificationToken.createWithTtl(600);
        user.regenerateToken(validToken);
        user.verifyEmail(validToken.token(), Instant.now());
        System.out.println("\nEmail successfully verified! Verified? " + user.verificationState().isVerified());

        // Now login succeeds
        user.attemptLogin();
        System.out.println("Login check passed! User is active and verified.");

        // (d) Suspended account login block
        System.out.println("\nSuspending user account...");
        user.suspend("Terms of service audit");
        try {
            System.out.print("Attempting login while SUSPENDED: ");
            user.attemptLogin();
        } catch (UserAccountException e) {
            System.out.println("BLOCKED -> " + e.getMessage());
        }

        System.out.println("==================================================");
        System.out.println("  Case Study 5 Invariants Fully Enforced!         ");
        System.out.println("==================================================");
    }
}

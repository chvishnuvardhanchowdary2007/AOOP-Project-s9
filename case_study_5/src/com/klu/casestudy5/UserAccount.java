package com.klu.casestudy5;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class UserAccount {
    private final UserId userId;
    private EmailAddress email;
    private EmailVerificationState verificationState;
    private Optional<PhoneNumber> phone = Optional.empty();
    private Optional<PostalAddress> address = Optional.empty();
    private AccountStatus status;

    public UserAccount(UserId userId, EmailAddress email, VerificationToken initialToken) {
        this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
        this.email = Objects.requireNonNull(email, "Email address cannot be null");
        this.verificationState = new EmailVerificationState.Unverified(initialToken);
        this.status = AccountStatus.ACTIVE;
    }

    public void verifyEmail(String candidateToken, Instant now) {
        if (verificationState instanceof EmailVerificationState.Verified) {
            throw new UserAccountException("Email is already verified");
        }

        EmailVerificationState.Unverified unverified = (EmailVerificationState.Unverified) verificationState;
        if (unverified.token().isExpired(now)) {
            throw new UserAccountException("Verification token has expired. Please request a new token.");
        }

        if (!unverified.token().token().equals(candidateToken)) {
            throw new UserAccountException("Invalid verification token provided.");
        }

        // Atomic transition to Verified state
        this.verificationState = new EmailVerificationState.Verified(now);
    }

    public void regenerateToken(VerificationToken newToken) {
        if (verificationState instanceof EmailVerificationState.Verified) {
            throw new UserAccountException("Cannot regenerate token: email is already verified");
        }
        this.verificationState = new EmailVerificationState.Unverified(newToken);
    }

    public void changeEmail(EmailAddress newEmail, VerificationToken newToken) {
        requireActive("change email");
        this.email = Objects.requireNonNull(newEmail, "New email cannot be null");
        // Invariant: Changing email resets verification state back to Unverified
        this.verificationState = new EmailVerificationState.Unverified(newToken);
    }

    public void updatePhone(PhoneNumber phone) {
        requireActive("update phone number");
        this.phone = Optional.ofNullable(phone);
    }

    public void updateAddress(PostalAddress address) {
        requireActive("update address");
        this.address = Optional.ofNullable(address);
    }

    public void suspend(String reason) {
        if (status == AccountStatus.DELETED) {
            throw new UserAccountException("Cannot suspend a deleted account");
        }
        this.status = AccountStatus.SUSPENDED;
    }

    public void reactivate() {
        if (status == AccountStatus.DELETED) {
            throw new UserAccountException("Cannot reactivate a deleted account");
        }
        this.status = AccountStatus.ACTIVE;
    }

    public void delete() {
        this.status = AccountStatus.DELETED;
    }

    public void attemptLogin() {
        if (status == AccountStatus.DELETED) {
            throw new UserAccountException("Login failed: account has been deleted");
        }
        if (status == AccountStatus.SUSPENDED) {
            throw new UserAccountException("Login failed: account is currently suspended");
        }
        if (!verificationState.isVerified()) {
            throw new UserAccountException("Login failed: email address is not yet verified");
        }
    }

    private void requireActive(String action) {
        if (this.status != AccountStatus.ACTIVE) {
            throw new UserAccountException("Cannot " + action + ": account status is " + this.status);
        }
    }

    public UserId userId() { return userId; }
    public EmailAddress email() { return email; }
    public EmailVerificationState verificationState() { return verificationState; }
    public Optional<PhoneNumber> phone() { return phone; }
    public Optional<PostalAddress> address() { return address; }
    public AccountStatus status() { return status; }
}

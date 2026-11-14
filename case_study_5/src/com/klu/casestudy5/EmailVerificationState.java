package com.klu.casestudy5;

import java.time.Instant;
import java.util.Objects;

/**
 * Sealed hierarchy eliminating impossible states:
 * - An unverified email ALWAYS has a verification token and NEVER has a verifiedAt timestamp.
 * - A verified email ALWAYS has a verifiedAt timestamp and NEVER has a lingering verification token.
 */
public sealed interface EmailVerificationState permits 
    EmailVerificationState.Unverified, 
    EmailVerificationState.Verified {

    boolean isVerified();

    record Unverified(VerificationToken token) implements EmailVerificationState {
        public Unverified {
            Objects.requireNonNull(token, "Verification token cannot be null");
        }

        @Override
        public boolean isVerified() {
            return false;
        }
    }

    record Verified(Instant verifiedAt) implements EmailVerificationState {
        public Verified {
            Objects.requireNonNull(verifiedAt, "Verified timestamp cannot be null");
        }

        @Override
        public boolean isVerified() {
            return true;
        }
    }
}

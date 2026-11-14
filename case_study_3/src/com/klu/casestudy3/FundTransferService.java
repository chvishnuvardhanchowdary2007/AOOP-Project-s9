package com.klu.casestudy3;

import java.util.Objects;
import java.util.UUID;

public final class FundTransferService {

    public void transfer(BankAccount source, BankAccount destination, Money amount) {
        Objects.requireNonNull(source, "Source account cannot be null");
        Objects.requireNonNull(destination, "Destination account cannot be null");
        Objects.requireNonNull(amount, "Transfer amount cannot be null");

        if (source.accountNumber().equals(destination.accountNumber())) {
            throw new BankingException("Cannot transfer money to the same account");
        }

        String referenceId = UUID.randomUUID().toString().substring(0, 8);

        // Atomic double-entry execution with rollback compensation
        source.recordTransferOut(amount, destination.accountNumber(), referenceId);
        try {
            destination.recordTransferIn(amount, source.accountNumber(), referenceId);
        } catch (Exception ex) {
            // Compensating action to guarantee aggregate balance conservation
            source.deposit(amount, "COMPENSATION: Reversal for failed transfer " + referenceId);
            throw new BankingException("Transfer failed during destination credit. Source has been refunded: " + ex.getMessage());
        }
    }
}

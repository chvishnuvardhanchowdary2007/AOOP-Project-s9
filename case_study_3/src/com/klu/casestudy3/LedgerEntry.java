package com.klu.casestudy3;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class LedgerEntry {
    private final String entryId;
    private final Instant timestamp;
    private final TransactionType type;
    private final Money amount;
    private final Money balanceAfter;
    private final String description;

    public LedgerEntry(TransactionType type, Money amount, Money balanceAfter, String description) {
        this.entryId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.balanceAfter = Objects.requireNonNull(balanceAfter, "Balance after cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
    }

    public String entryId() { return entryId; }
    public Instant timestamp() { return timestamp; }
    public TransactionType type() { return type; }
    public Money amount() { return amount; }
    public Money balanceAfter() { return balanceAfter; }
    public String description() { return description; }

    @Override
    public String toString() {
        return String.format("[%s] %-12s %s | Balance: %s | %s",
            timestamp, type, amount, balanceAfter, description);
    }
}

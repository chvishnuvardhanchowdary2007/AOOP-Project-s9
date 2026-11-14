package com.klu.casestudy3;

import java.util.*;

public final class BankAccount {
    private final AccountNumber accountNumber;
    private final String currency;
    private Money balance;
    private OverdraftLimit overdraftLimit;
    private AccountStatus status;
    private final List<LedgerEntry> ledgerEntries = new ArrayList<>();

    public BankAccount(AccountNumber accountNumber, String currency, OverdraftLimit overdraftLimit) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.overdraftLimit = Objects.requireNonNull(overdraftLimit, "Overdraft limit cannot be null");
        this.balance = Money.zero(currency);
        this.status = AccountStatus.ACTIVE;
    }

    public synchronized void deposit(Money amount, String description) {
        requireStatus(AccountStatus.ACTIVE, "deposit");
        if (!amount.isPositive()) {
            throw new BankingException("Deposit amount must be strictly positive. Given: " + amount);
        }
        balance = balance.plus(amount);
        ledgerEntries.add(new LedgerEntry(TransactionType.DEPOSIT, amount, balance, description));
    }

    public synchronized void withdraw(Money amount, String description) {
        requireStatus(AccountStatus.ACTIVE, "withdraw");
        if (!amount.isPositive()) {
            throw new BankingException("Withdrawal amount must be strictly positive. Given: " + amount);
        }
        
        // Invariant: balance - amount >= -overdraftLimit
        long maxWithdrawable = Math.addExact(balance.minorUnits(), overdraftLimit.limit().minorUnits());
        if (amount.minorUnits() > maxWithdrawable) {
            throw new BankingException(String.format(
                "Withdrawal of %s exceeds available funds. Balance: %s, Overdraft Limit: %s",
                amount, balance, overdraftLimit.limit()));
        }

        balance = balance.minus(amount);
        ledgerEntries.add(new LedgerEntry(TransactionType.WITHDRAWAL, amount, balance, description));
    }

    public synchronized void recordTransferIn(Money amount, AccountNumber source, String reference) {
        requireStatus(AccountStatus.ACTIVE, "receive transfer");
        if (!amount.isPositive()) {
            throw new BankingException("Transfer amount must be positive");
        }
        balance = balance.plus(amount);
        ledgerEntries.add(new LedgerEntry(TransactionType.TRANSFER_IN, amount, balance, 
            "Transfer from " + source.value() + " [Ref: " + reference + "]"));
    }

    public synchronized void recordTransferOut(Money amount, AccountNumber destination, String reference) {
        requireStatus(AccountStatus.ACTIVE, "transfer out");
        if (!amount.isPositive()) {
            throw new BankingException("Transfer amount must be positive");
        }
        long maxWithdrawable = Math.addExact(balance.minorUnits(), overdraftLimit.limit().minorUnits());
        if (amount.minorUnits() > maxWithdrawable) {
            throw new BankingException("Insufficient funds for transfer: " + amount);
        }
        balance = balance.minus(amount);
        ledgerEntries.add(new LedgerEntry(TransactionType.TRANSFER_OUT, amount, balance, 
            "Transfer to " + destination.value() + " [Ref: " + reference + "]"));
    }

    public synchronized void freeze(String reason) {
        if (status == AccountStatus.CLOSED) {
            throw new BankingException("Cannot freeze a closed account");
        }
        this.status = AccountStatus.FROZEN;
    }

    public synchronized void unfreeze() {
        if (status != AccountStatus.FROZEN) {
            throw new BankingException("Account is not frozen");
        }
        this.status = AccountStatus.ACTIVE;
    }

    public synchronized void close() {
        if (balance.minorUnits() != 0) {
            throw new BankingException("Cannot close account with non-zero balance: " + balance);
        }
        this.status = AccountStatus.CLOSED;
    }

    public synchronized void setOverdraftLimit(OverdraftLimit newLimit) {
        requireStatus(AccountStatus.ACTIVE, "update overdraft limit");
        this.overdraftLimit = Objects.requireNonNull(newLimit);
    }

    private void requireStatus(AccountStatus expected, String operation) {
        if (this.status != expected) {
            throw new BankingException("Cannot " + operation + ": account " + accountNumber.value() + 
                " is currently " + this.status);
        }
    }

    public AccountNumber accountNumber() { return accountNumber; }
    public String currency() { return currency; }
    public synchronized Money balance() { return balance; }
    public synchronized OverdraftLimit overdraftLimit() { return overdraftLimit; }
    public synchronized AccountStatus status() { return status; }
    public synchronized List<LedgerEntry> ledgerHistory() { return List.copyOf(ledgerEntries); }

    public synchronized boolean verifyBalanceIntegrity() {
        long computed = 0;
        for (LedgerEntry entry : ledgerEntries) {
            switch (entry.type()) {
                case DEPOSIT, TRANSFER_IN -> computed += entry.amount().minorUnits();
                case WITHDRAWAL, TRANSFER_OUT -> computed -= entry.amount().minorUnits();
            }
        }
        return computed == balance.minorUnits();
    }
}

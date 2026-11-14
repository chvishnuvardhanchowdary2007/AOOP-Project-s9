package com.klu.casestudy3;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   CASE STUDY 3: Bank Account & Money Transfers   ");
        System.out.println("==================================================");

        // 1. Create Accounts
        BankAccount acc1 = new BankAccount(
            new AccountNumber("1234567890"),
            "INR",
            new OverdraftLimit(Money.of("1000.00", "INR"))
        );

        BankAccount acc2 = new BankAccount(
            new AccountNumber("9876543210"),
            "INR",
            OverdraftLimit.zero("INR")
        );

        System.out.println("Initial Account 1: " + acc1.accountNumber().value() + " | Balance: " + acc1.balance());
        System.out.println("Initial Account 2: " + acc2.accountNumber().value() + " | Balance: " + acc2.balance());

        // 2. Deposit
        acc1.deposit(Money.of("10000.00", "INR"), "Salary Credit");
        System.out.println("\nAfter 10,000 INR Deposit, Acc 1 Balance: " + acc1.balance());

        // 3. Transfer
        FundTransferService transferService = new FundTransferService();
        System.out.println("Transferring 5,000 INR from Acc 1 to Acc 2...");
        transferService.transfer(acc1, acc2, Money.of("5000.00", "INR"));

        System.out.println("Acc 1 Balance: " + acc1.balance());
        System.out.println("Acc 2 Balance: " + acc2.balance());

        // 4. Overdraft test
        System.out.println("\nTesting Overdraft limit on Acc 1 (Balance: 5,000, Overdraft: 1,000 -> Max: 6,000):");
        acc1.withdraw(Money.of("5800.00", "INR"), "Equipment Purchase");
        System.out.println("After 5,800 INR withdrawal, Acc 1 Balance: " + acc1.balance() + " (In Overdraft)");

        // 5. Test Invariant Guards
        System.out.println("\n--- Testing Invariant Guards ---");

        // (a) Exceeding overdraft
        try {
            System.out.print("Attempting to withdraw 500 INR more (exceeding overdraft): ");
            acc1.withdraw(Money.of("500.00", "INR"), "ATM Cash");
        } catch (BankingException e) {
            System.out.println("BLOCKED -> " + e.getMessage());
        }

        // (b) Negative or zero amount
        try {
            System.out.print("Attempting to deposit -500 INR: ");
            acc2.deposit(Money.of("-500.00", "INR"), "Fraudulent Deposit");
        } catch (BankingException e) {
            System.out.println("BLOCKED -> " + e.getMessage());
        }

        // (c) Frozen account withdrawal
        System.out.println("\nFreezing Acc 2...");
        acc2.freeze("Suspicious international activity");
        try {
            System.out.print("Attempting to withdraw from frozen Acc 2: ");
            acc2.withdraw(Money.of("1000.00", "INR"), "Unauthorized withdrawal");
        } catch (BankingException e) {
            System.out.println("BLOCKED -> " + e.getMessage());
        }

        // 6. Verify Ledger Integrity
        System.out.println("\n--- Ledger Integrity Verification ---");
        System.out.println("Account 1 Balance Integrity Verified: " + acc1.verifyBalanceIntegrity());
        System.out.println("Account 2 Balance Integrity Verified: " + acc2.verifyBalanceIntegrity());

        System.out.println("\nAccount 1 Ledger Entries:");
        acc1.ledgerHistory().forEach(entry -> System.out.println(" " + entry));

        System.out.println("==================================================");
        System.out.println("   Case Study 3 Invariants Fully Enforced!        ");
        System.out.println("==================================================");
    }
}

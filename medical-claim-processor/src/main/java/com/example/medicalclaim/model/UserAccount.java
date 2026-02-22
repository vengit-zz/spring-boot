package com.example.medicalclaim.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory FSA/HSA account for a user.
 */
public class UserAccount {

    private final String userId;
    private final String name;
    private double balance;
    private final List<Transaction> transactions = new ArrayList<>();

    public UserAccount(String userId, String name, double initialBalance) {
        this.userId = userId;
        this.name = name;
        this.balance = initialBalance;
    }

    public synchronized void credit(double amount, String description, String claimId) {
        this.balance += amount;
        transactions.add(new Transaction(claimId, amount, description, balance, LocalDateTime.now()));
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return List.copyOf(transactions); }

    // -------------------------------------------------------------------------
    // Nested transaction record
    // -------------------------------------------------------------------------

    public static class Transaction {
        private final String claimId;
        private final double amount;
        private final String description;
        private final double balanceAfter;
        private final LocalDateTime timestamp;

        public Transaction(String claimId, double amount, String description,
                           double balanceAfter, LocalDateTime timestamp) {
            this.claimId = claimId;
            this.amount = amount;
            this.description = description;
            this.balanceAfter = balanceAfter;
            this.timestamp = timestamp;
        }

        public String getClaimId() { return claimId; }
        public double getAmount() { return amount; }
        public String getDescription() { return description; }
        public double getBalanceAfter() { return balanceAfter; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}

package com.example.medicalclaim.service;

import com.example.medicalclaim.model.UserAccount;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe, in-memory FSA/HSA account store.
 *
 * <p>Pre-seeds three demo accounts on startup. In a production system this
 * would delegate to a persistent database.
 */
@Service
public class AccountService {

    private final Map<String, UserAccount> accounts = new ConcurrentHashMap<>();

    public AccountService() {
        // Seed demo accounts
        accounts.put("user001", new UserAccount("user001", "Alice Johnson", 1_500.00));
        accounts.put("user002", new UserAccount("user002", "Bob Martinez",  2_000.00));
        accounts.put("user003", new UserAccount("user003", "Carol Williams", 750.00));
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public Optional<UserAccount> findById(String userId) {
        return Optional.ofNullable(accounts.get(userId));
    }

    public boolean exists(String userId) {
        return accounts.containsKey(userId);
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Creates a new account with the given initial balance.
     *
     * @throws IllegalArgumentException if userId already exists
     */
    public UserAccount createAccount(String userId, String name, double initialBalance) {
        if (accounts.containsKey(userId)) {
            throw new IllegalArgumentException("Account already exists for userId: " + userId);
        }
        UserAccount account = new UserAccount(userId, name, initialBalance);
        accounts.put(userId, account);
        return account;
    }

    /**
     * Credits (adds funds) to the specified account.
     *
     * @return updated account
     * @throws IllegalArgumentException if account not found
     */
    public UserAccount credit(String userId, double amount, String description, String claimId) {
        UserAccount account = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + userId));
        account.credit(amount, description, claimId);
        return account;
    }
}

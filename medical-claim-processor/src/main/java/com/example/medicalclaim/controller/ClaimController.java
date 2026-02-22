package com.example.medicalclaim.controller;

import com.example.medicalclaim.model.ClaimRequest;
import com.example.medicalclaim.model.ClaimResult;
import com.example.medicalclaim.model.UserAccount;
import com.example.medicalclaim.service.AccountService;
import com.example.medicalclaim.service.ClaimProcessorAgent;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * REST API for the medical FSA/HSA claim processor.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST /api/claims}           – Submit a claim for AI processing</li>
 *   <li>{@code GET  /api/accounts/{userId}} – Retrieve account balance & history</li>
 *   <li>{@code POST /api/accounts}          – Create a new demo account</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class ClaimController {

    private final ClaimProcessorAgent agent;
    private final AccountService accountService;

    public ClaimController(ClaimProcessorAgent agent, AccountService accountService) {
        this.agent = agent;
        this.accountService = accountService;
    }

    // =========================================================================
    // Claims
    // =========================================================================

    /**
     * Submit a medical claim for AI-powered processing.
     *
     * <p>The agent validates the receipt, checks FSA/HSA eligibility, calculates
     * the approved reimbursement, and credits the account — all autonomously.
     *
     * <p>Example request body:
     * <pre>{@code
     * {
     *   "userId": "user001",
     *   "claimedAmount": 150.00,
     *   "description": "Annual physical exam",
     *   "providerName": "Dr. Jane Smith MD",
     *   "serviceDate": "2026-02-20",
     *   "serviceType": "DOCTOR_VISIT",
     *   "receiptText": "Provider: Dr. Jane Smith MD\nDate: 02/20/2026\nService: Annual Physical Exam\nAmount: $150.00"
     * }
     * }</pre>
     */
    @PostMapping("/claims")
    public ResponseEntity<ClaimResult> submitClaim(@Valid @RequestBody ClaimRequest request) {
        validateClaimInput(request);

        String claimId = "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ClaimResult result = agent.processClaim(request, claimId);
        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Accounts
    // =========================================================================

    /**
     * Returns the account balance and transaction history for a user.
     */
    @GetMapping("/accounts/{userId}")
    public ResponseEntity<UserAccount> getAccount(@PathVariable String userId) {
        return accountService.findById(userId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + userId));
    }

    /**
     * Creates a new FSA/HSA account (for demo / testing purposes).
     *
     * <p>Example request body:
     * <pre>{@code
     * { "userId": "user999", "name": "Test User", "initialBalance": 500.00 }
     * }</pre>
     */
    @PostMapping("/accounts")
    public ResponseEntity<UserAccount> createAccount(
            @RequestBody Map<String, Object> body) {

        String userId = getString(body, "userId");
        String name   = getString(body, "name");
        double balance = getDouble(body, "initialBalance", 0.0);

        if (accountService.exists(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Account already exists: " + userId);
        }

        UserAccount account = accountService.createAccount(userId, name, balance);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void validateClaimInput(ClaimRequest request) {
        if (request.getReceiptImageBase64() == null
                && (request.getReceiptText() == null || request.getReceiptText().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one of 'receiptImageBase64' or 'receiptText' must be provided.");
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing required field: " + key);
        }
        return val.toString();
    }

    private double getDouble(Map<String, Object> map, String key, double defaultVal) {
        Object val = map.get(key);
        if (val == null) return defaultVal;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid numeric value for field: " + key);
        }
    }
}

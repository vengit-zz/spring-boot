package com.example.medicalclaim.model;

import java.time.LocalDateTime;

/**
 * Result returned after a claim has been processed by the AI agent.
 */
public class ClaimResult {

    public enum Status {
        /** Full claimed amount is approved and credited. */
        APPROVED,
        /** Only part of the claimed amount is eligible. */
        PARTIAL,
        /** Claim is ineligible and no amount is credited. */
        DENIED
    }

    private String claimId;
    private String userId;
    private Status status;
    private double claimedAmount;
    private double approvedAmount;
    private double newBalance;

    /** Human-readable explanation from the AI agent. */
    private String explanation;

    private LocalDateTime processedAt;

    // -------------------------------------------------------------------------
    // Factory helper
    // -------------------------------------------------------------------------

    public static ClaimResult denied(String claimId, String userId,
                                     double claimedAmount, double currentBalance,
                                     String explanation) {
        ClaimResult r = new ClaimResult();
        r.claimId = claimId;
        r.userId = userId;
        r.status = Status.DENIED;
        r.claimedAmount = claimedAmount;
        r.approvedAmount = 0.0;
        r.newBalance = currentBalance;
        r.explanation = explanation;
        r.processedAt = LocalDateTime.now();
        return r;
    }

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getClaimedAmount() { return claimedAmount; }
    public void setClaimedAmount(double claimedAmount) { this.claimedAmount = claimedAmount; }

    public double getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(double approvedAmount) { this.approvedAmount = approvedAmount; }

    public double getNewBalance() { return newBalance; }
    public void setNewBalance(double newBalance) { this.newBalance = newBalance; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}

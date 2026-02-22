package com.example.medicalclaim.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Incoming claim submission from a user.
 *
 * <p>The receipt can be provided either as a base64-encoded image string
 * (receiptImageBase64) or as a plain-text description (receiptText).
 * At least one of the two must be supplied.
 */
public class ClaimRequest {

    /** The authenticated user submitting the claim. */
    @NotBlank(message = "userId is required")
    private String userId;

    /** Amount the user is claiming for reimbursement (in USD). */
    @NotNull(message = "claimedAmount is required")
    @Positive(message = "claimedAmount must be positive")
    private Double claimedAmount;

    /** Human-readable description of the medical service. */
    @NotBlank(message = "description is required")
    private String description;

    /** Name of the healthcare provider (e.g. "Dr. Jane Smith MD"). */
    @NotBlank(message = "providerName is required")
    private String providerName;

    /**
     * Date of service in ISO-8601 format (e.g. "2026-02-20").
     */
    @NotBlank(message = "serviceDate is required")
    private String serviceDate;

    /**
     * Category of medical service.
     * Common values: DOCTOR_VISIT, PRESCRIPTION, DENTAL, VISION,
     * MENTAL_HEALTH, PHYSICAL_THERAPY, LAB_TEST, HOSPITAL, OTHER.
     */
    @NotBlank(message = "serviceType is required")
    private String serviceType;

    /**
     * Optional base64-encoded receipt image (JPEG, PNG, GIF, or WebP).
     * When present Claude will perform visual analysis of the receipt.
     */
    private String receiptImageBase64;

    /**
     * Optional plain-text description of the receipt contents.
     * Used when no image is available.
     */
    private String receiptText;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ClaimRequest() {}

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getClaimedAmount() { return claimedAmount; }
    public void setClaimedAmount(Double claimedAmount) { this.claimedAmount = claimedAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getServiceDate() { return serviceDate; }
    public void setServiceDate(String serviceDate) { this.serviceDate = serviceDate; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getReceiptImageBase64() { return receiptImageBase64; }
    public void setReceiptImageBase64(String receiptImageBase64) {
        this.receiptImageBase64 = receiptImageBase64;
    }

    public String getReceiptText() { return receiptText; }
    public void setReceiptText(String receiptText) { this.receiptText = receiptText; }
}

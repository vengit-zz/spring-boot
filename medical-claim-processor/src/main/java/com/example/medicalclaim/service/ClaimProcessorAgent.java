package com.example.medicalclaim.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.example.medicalclaim.model.ClaimRequest;
import com.example.medicalclaim.model.ClaimResult;
import com.example.medicalclaim.model.UserAccount;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI-powered medical claim processing agent.
 *
 * <p>Uses Claude Opus 4.6 with a manual agentic tool-use loop to:
 * <ol>
 *   <li>Validate the submitted receipt (visual or text)</li>
 *   <li>Check FSA/HSA eligibility of the medical service</li>
 *   <li>Calculate the approved reimbursement amount</li>
 *   <li>Credit the user's account</li>
 * </ol>
 *
 * <p>All four steps are exposed as tools that Claude autonomously decides
 * when and how to call, mirroring a real claims-adjudication workflow.
 */
@Service
public class ClaimProcessorAgent {

    private static final Logger log = LoggerFactory.getLogger(ClaimProcessorAgent.class);

    private static final String MODEL = "claude-opus-4-6";
    private static final long MAX_TOKENS = 8_096L;

    private final AnthropicClient anthropic;
    private final AccountService accountService;
    private final ObjectMapper objectMapper;

    public ClaimProcessorAgent(AnthropicClient anthropic,
                               AccountService accountService,
                               ObjectMapper objectMapper) {
        this.anthropic = anthropic;
        this.accountService = accountService;
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Processes a medical FSA/HSA claim end-to-end.
     *
     * @param request  validated claim submitted by the user
     * @param claimId  unique identifier pre-assigned to this claim
     * @return         result with status, approved amount, and new balance
     */
    public ClaimResult processClaim(ClaimRequest request, String claimId) {
        log.info("Processing claim {} for user {}", claimId, request.getUserId());

        // Verify account exists before invoking Claude
        Optional<UserAccount> accountOpt = accountService.findById(request.getUserId());
        if (accountOpt.isEmpty()) {
            return ClaimResult.denied(claimId, request.getUserId(),
                    request.getClaimedAmount(), 0.0,
                    "Account not found for userId: " + request.getUserId());
        }

        try {
            return runAgenticLoop(request, claimId, accountOpt.get());
        } catch (Exception e) {
            log.error("Claim processing failed for claim {}", claimId, e);
            return ClaimResult.denied(claimId, request.getUserId(),
                    request.getClaimedAmount(), accountOpt.get().getBalance(),
                    "Internal processing error: " + e.getMessage());
        }
    }

    // =========================================================================
    // Agentic loop
    // =========================================================================

    private ClaimResult runAgenticLoop(ClaimRequest request,
                                       String claimId,
                                       UserAccount account) throws Exception {

        List<Tool> tools = buildTools();
        List<MessageParam> messages = buildInitialMessages(request, claimId, account.getBalance());
        String systemPrompt = buildSystemPrompt();

        // State shared across tool calls during this claim
        ClaimContext ctx = new ClaimContext(claimId, request, account.getBalance());

        int maxIterations = 10;
        for (int iteration = 0; iteration < maxIterations; iteration++) {

            MessageCreateParams params = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(MAX_TOKENS)
                    .system(systemPrompt)
                    .tools(tools)
                    .messages(messages)
                    .build();

            Message response = anthropic.messages().create(params);
            log.debug("Iteration {}: stopReason={}", iteration, response.stopReason());

            // Append the assistant turn to the conversation
            messages.add(toAssistantParam(response));

            if (response.stopReason() == StopReason.END_TURN) {
                // Claude finished without more tool calls — build result from context
                String finalText = extractFinalText(response);
                return ctx.buildResult(finalText);
            }

            if (response.stopReason() != StopReason.TOOL_USE) {
                log.warn("Unexpected stop reason: {}", response.stopReason());
                break;
            }

            // Execute all tool calls in this turn and collect results
            List<ContentBlockParam> toolResults = new ArrayList<>();
            for (ContentBlock block : response.content()) {
                block.toolUse().ifPresent(toolUse -> {
                    String result = executeTool(toolUse.name(), toolUse.input(), ctx);
                    log.info("Tool {} executed: {}", toolUse.name(), result);

                    toolResults.add(ContentBlockParam.ofToolResult(
                            ToolResultBlockParam.builder()
                                    .toolUseId(toolUse.id())
                                    .content(ToolResultBlockParam.Content.ofText(result))
                                    .build()));
                });
            }

            if (toolResults.isEmpty()) {
                break; // nothing to feed back
            }

            messages.add(MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .content(MessageParam.Content.ofArray(toolResults))
                    .build());
        }

        return ctx.buildResult("Claim processing completed.");
    }

    // =========================================================================
    // Message construction
    // =========================================================================

    private List<MessageParam> buildInitialMessages(ClaimRequest request,
                                                    String claimId,
                                                    double currentBalance) {
        List<ContentBlockParam> blocks = new ArrayList<>();

        // Optionally include the receipt image for visual analysis
        if (request.getReceiptImageBase64() != null
                && !request.getReceiptImageBase64().isBlank()) {
            blocks.add(ContentBlockParam.ofImage(
                    ImageBlockParam.builder()
                            .source(ImageBlockParam.Source.ofBase64(
                                    Base64ImageSource.builder()
                                            .data(request.getReceiptImageBase64())
                                            .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                            .build()))
                            .build()));
        }

        // Main claim details as text
        String claimSummary = buildClaimSummary(request, claimId, currentBalance);
        blocks.add(ContentBlockParam.ofText(
                TextBlockParam.builder().text(claimSummary).build()));

        return List.of(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(MessageParam.Content.ofArray(blocks))
                .build());
    }

    private String buildClaimSummary(ClaimRequest request, String claimId, double balance) {
        StringBuilder sb = new StringBuilder();
        sb.append("Please process the following medical FSA/HSA claim.\n\n");
        sb.append("CLAIM ID: ").append(claimId).append("\n");
        sb.append("USER ID: ").append(request.getUserId()).append("\n");
        sb.append("CURRENT ACCOUNT BALANCE: $").append(String.format("%.2f", balance)).append("\n\n");
        sb.append("CLAIM DETAILS:\n");
        sb.append("- Provider: ").append(request.getProviderName()).append("\n");
        sb.append("- Service Type: ").append(request.getServiceType()).append("\n");
        sb.append("- Service Date: ").append(request.getServiceDate()).append("\n");
        sb.append("- Description: ").append(request.getDescription()).append("\n");
        sb.append("- Claimed Amount: $").append(String.format("%.2f", request.getClaimedAmount())).append("\n");

        if (request.getReceiptText() != null && !request.getReceiptText().isBlank()) {
            sb.append("\nRECEIPT TEXT:\n").append(request.getReceiptText()).append("\n");
        }
        if (request.getReceiptImageBase64() != null && !request.getReceiptImageBase64().isBlank()) {
            sb.append("\n(A receipt image has been attached above for visual verification.)\n");
        }

        sb.append("\nPlease use the available tools to: validate the receipt, check FSA/HSA ")
          .append("eligibility, calculate the approved amount, and credit the account if approved.");

        return sb.toString();
    }

    private String buildSystemPrompt() {
        return """
                You are a precise and fair medical FSA/HSA claims processing agent.

                Your responsibilities:
                1. VALIDATE the receipt — verify provider, date, and amount are legitimate.
                2. CHECK ELIGIBILITY — determine whether the medical service qualifies under
                   IRS FSA/HSA eligible expense rules (Publication 502).
                3. CALCULATE REIMBURSEMENT — determine the exact approved amount (0–100% of claimed).
                4. CREDIT ACCOUNT — if approved (even partially), credit the user's account.

                FSA/HSA eligibility rules (non-exhaustive):
                - Eligible: doctor visits, prescriptions, dental care, vision care, lab tests,
                  mental health therapy, physical therapy, hospital services, medical equipment.
                - Not eligible: cosmetic procedures, gym memberships, general wellness supplements,
                  teeth whitening, hair restoration, over-the-counter non-prescription items (most).

                Always use ALL four tools in sequence. Never skip steps.
                Be conservative but fair: when in doubt, allow partial reimbursement with an explanation.
                Provide a clear, patient-friendly explanation of the final decision.
                """;
    }

    // =========================================================================
    // Tool definitions
    // =========================================================================

    private List<Tool> buildTools() {
        return List.of(
                buildValidateReceiptTool(),
                buildCheckEligibilityTool(),
                buildCalculateReimbursementTool(),
                buildCreditAccountTool()
        );
    }

    private Tool buildValidateReceiptTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("provider_name", Map.of(
                "type", "string",
                "description", "Name of the medical provider as shown on the receipt"));
        properties.put("service_date", Map.of(
                "type", "string",
                "description", "Date of service (ISO-8601 format, e.g. 2026-02-20)"));
        properties.put("amount", Map.of(
                "type", "number",
                "description", "Total amount charged as shown on the receipt (USD)"));
        properties.put("service_type", Map.of(
                "type", "string",
                "description", "Category of medical service (e.g. DOCTOR_VISIT, PRESCRIPTION, DENTAL)"));
        properties.put("notes", Map.of(
                "type", "string",
                "description", "Any additional observations from receipt analysis"));

        return buildTool(
                "validate_receipt",
                "Validates the medical receipt. Checks that the provider name, service date, "
                        + "and amount are present and internally consistent. Returns a JSON object "
                        + "with fields: valid (boolean), issues (list of strings).",
                properties,
                List.of("provider_name", "service_date", "amount", "service_type"));
    }

    private Tool buildCheckEligibilityTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("service_type", Map.of(
                "type", "string",
                "description", "Category of medical service"));
        properties.put("description", Map.of(
                "type", "string",
                "description", "Detailed description of the service provided"));
        properties.put("eligibility_percentage", Map.of(
                "type", "number",
                "description", "Estimated percentage of the service that is FSA/HSA eligible (0–100)"));

        return buildTool(
                "check_fsa_eligibility",
                "Determines whether the medical service qualifies under IRS Publication 502 "
                        + "FSA/HSA eligible expense rules. Returns a JSON object with fields: "
                        + "eligible (boolean), eligibility_percentage (0–100), reason (string).",
                properties,
                List.of("service_type", "description", "eligibility_percentage"));
    }

    private Tool buildCalculateReimbursementTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("claimed_amount", Map.of(
                "type", "number",
                "description", "The amount originally claimed by the user (USD)"));
        properties.put("eligibility_percentage", Map.of(
                "type", "number",
                "description", "Percentage of the expense that is FSA/HSA eligible (0–100)"));
        properties.put("approved_amount", Map.of(
                "type", "number",
                "description", "Final approved reimbursement amount in USD"));
        properties.put("notes", Map.of(
                "type", "string",
                "description", "Explanation for any reduction from the claimed amount"));

        return buildTool(
                "calculate_reimbursement",
                "Calculates the approved reimbursement amount based on eligibility. "
                        + "Returns a JSON object with fields: approved_amount (number), notes (string).",
                properties,
                List.of("claimed_amount", "eligibility_percentage", "approved_amount"));
    }

    private Tool buildCreditAccountTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("user_id", Map.of(
                "type", "string",
                "description", "The account holder's user ID"));
        properties.put("amount", Map.of(
                "type", "number",
                "description", "Amount to credit to the account (USD)"));
        properties.put("claim_id", Map.of(
                "type", "string",
                "description", "Unique identifier for this claim"));
        properties.put("description", Map.of(
                "type", "string",
                "description", "Short credit memo description (e.g. 'Approved: Doctor visit 2026-02-20')"));

        return buildTool(
                "credit_user_account",
                "Credits the user's FSA/HSA account with the approved reimbursement amount. "
                        + "Returns a JSON object with fields: success (boolean), new_balance (number), "
                        + "transaction_id (string).",
                properties,
                List.of("user_id", "amount", "claim_id", "description"));
    }

    private Tool buildTool(String name, String description,
                           Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);

        return Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(Tool.InputSchema.builder()
                        .type(Tool.InputSchema.Type.OBJECT)
                        .properties(JsonValue.from(properties))
                        .putAdditionalProperty("required", JsonValue.from(required))
                        .build())
                .build();
    }

    // =========================================================================
    // Tool execution dispatch
    // =========================================================================

    private String executeTool(String toolName, JsonValue input, ClaimContext ctx) {
        try {
            Map<String, Object> args = objectMapper.readValue(
                    input.toString(), new TypeReference<Map<String, Object>>() {});

            return switch (toolName) {
                case "validate_receipt"       -> executeValidateReceipt(args, ctx);
                case "check_fsa_eligibility"  -> executeCheckEligibility(args, ctx);
                case "calculate_reimbursement" -> executeCalculateReimbursement(args, ctx);
                case "credit_user_account"    -> executeCreditAccount(args, ctx);
                default -> "{\"error\": \"Unknown tool: " + toolName + "\"}";
            };
        } catch (Exception e) {
            log.error("Tool execution failed for {}", toolName, e);
            return "{\"error\": \"Tool execution failed: " + e.getMessage() + "\"}";
        }
    }

    // -------------------------------------------------------------------------
    // validate_receipt
    // -------------------------------------------------------------------------

    private String executeValidateReceipt(Map<String, Object> args, ClaimContext ctx)
            throws Exception {

        String providerName = (String) args.getOrDefault("provider_name", "");
        String serviceDate  = (String) args.getOrDefault("service_date", "");
        Number amount       = (Number) args.getOrDefault("amount", 0);
        String serviceType  = (String) args.getOrDefault("service_type", "");

        List<String> issues = new ArrayList<>();

        if (providerName.isBlank())  issues.add("Provider name is missing");
        if (serviceDate.isBlank())   issues.add("Service date is missing");
        if (amount.doubleValue() <= 0) issues.add("Amount must be greater than zero");
        if (serviceType.isBlank())   issues.add("Service type is missing");

        // Cross-check amount vs claimed amount (flag large discrepancies)
        double claimed = ctx.getRequest().getClaimedAmount();
        double receiptAmount = amount.doubleValue();
        if (receiptAmount > 0 && Math.abs(receiptAmount - claimed) > claimed * 0.10) {
            issues.add(String.format(
                    "Receipt amount $%.2f differs from claimed amount $%.2f by more than 10%%",
                    receiptAmount, claimed));
        }

        boolean valid = issues.isEmpty();
        ctx.setReceiptValid(valid);
        ctx.setValidatedAmount(receiptAmount);

        Map<String, Object> result = Map.of(
                "valid", valid,
                "issues", issues,
                "validated_amount", receiptAmount);

        return objectMapper.writeValueAsString(result);
    }

    // -------------------------------------------------------------------------
    // check_fsa_eligibility
    // -------------------------------------------------------------------------

    private String executeCheckEligibility(Map<String, Object> args, ClaimContext ctx)
            throws Exception {

        String serviceType  = (String) args.getOrDefault("service_type", "");
        String description  = (String) args.getOrDefault("description", "");
        Number eligPctInput = (Number) args.getOrDefault("eligibility_percentage", 0);

        // Authoritative eligibility rules (IRS Pub. 502)
        EligibilityResult elig = determineEligibility(serviceType, description);

        // If Claude already estimated a percentage, keep it if it's more restrictive
        double finalPct = Math.min(elig.percentage, eligPctInput.doubleValue() > 0
                ? eligPctInput.doubleValue() : 100.0);

        ctx.setEligibilityPercentage(finalPct);
        ctx.setEligible(elig.eligible);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eligible", elig.eligible);
        result.put("eligibility_percentage", finalPct);
        result.put("reason", elig.reason);
        result.put("irs_reference", "IRS Publication 502");

        return objectMapper.writeValueAsString(result);
    }

    private EligibilityResult determineEligibility(String serviceType, String description) {
        String type = serviceType.toUpperCase(Locale.ROOT);
        String desc = description.toUpperCase(Locale.ROOT);

        // Fully eligible categories
        if (type.contains("DOCTOR") || type.contains("PHYSICIAN")
                || type.contains("MEDICAL_VISIT") || type.contains("OFFICE_VISIT")) {
            return new EligibilityResult(true, 100.0,
                    "Doctor/physician visits are fully FSA/HSA eligible under IRS Pub. 502.");
        }
        if (type.contains("PRESCRIPTION") || type.contains("PHARMACY")) {
            return new EligibilityResult(true, 100.0,
                    "Prescription medications are fully FSA/HSA eligible.");
        }
        if (type.contains("DENTAL")) {
            boolean cosmetic = desc.contains("WHITENING") || desc.contains("COSMETIC")
                    || desc.contains("BLEACH");
            if (cosmetic) {
                return new EligibilityResult(false, 0.0,
                        "Cosmetic dental procedures (whitening, etc.) are NOT eligible.");
            }
            return new EligibilityResult(true, 100.0,
                    "Dental care (non-cosmetic) is fully FSA/HSA eligible.");
        }
        if (type.contains("VISION") || type.contains("EYE") || type.contains("OPTICAL")) {
            return new EligibilityResult(true, 100.0,
                    "Vision care (exams, glasses, contacts) is fully FSA/HSA eligible.");
        }
        if (type.contains("MENTAL_HEALTH") || type.contains("THERAPY")
                || type.contains("PSYCHIATRY") || type.contains("PSYCHOLOGY")) {
            return new EligibilityResult(true, 100.0,
                    "Mental health treatment is fully FSA/HSA eligible.");
        }
        if (type.contains("PHYSICAL_THERAPY") || type.contains("OCCUPATIONAL_THERAPY")) {
            return new EligibilityResult(true, 100.0,
                    "Physical and occupational therapy prescribed by a physician is fully eligible.");
        }
        if (type.contains("LAB") || type.contains("DIAGNOSTIC")
                || type.contains("IMAGING") || type.contains("XRAY")) {
            return new EligibilityResult(true, 100.0,
                    "Laboratory tests and diagnostic imaging are fully FSA/HSA eligible.");
        }
        if (type.contains("HOSPITAL") || type.contains("SURGERY")
                || type.contains("EMERGENCY")) {
            return new EligibilityResult(true, 100.0,
                    "Hospital and surgical expenses are fully FSA/HSA eligible.");
        }

        // Partially or conditionally eligible
        if (type.contains("CHIROPRACTIC") || type.contains("ACUPUNCTURE")) {
            return new EligibilityResult(true, 100.0,
                    "Chiropractic and acupuncture services are FSA/HSA eligible when medically necessary.");
        }
        if (type.contains("NUTRITION") || type.contains("DIETITIAN")) {
            boolean medicallyNecessary = desc.contains("DIABETES") || desc.contains("OBESITY")
                    || desc.contains("PRESCRIBED") || desc.contains("MEDICAL");
            if (medicallyNecessary) {
                return new EligibilityResult(true, 100.0,
                        "Nutrition counseling is eligible when medically necessary (e.g., for diabetes).");
            }
            return new EligibilityResult(false, 0.0,
                    "General nutrition counseling is not FSA/HSA eligible without medical necessity.");
        }
        if (type.contains("WELLNESS") || type.contains("GYM") || type.contains("FITNESS")) {
            return new EligibilityResult(false, 0.0,
                    "Gym memberships and general wellness programs are NOT FSA/HSA eligible.");
        }
        if (type.contains("COSMETIC") || type.contains("AESTHETIC")) {
            return new EligibilityResult(false, 0.0,
                    "Cosmetic procedures are NOT FSA/HSA eligible.");
        }

        // Default: treat as eligible but flag for review
        return new EligibilityResult(true, 80.0,
                "Service type not in predefined list; provisionally approved at 80% pending manual review.");
    }

    private record EligibilityResult(boolean eligible, double percentage, String reason) {}

    // -------------------------------------------------------------------------
    // calculate_reimbursement
    // -------------------------------------------------------------------------

    private String executeCalculateReimbursement(Map<String, Object> args, ClaimContext ctx)
            throws Exception {

        Number claimedAmount   = (Number) args.getOrDefault("claimed_amount", ctx.getRequest().getClaimedAmount());
        Number eligPct         = (Number) args.getOrDefault("eligibility_percentage", ctx.getEligibilityPercentage());
        Number approvedAmtArg  = (Number) args.getOrDefault("approved_amount", 0);
        String notes           = (String) args.getOrDefault("notes", "");

        double claimed  = claimedAmount.doubleValue();
        double pct      = eligPct.doubleValue();

        // Use Claude's explicit approved_amount if provided; otherwise calculate
        double approved;
        if (approvedAmtArg.doubleValue() > 0) {
            approved = Math.min(approvedAmtArg.doubleValue(), claimed);
        } else {
            approved = Math.round((claimed * pct / 100.0) * 100.0) / 100.0;
        }

        // Cap at the current account balance is NOT enforced here — FSA credits
        // come *into* the account, not from it. Keep the full approved amount.
        ctx.setApprovedAmount(approved);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("claimed_amount", claimed);
        result.put("eligibility_percentage", pct);
        result.put("approved_amount", approved);
        result.put("notes", notes.isBlank() ? "Standard calculation applied." : notes);

        return objectMapper.writeValueAsString(result);
    }

    // -------------------------------------------------------------------------
    // credit_user_account
    // -------------------------------------------------------------------------

    private String executeCreditAccount(Map<String, Object> args, ClaimContext ctx)
            throws Exception {

        String userId      = (String) args.getOrDefault("user_id", ctx.getRequest().getUserId());
        Number amountArg   = (Number) args.getOrDefault("amount", ctx.getApprovedAmount());
        String claimId     = (String) args.getOrDefault("claim_id", ctx.getClaimId());
        String description = (String) args.getOrDefault("description", "Medical claim reimbursement");

        double amount = amountArg.doubleValue();

        if (amount <= 0) {
            Map<String, Object> result = Map.of(
                    "success", false,
                    "message", "Amount must be greater than zero; no credit applied.");
            return objectMapper.writeValueAsString(result);
        }

        UserAccount updated = accountService.credit(userId, amount, description, claimId);
        ctx.setNewBalance(updated.getBalance());
        ctx.setCredited(true);

        String txId = "TX-" + claimId.toUpperCase() + "-" + System.currentTimeMillis();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("credited_amount", amount);
        result.put("new_balance", updated.getBalance());
        result.put("transaction_id", txId);
        result.put("message", String.format(
                "Successfully credited $%.2f to account %s. New balance: $%.2f",
                amount, userId, updated.getBalance()));

        return objectMapper.writeValueAsString(result);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Converts a {@link Message} response into an assistant {@link MessageParam}. */
    private MessageParam toAssistantParam(Message response) {
        List<ContentBlockParam> blocks = new ArrayList<>();

        for (ContentBlock block : response.content()) {
            block.text().ifPresent(tb ->
                    blocks.add(ContentBlockParam.ofText(
                            TextBlockParam.builder().text(tb.text()).build())));

            block.toolUse().ifPresent(tu ->
                    blocks.add(ContentBlockParam.ofToolUse(
                            ToolUseBlockParam.builder()
                                    .id(tu.id())
                                    .name(tu.name())
                                    .input(tu.input())
                                    .build())));
        }

        return MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .content(MessageParam.Content.ofArray(blocks))
                .build();
    }

    private String extractFinalText(Message response) {
        StringBuilder sb = new StringBuilder();
        response.content().forEach(block ->
                block.text().ifPresent(tb -> sb.append(tb.text()).append(" ")));
        return sb.toString().trim();
    }

    // =========================================================================
    // Claim context (mutable state shared across tool calls in one request)
    // =========================================================================

    /**
     * Mutable state bag passed to tool executors within a single claim session.
     * Avoids relying on Claude faithfully echoing back every intermediate value.
     */
    private static class ClaimContext {

        private final String claimId;
        private final ClaimRequest request;
        private final double previousBalance;

        private boolean receiptValid = false;
        private double validatedAmount = 0.0;
        private boolean eligible = false;
        private double eligibilityPercentage = 0.0;
        private double approvedAmount = 0.0;
        private double newBalance;
        private boolean credited = false;

        ClaimContext(String claimId, ClaimRequest request, double previousBalance) {
            this.claimId = claimId;
            this.request = request;
            this.previousBalance = previousBalance;
            this.newBalance = previousBalance;
        }

        ClaimResult buildResult(String explanation) {
            ClaimResult result = new ClaimResult();
            result.setClaimId(claimId);
            result.setUserId(request.getUserId());
            result.setClaimedAmount(request.getClaimedAmount());
            result.setApprovedAmount(approvedAmount);
            result.setNewBalance(newBalance);
            result.setExplanation(explanation.isBlank() ? "Claim processed." : explanation);
            result.setProcessedAt(LocalDateTime.now());

            if (!credited || approvedAmount <= 0) {
                result.setStatus(ClaimResult.Status.DENIED);
            } else if (approvedAmount >= request.getClaimedAmount() - 0.01) {
                result.setStatus(ClaimResult.Status.APPROVED);
            } else {
                result.setStatus(ClaimResult.Status.PARTIAL);
            }

            return result;
        }

        // Getters & setters
        String getClaimId()  { return claimId; }
        ClaimRequest getRequest() { return request; }
        double getEligibilityPercentage() { return eligibilityPercentage; }
        double getApprovedAmount() { return approvedAmount; }

        void setReceiptValid(boolean v)  { this.receiptValid = v; }
        void setValidatedAmount(double v){ this.validatedAmount = v; }
        void setEligible(boolean v)      { this.eligible = v; }
        void setEligibilityPercentage(double v) { this.eligibilityPercentage = v; }
        void setApprovedAmount(double v) { this.approvedAmount = v; }
        void setNewBalance(double v)     { this.newBalance = v; }
        void setCredited(boolean v)      { this.credited = v; }
    }
}

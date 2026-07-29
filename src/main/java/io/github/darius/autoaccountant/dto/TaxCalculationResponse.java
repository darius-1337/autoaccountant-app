package io.github.darius.autoaccountant.dto;

public record TaxCalculationResponse(
        boolean isValid,
        String invoiceDate,
        double originalTaxBase,
        double deductibleTaxBaseIRPF,
        double originalVat,
        double deductibleVat,
        String category,
        boolean requiresManualReview,
        String manualReviewReason,
        String aiReasoning
) {
}

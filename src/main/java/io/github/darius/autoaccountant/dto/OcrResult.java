package io.github.darius.autoaccountant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrResult(
        boolean isValidInvoice,
        String invoiceNumber,
        String invoiceDate,
        String issuerName,
        String issuerTaxId,
        String currency,
        double taxBase,
        double vatRate,
        double vatAmount,
        double totalAmount,
        String expenseCategory,
        boolean isDeductibleVAT,
        Double vatDeductiblePercentage,
        boolean isDeductibleIRPF,
        Double irpfDeductiblePercentage,
        String correlationReasoning,
        boolean requiresManualReview,
        String manualReviewReason,
        double confidenceScore
) {
}

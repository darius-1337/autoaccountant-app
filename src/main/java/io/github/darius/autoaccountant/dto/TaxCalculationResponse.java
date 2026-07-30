package io.github.darius.autoaccountant.dto;

import io.github.darius.autoaccountant.domain.ExpenseCategory;
import io.github.darius.autoaccountant.domain.ProcessedExpense;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    public boolean canBeAssignedToQuarter() {
        return isValid && invoiceDate != null && !invoiceDate.isBlank();
    }

    public ProcessedExpense toDomain() {
        if (!canBeAssignedToQuarter()) {
            throw new IllegalStateException("Factura con fecha no valida o illegible, no se puede calclar el trimestre");
        }

        return new ProcessedExpense(
                LocalDate.parse(invoiceDate),
                BigDecimal.valueOf(originalTaxBase),
                BigDecimal.valueOf(originalVat),
                BigDecimal.valueOf(deductibleVat),
                BigDecimal.valueOf(deductibleTaxBaseIRPF),
                ExpenseCategory.fromText(category),
                requiresManualReview
        );
    }
}

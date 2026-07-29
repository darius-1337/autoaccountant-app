package io.github.darius.autoaccountant.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProcessedExpense(
        LocalDate date,
        BigDecimal taxBase,
        BigDecimal vatAmount,
        BigDecimal deductibleVat,
        BigDecimal deductibleTaxBase,
        ExpenseCategory category,
        boolean requiresManualReview
) {
    public BigDecimal grossAmount() {
        return taxBase.add(vatAmount);
    }

    public FiscalQuarter quarter() {
        return FiscalQuarter.of(date);
    }

    public boolean countsTowardsQuarter() {
        return !requiresManualReview;
    }
}

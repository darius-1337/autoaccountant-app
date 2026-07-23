package io.github.darius.autoaccountant.dto;

public record TaxCalculationResponse(
        double originalTaxBase,
        double deductibleTaxBase,
        double originalVat,
        double deductibleVat,
        String message
) {
}

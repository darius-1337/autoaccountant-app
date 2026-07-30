package io.github.darius.autoaccountant.dto;

import io.github.darius.autoaccountant.domain.FiscalQuarter;

import java.util.List;

public record QuarterlyRequest(
        TaxPayerProfileRequest profile,
        FiscalQuarter quarter,
        int year,
        List<IncomeRequest> income,
        List<TaxCalculationResponse> expenses
) {
}

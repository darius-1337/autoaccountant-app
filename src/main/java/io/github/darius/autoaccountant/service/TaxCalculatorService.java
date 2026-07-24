package io.github.darius.autoaccountant.service;

import io.github.darius.autoaccountant.domain.DeductionRates;
import io.github.darius.autoaccountant.domain.ExpenseCategory;
import io.github.darius.autoaccountant.dto.OcrResult;
import io.github.darius.autoaccountant.dto.TaxCalculationResponse;
import org.springframework.stereotype.Service;

@Service
public class TaxCalculatorService {
    public TaxCalculationResponse processExpense(OcrResult aiResult, String sector) {

        if (!aiResult.isValidInvoice()) {
            return buildInvalidResponse(aiResult);
        }

        boolean isVehicleIntensive = "TRANSPORT".equalsIgnoreCase(sector);

        ExpenseCategory category = ExpenseCategory.fromText(aiResult.expenseCategory());
        DeductionRates domainRates = category.calculateRates(isVehicleIntensive);

        double appliedVatPercentage = (aiResult.vatDeductiblePercentage() != null) ? aiResult.vatDeductiblePercentage() / 100.0 : 0.0;
        double appliedIrpfPercentage = (aiResult.irpfDeductiblePercentage() != null) ? aiResult.irpfDeductiblePercentage() / 100.0 : 0.0;

        boolean finalManualReview = domainRates.requiresReview() || aiResult.requiresManualReview();

        double deductableVat = aiResult.vatAmount() * appliedVatPercentage;
        double deductibleTaxBaseIRPF = aiResult.taxBase() * appliedIrpfPercentage;

        return new TaxCalculationResponse(
                true,
                aiResult.taxBase(),
                deductibleTaxBaseIRPF,
                aiResult.vatAmount(),
                deductableVat,
                category.name(),
                finalManualReview,
                aiResult.manualReviewReason(),
                aiResult.correlationReasoning()
        );
    }

    private TaxCalculationResponse buildInvalidResponse(OcrResult result) {
        return new TaxCalculationResponse(false, 0, 0, 0, 0, "INVALID", true,
                result.manualReviewReason() != null ? result.manualReviewReason() : "Invalid or illegible document",
                "");
    }
}

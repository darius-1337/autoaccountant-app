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

        // sin fechas no puede calcular un trimestre, la factura no entra y va a revision manual.
        boolean missingDate = aiResult.invoiceDate() == null || aiResult.invoiceDate().isBlank();

        // revision manual si se cumplen las condiciones de revision
        boolean requiresReview = aiResult.requiresManualReview() || domainRates.requiresReview() || missingDate;

        String reviewReason = missingDate ? "Unable to read date from invoice, please insert it manually." : aiResult.manualReviewReason();

        return new TaxCalculationResponse(
                true,
                aiResult.invoiceDate(),
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

    // response por defecto si el documento es invalido.
    private TaxCalculationResponse buildInvalidResponse(OcrResult result) {
        return new TaxCalculationResponse(false, null, 0, 0, 0, 0, "INVALID", true,
                result.manualReviewReason() != null ? result.manualReviewReason() : "Invalid or illegible document",
                "");
    }
}

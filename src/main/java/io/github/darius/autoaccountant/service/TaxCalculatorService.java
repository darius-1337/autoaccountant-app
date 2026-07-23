package io.github.darius.autoaccountant.service;

import io.github.darius.autoaccountant.domain.Expense;
import io.github.darius.autoaccountant.domain.ExpenseCategory;
import io.github.darius.autoaccountant.dto.OcrResult;
import io.github.darius.autoaccountant.dto.TaxCalculationResponse;
import org.springframework.stereotype.Service;

@Service
public class TaxCalculatorService {
    public TaxCalculationResponse processExpense(OcrResult ocr, String sector) {
        ExpenseCategory category = ExpenseCategory.fromText(ocr.category());
        Expense expense = new Expense(ocr.taxBase(), ocr.vatAmount(), category);

        return new TaxCalculationResponse(
                ocr.taxBase(),
                expense.getDeductibleTaxBase(sector),
                ocr.vatAmount(),
                expense.getDeductibleVat(sector),
                expense.getStatusMessage(sector)
        );
    }
}

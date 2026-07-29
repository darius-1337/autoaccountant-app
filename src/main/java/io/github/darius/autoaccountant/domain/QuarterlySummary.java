package io.github.darius.autoaccountant.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

public record QuarterlySummary(
        FiscalQuarter quarter,
        int year,
        BigDecimal totalBilled,
        BigDecimal taxableIncome,
        BigDecimal outputVat,
        BigDecimal deductibleInputVat,
        BigDecimal vatToPay,
        BigDecimal deductibleExpenses,
        BigDecimal retaContribution,
        BigDecimal hardJustifyExpense,
        BigDecimal netProfit,
        BigDecimal retentionsApplied,
        BigDecimal irpfPrePayment,
        BigDecimal avalibleCash,
        int excludedDocuments
) {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static BigDecimal IRPF_PAYMENT_RATE = new BigDecimal("0.20");

    private static final BigDecimal HARD_JUSTIFY_RATE = new BigDecimal("0.05");
    private static final BigDecimal HARD_JUSTIFY_YEAR_CAP = new BigDecimal("2000");

    public static QuarterlySummary calculate(List<IncomeEntry> allIncome,
                                             List<ProcessedExpense> allExpenses,
                                             TaxPayerProfile profile,
                                             FiscalQuarter quarter,
                                             int year) {
        List<IncomeEntry> income = allIncome.stream()
                .filter(entry -> quarter.contains(entry.date(), year))
                .toList();

        List<ProcessedExpense> quarterExpenses = allExpenses.stream()
                .filter(expense -> quarter.contains(expense.date(), year))
                .toList();

        List<ProcessedExpense> pendingExpenses = allExpenses.stream()
                .filter(ProcessedExpense::countsTowardsQuarter)
                .toList();

        int excludedExpenses = quarterExpenses.size() - pendingExpenses.size();

        // ingresos
        BigDecimal taxableIncome = sum(income, IncomeEntry::taxBase);
        BigDecimal outputVat = sum(income, IncomeEntry::vatAmount);
        BigDecimal totalBilled = taxableIncome.add(outputVat);
        BigDecimal retentions = income.stream()
                .map(entry -> entry.retentionAmount(profile.iaeSection(), profile.activityStartYear()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);

        // gastos
        BigDecimal deductibleInputVat = sum(pendingExpenses, ProcessedExpense::deductibleVat);
        BigDecimal deductibleExpenseBase = sum(pendingExpenses, ProcessedExpense::deductibleTaxBase);
        BigDecimal grossExpensePaid = sum(pendingExpenses, ProcessedExpense::grossAmount);

        BigDecimal reta = profile.quarterlySelfEmployeeFee().setScale(SCALE, ROUNDING);

        // modelo 303
        BigDecimal vatToPay = outputVat.subtract(deductibleInputVat).setScale(SCALE, ROUNDING);

        // modelo 130
        BigDecimal profitBeforeAllowance = taxableIncome
                .subtract(deductibleExpenseBase)
                .subtract(reta);

        BigDecimal hardToJustify = hardJustifyAllowance(profitBeforeAllowance);
        BigDecimal netProfit = profitBeforeAllowance.subtract(hardToJustify).setScale(SCALE, ROUNDING);

        BigDecimal irpfPrepayment = netProfit.signum() <= 0
                ? BigDecimal.ZERO.setScale(SCALE)
                : netProfit.multiply(IRPF_PAYMENT_RATE)
                  .subtract(retentions)
                  .max(BigDecimal.ZERO)
                  .setScale(SCALE, ROUNDING);

        // dinero cobrado sin pagar provedores y SS, no incluido lo que se paga a hacienda
        BigDecimal avalibleCash = totalBilled
                .subtract(grossExpensePaid)
                .subtract(reta)
                .subtract(vatToPay.max(BigDecimal.ZERO))
                .subtract(irpfPrepayment)
                .setScale(SCALE, ROUNDING);

        return new QuarterlySummary(
                quarter, year,
                totalBilled, taxableIncome, outputVat,
                deductibleInputVat, vatToPay,
                deductibleExpenseBase, reta, hardToJustify,
                netProfit, retentions, irpfPrepayment,
                avalibleCash, excludedExpenses
        );
    }

    private static BigDecimal hardJustifyAllowance(BigDecimal profitBeforeAllowance) {
        if (profitBeforeAllowance.signum() <= 0) {
            return BigDecimal.ZERO.setScale(SCALE);
        }

        BigDecimal quarterlyCap = HARD_JUSTIFY_YEAR_CAP.divide(BigDecimal.valueOf(4), SCALE, ROUNDING);
        return profitBeforeAllowance.multiply(HARD_JUSTIFY_RATE)
                .min(quarterlyCap)
                .setScale(SCALE, ROUNDING);
    }

    private static <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> field) {
        return items.stream()
                .map(field)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);
    }

    public boolean vatIsRefundable() {
        return vatToPay.signum() < 0;
    }

    public boolean hasLosses() {
        return  netProfit.signum() < 0;
    }
}

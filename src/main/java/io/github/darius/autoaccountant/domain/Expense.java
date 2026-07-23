package io.github.darius.autoaccountant.domain;

public class Expense {
    private final double taxBase;
    private final double vatAmount;
    private final ExpenseCategory category;

    public Expense(double taxBase, double vatAmount, ExpenseCategory category) {
        this.taxBase = taxBase;
        this.vatAmount = vatAmount;
        this.category = category;
    }

    public double getDeductibleTaxBase(String sector) {
        return taxBase * category.calculateDeductiblePercentage(sector);
    }

    public double getDeductibleVat(String sector) {
        return vatAmount * category.calculateDeductiblePercentage(sector);
    }

    public String getStatusMessage(String sector) {
        double percentage = category.calculateDeductiblePercentage(sector);
        if (percentage >= 1.0) return "100% deductible (" + category.name() + ")";
        if (percentage > 0.0) return "Partially deductible by law (" + category.name() + ")";
        return "Non-deductible expense (" + category.name() + ")";
    }
}

package io.github.darius.autoaccountant.domain;

public enum ExpenseCategory {

    FUEL {
        @Override
        public double calculateDeductiblePercentage(String sector) {
            return "TRANSPORT".equalsIgnoreCase(sector) ? 1.0 : 0.5;
        }
    },
    FOOD {
        @Override
        public double calculateDeductiblePercentage(String sector) {
            return 0.0;
        }
    },
    OFFICE_AND_TECH {
        @Override
        public double calculateDeductiblePercentage(String sector) {
            return 1.0;
        }
    };

    public abstract double calculateDeductiblePercentage(String sector);

    public static ExpenseCategory fromText(String text) {
        try {
            return ExpenseCategory.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            return OFFICE_AND_TECH;
            // safe default, manage this is pending dw xoxoxo
        }
    }
}

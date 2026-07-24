package io.github.darius.autoaccountant.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExpenseCategory {

    FUEL_AND_VEHICLE {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) {
            return isVehicleIntensive
                    ? new DeductionRates(1.0, 1.0, false)
                    : new DeductionRates(0.5, 0.0, true);
        }
    },
    HOME_OFFICE_SUPPLIES {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) {
            return new DeductionRates(0.0, 0.0, true);
        }
    },
    OFFICE_RENT {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, false); }
    },
    OFFICE_AND_TECH {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, false); }
    },
    PROFESSIONAL_SERVICES {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, false); }
    },
    TRAINING {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, false); }
    },
    INSURANCE_AND_FEES {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, false); }
    },
    BANKING_FEES {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, false); }
    },

    TRAVEL_AND_MEALS {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, true); }
    },
    CLIENT_ENTERTAINMENT {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, true); }
    },
    WORK_CLOTHING {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) { return new DeductionRates(1.0, 1.0, true); }
    },

    // save default so it doesnt throw a null
    OTHER {
        @Override
        public DeductionRates calculateRates(boolean isVehicleIntensive) {
            return new DeductionRates(0.0, 0.0, true);
        }
    };

    public abstract DeductionRates calculateRates(boolean isVehicleIntensive);

    @JsonCreator
    public static ExpenseCategory fromText(String text) {
        if (text == null || text.isBlank()) return OTHER;
        try {
            return ExpenseCategory.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
package io.github.darius.autoaccountant.domain;

public record DeductionRates(
        double vatPercentage,
        double irpfPercentage,
        boolean requiresReview
) {
}

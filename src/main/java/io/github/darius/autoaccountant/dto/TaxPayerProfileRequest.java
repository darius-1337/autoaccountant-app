package io.github.darius.autoaccountant.dto;

import io.github.darius.autoaccountant.domain.DeductionProfile;
import io.github.darius.autoaccountant.domain.IaeSection;
import io.github.darius.autoaccountant.domain.TaxPayerProfile;

import java.math.BigDecimal;

public record TaxPayerProfileRequest(
        String iaeCode,
        IaeSection iaeSection,
        DeductionProfile deductionProfile,
        BigDecimal monthlyRetaFee,
        int activityStartYear
) {
    public TaxPayerProfile toDomain() {
        return new TaxPayerProfile(iaeCode, iaeSection, deductionProfile, monthlyRetaFee, activityStartYear);
    }
}

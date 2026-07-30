package io.github.darius.autoaccountant.dto;

import io.github.darius.autoaccountant.domain.ClientType;
import io.github.darius.autoaccountant.domain.IncomeEntry;
import io.github.darius.autoaccountant.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeRequest(
        LocalDate date,
        BigDecimal amount,
        BigDecimal vatRate,
        ClientType clientType,
        PaymentMethod paymentMethod
) {
    public IncomeEntry toDomain() {
        return IncomeEntry.fromGrossAmount(date, amount, vatRate, clientType, paymentMethod);
    }
}

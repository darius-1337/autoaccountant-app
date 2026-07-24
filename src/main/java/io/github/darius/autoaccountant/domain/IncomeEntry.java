package io.github.darius.autoaccountant.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;


/**
 * Un ingreso declarado por el usuario. NO pasa por la IA: la mayoría de ingresos
 * de un autónomo pequeño (efectivo, Bizum) no tienen ningún PDF que analizar.
 * <p>
 * BigDecimal, no double: esto se va a sumar decenas de veces para cerrar un
 * trimestre y los errores de coma flotante se acumulan en céntimos reales.
 */
public record IncomeEntry(
        LocalDate date,
        BigDecimal taxBase,
        BigDecimal vatRate,
        ClientType clientType,
        PaymentMethod paymentMethod
) {
    private static final BigDecimal CASH_LIMIT = new BigDecimal("1000");

    private static final int SCALE = 2;

    public BigDecimal vatAmount() {
        return taxBase.multiply(vatRate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal grossAmmount() {
        return taxBase.add(vatAmount()).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal retentionAmount(IaeSection section, int activityStartYear) {
        BigDecimal rate = section.retentionRate(clientType, activityStartYear, date.getYear());

        return taxBase.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public boolean exceedsCashLimit() {
        return paymentMethod == PaymentMethod.CASH && grossAmmount().compareTo(CASH_LIMIT) > 0;
    }
}

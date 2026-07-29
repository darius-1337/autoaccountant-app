package io.github.darius.autoaccountant.domain;

import java.math.BigDecimal;
import java.math.MathContext;
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
    // limite de facturacion de dinero en efectivo
    private static final BigDecimal CASH_LIMIT = new BigDecimal("1000");
    private static final int SCALE = 2;

    public static IncomeEntry fromGrossAmount(LocalDate date, BigDecimal grossAmount, BigDecimal vatRate, ClientType clientType, PaymentMethod paymentMethod) {
        BigDecimal base = grossAmount
                .divide(BigDecimal.ONE.add(vatRate), MathContext.DECIMAL64)
                .setScale(SCALE, RoundingMode.HALF_UP);

        return new IncomeEntry(date, base, vatRate, clientType, paymentMethod);
    }

    public static IncomeEntry fromTaxBase(LocalDate date, BigDecimal taxBase, BigDecimal vatRate,
                                          ClientType clientType, PaymentMethod paymentMethod) {
        return new IncomeEntry(date, taxBase.setScale(SCALE, RoundingMode.HALF_UP),
                vatRate, clientType, paymentMethod);
    }

    public BigDecimal vatAmount() {
        return taxBase.multiply(vatRate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal grossAmount() {
        return taxBase.add(vatAmount()).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal retentionAmount(IaeSection section, int activityStartYear) {
        BigDecimal rate = section.retentionRate(clientType, activityStartYear, date.getYear());

        return taxBase.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public boolean exceedsCashLimit() {
        return paymentMethod == PaymentMethod.CASH && grossAmount().compareTo(CASH_LIMIT) > 0;
    }

    public FiscalQuarter quarter() {
        return FiscalQuarter.of(date);
    }
}

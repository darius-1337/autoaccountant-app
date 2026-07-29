package io.github.darius.autoaccountant.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escenario: tatuador autónomo, epígrafe 979.9 (sección primera, sin retenciones),
 * trabaja en estudio ajeno al que entrega un 40%, cuota RETA de 300 €/mes.
 * <p>
 * No hace falta contexto de Spring: QuarterlySummary es aritmética pura.
 */
class QuarterlySummaryTest {

    private static final BigDecimal VAT_21 = new BigDecimal("0.21");

    private final TaxPayerProfile profile = new TaxPayerProfile(
            "979.9",
            IaeSection.PRIMERA,
            DeductionProfile.STUDIO_BASED,
            new BigDecimal("300"),
            2022
    );

    /** Tres cobros de 2.117,50 € con el IVA ya incluido: base 1.750 € cada uno. */
    private List<IncomeEntry> income() {
        return List.of(
                IncomeEntry.fromGrossAmount(LocalDate.of(2026, 1, 15), new BigDecimal("2117.50"),
                        VAT_21, ClientType.PARTICULAR, PaymentMethod.CASH),
                IncomeEntry.fromGrossAmount(LocalDate.of(2026, 2, 10), new BigDecimal("2117.50"),
                        VAT_21, ClientType.PARTICULAR, PaymentMethod.BIZUM),
                IncomeEntry.fromGrossAmount(LocalDate.of(2026, 3, 5), new BigDecimal("2117.50"),
                        VAT_21, ClientType.PARTICULAR, PaymentMethod.TRANSFER)
        );
    }

    private ProcessedExpense studioCommission() {
        return new ProcessedExpense(
                LocalDate.of(2026, 3, 31),
                new BigDecimal("2100.00"), new BigDecimal("441.00"),
                new BigDecimal("441.00"), new BigDecimal("2100.00"),
                ExpenseCategory.OFFICE_RENT, false
        );
    }

    private ProcessedExpense monitor() {
        return new ProcessedExpense(
                LocalDate.of(2026, 2, 20),
                new BigDecimal("300.00"), new BigDecimal("63.00"),
                new BigDecimal("63.00"), new BigDecimal("300.00"),
                ExpenseCategory.OFFICE_AND_TECH, false
        );
    }

    @Test
    @DisplayName("El importe cobrado lleva el IVA dentro: 2117,50 € son 1750 € de base")
    void separatesVatFromGrossAmount() {
        IncomeEntry entry = IncomeEntry.fromGrossAmount(LocalDate.of(2026, 1, 15),
                new BigDecimal("2117.50"), VAT_21, ClientType.PARTICULAR, PaymentMethod.CASH);

        assertEquals(new BigDecimal("1750.00"), entry.taxBase());
        assertEquals(new BigDecimal("367.50"), entry.vatAmount());
    }

    @Test
    @DisplayName("Cierre completo del primer trimestre")
    void calculatesFullQuarter() {
        QuarterlySummary summary = QuarterlySummary.calculate(
                income(), List.of(studioCommission(), monitor()), profile, FiscalQuarter.Q1, 2026);

        // Ingresos: 5.250 € de base + 1.102,50 € de IVA repercutido
        assertEquals(new BigDecimal("5250.00"), summary.taxableIncome());
        assertEquals(new BigDecimal("1102.50"), summary.outputVat());
        assertEquals(new BigDecimal("6352.50"), summary.totalBilled());

        // Modelo 303: 1.102,50 − 504 de IVA soportado
        assertEquals(new BigDecimal("504.00"), summary.deductibleInputVat());
        assertEquals(new BigDecimal("598.50"), summary.vatToPay());

        // Rendimiento neto: 5.250 − 2.400 de gastos − 900 de RETA − 97,50 de difícil justificación
        assertEquals(new BigDecimal("97.50"), summary.hardToJustifyExpense());
        assertEquals(new BigDecimal("1852.50"), summary.netProfit());

        // Modelo 130: 20% del neto. Cliente particular, así que no hay retenciones que restar
        assertEquals(BigDecimal.ZERO.setScale(2), summary.retentionsApplied());
        assertEquals(new BigDecimal("370.50"), summary.irpfPrePayment());

        // Lo que de verdad queda para tres meses
        assertEquals(new BigDecimal("1579.50"), summary.availableCash());
    }

    @Test
    @DisplayName("Una factura pendiente de revisión no suma y se contabiliza como excluida")
    void excludesExpensesPendingReview() {
        ProcessedExpense pending = new ProcessedExpense(
                LocalDate.of(2026, 2, 20),
                new BigDecimal("500.00"), new BigDecimal("105.00"),
                new BigDecimal("105.00"), new BigDecimal("500.00"),
                ExpenseCategory.FUEL_AND_VEHICLE, true
        );

        QuarterlySummary summary = QuarterlySummary.calculate(
                income(), List.of(monitor(), pending), profile, FiscalQuarter.Q1, 2026);

        assertEquals(1, summary.excludedDocuments());
        assertEquals(new BigDecimal("63.00"), summary.deductibleInputVat());
    }

    @Test
    @DisplayName("Una factura de otro trimestre queda fuera del cálculo")
    void ignoresExpensesOutsideTheQuarter() {
        ProcessedExpense fromQ2 = new ProcessedExpense(
                LocalDate.of(2026, 4, 2),
                new BigDecimal("1000.00"), new BigDecimal("210.00"),
                new BigDecimal("210.00"), new BigDecimal("1000.00"),
                ExpenseCategory.OFFICE_AND_TECH, false
        );

        QuarterlySummary summary = QuarterlySummary.calculate(
                income(), List.of(monitor(), fromQ2), profile, FiscalQuarter.Q1, 2026);

        assertEquals(new BigDecimal("63.00"), summary.deductibleInputVat());
        assertEquals(0, summary.excludedDocuments(), "Fuera del trimestre no es lo mismo que excluida");
    }

    @Test
    @DisplayName("Un trimestre en pérdidas no genera pago fraccionado")
    void noPrepaymentWhenLosingMoney() {
        ProcessedExpense hugeExpense = new ProcessedExpense(
                LocalDate.of(2026, 2, 1),
                new BigDecimal("9000.00"), new BigDecimal("1890.00"),
                new BigDecimal("1890.00"), new BigDecimal("9000.00"),
                ExpenseCategory.OFFICE_AND_TECH, false
        );

        QuarterlySummary summary = QuarterlySummary.calculate(
                income(), List.of(hugeExpense), profile, FiscalQuarter.Q1, 2026);

        assertTrue(summary.hasLosses());
        assertEquals(BigDecimal.ZERO.setScale(2), summary.irpfPrePayment());
        assertTrue(summary.vatIsRefundable(), "Más IVA soportado que repercutido: sale a compensar");
    }
}
package io.github.darius.autoaccountant.domain;



import java.math.BigDecimal;

/**
 * La sección del IAE determina si las facturas emitidas
 * llevan retención de IRPF. Art. 95 RD 439/2007 (RIRPF).
 * <p>
 * Caso real que motivó esto: un tatuador puede darse de alta en el epígrafe 979.9
 * (sección 1ª, empresarial, sin retención) o en el 887 (sección 2ª, profesional,
 * con retención). Mismo trabajo, tributación distinta.
 */
public enum IaeSection {
    PRIMERA(false),
    SEGUNDA(true),
    TERCERA(true);

    private static final BigDecimal STANDARD_RATE = new BigDecimal("0.15");
    private static final BigDecimal REDUCED_RATE = new BigDecimal("0.07");

    private static final int REDUCED_RATE_YEARS = 3;

    private final boolean subjectToRetention;

    IaeSection(boolean subjectToRetention) {
        this.subjectToRetention = subjectToRetention;
    }

    public boolean isSubjectToRetention() {
        return subjectToRetention;
    }

    /**
     * Tipo de retención aplicable a una factura concreta.
     * <p>
     * Devuelve CERO en dos casos que se confunden con frecuencia:
     * estar en sección 1ª (nunca hay retención) y facturar a un particular
     * (nunca hay retención, aunque estés en sección 2ª).
     *
     * @param clientType         a quién se factura
     * @param activityStartYear  año de alta en la actividad, para el 7% reducido
     * @param invoiceYear        año de la factura
     */
    public BigDecimal retentionRate(ClientType clientType, int activityStartYear, int invoiceYear) {
        if (!subjectToRetention || !clientType.triggersRetention()) {
            return BigDecimal.ZERO;
        }

        boolean withinReducedPeriod = invoiceYear < activityStartYear + REDUCED_RATE_YEARS;

        return withinReducedPeriod ? REDUCED_RATE : STANDARD_RATE;
    }
}

package io.github.darius.autoaccountant.domain;

import java.math.BigDecimal;

/**
 * Configuración del autónomo para el trimestre.
 * <p>
 * Clave del diseño: iaeCode y deductionProfile son cosas DISTINTAS.
 * El epígrafe sirve para declarar ante Hacienda y para saber la sección
 * (que decide las retenciones). El perfil de deducción decide las reglas
 * de gasto. Un tatuador dado de alta en "maquilladores y esteticistas"
 * no es un esteticista: si mezclas ambos conceptos, le aplicas reglas
 * de gasto equivocadas.
 *
 * @param iaeCode           epígrafe elegido, del JSON del ISTAC (ej. "979.9")
 * @param iaeSection        sección del epígrafe: decide si hay retención
 * @param deductionProfile  perfil real de actividad: decide las reglas de gasto
 * @param monthlySelfEmployeeFee    cuota mensual de autónomos que paga el usuario.
 *                          Se pide como importe en vez de calcularse por tramos:
 *                          el usuario lo tiene en su recibo mensual, y la tabla
 *                          de tramos cambia cada año. Menos "inteligente",
 *                          más exacto y sin mantenimiento anual.
 * @param activityStartYear año de alta, para saber si aplica la retención del 7%
 */
public record TaxPayerProfile(
        String iaeCode,
        IaeSection iaeSection,
        DeductionProfile deductionProfile,
        BigDecimal monthlySelfEmployeeFee,
        int activityStartYear
) {
    public BigDecimal quarterlySelfEmployeeFee() {
        return monthlySelfEmployeeFee.multiply(BigDecimal.valueOf(3));
    }
}

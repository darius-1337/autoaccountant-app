package io.github.darius.autoaccountant.dto;

public record CalculationResponse(
        double baseTotal,
        double baseDeducible,
        double ivaTotal,
        double ivaDeducible,
        String mensaje
) {
}

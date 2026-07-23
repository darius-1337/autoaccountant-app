package io.github.darius.autoaccountant.dto;

public record OcrResult(
        double taxBase,
        double vatAmount,
        String category,
        boolean isCorrelated,
        String correlationReasoning
) {
}

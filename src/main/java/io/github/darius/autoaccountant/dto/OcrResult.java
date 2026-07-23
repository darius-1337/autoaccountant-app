package io.github.darius.autoaccountant.dto;

public record OcrResult(
        double baseImponible,
        double iva,
        String categoria
) {
}

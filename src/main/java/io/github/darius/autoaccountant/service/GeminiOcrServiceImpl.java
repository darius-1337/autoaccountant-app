package io.github.darius.autoaccountant.service;

import io.github.darius.autoaccountant.dto.OcrResult;
import org.springframework.web.multipart.MultipartFile;

public class GeminiOcrServiceImpl implements InvoiceOcrService {

    @Override
    public OcrResult readInvoice(MultipartFile file) {
        return new OcrResult(100.0, 21.0, "GASOLINERA");
    }
}

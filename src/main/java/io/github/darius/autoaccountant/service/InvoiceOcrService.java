package io.github.darius.autoaccountant.service;

import io.github.darius.autoaccountant.dto.OcrResult;
import org.springframework.web.multipart.MultipartFile;

public interface InvoiceOcrService {
    OcrResult readInvoice(MultipartFile file, String sector);
}

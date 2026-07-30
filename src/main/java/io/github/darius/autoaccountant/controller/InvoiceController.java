package io.github.darius.autoaccountant.controller;

import io.github.darius.autoaccountant.domain.DeductionProfile;
import io.github.darius.autoaccountant.dto.OcrResult;
import io.github.darius.autoaccountant.dto.TaxCalculationResponse;
import io.github.darius.autoaccountant.service.InvoiceOcrService;
import io.github.darius.autoaccountant.service.TaxCalculatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceOcrService ocrService;
    private final TaxCalculatorService taxCalculatorService;

    public InvoiceController(InvoiceOcrService ocrService, TaxCalculatorService taxCalculatorService) {
        this.ocrService = ocrService;
        this.taxCalculatorService = taxCalculatorService;
    }

    @PostMapping("/process")
    public ResponseEntity<TaxCalculationResponse> processInvoice(
            @RequestParam("file")MultipartFile file,
            @RequestParam("sector") String sector
            ) {
        try {
            OcrResult ocrResult = ocrService.readInvoice(file, sector);

            DeductionProfile profile = DeductionProfile.valueOf(sector);
            TaxCalculationResponse response = taxCalculatorService.processExpense(ocrResult, sector);

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();

            // refactor this later with a global error handler so i dont use generic exceptions
        }
    }
}

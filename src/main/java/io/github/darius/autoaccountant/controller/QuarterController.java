package io.github.darius.autoaccountant.controller;

import io.github.darius.autoaccountant.domain.IncomeEntry;
import io.github.darius.autoaccountant.domain.ProcessedExpense;
import io.github.darius.autoaccountant.domain.QuarterlySummary;
import io.github.darius.autoaccountant.dto.IncomeRequest;
import io.github.darius.autoaccountant.dto.QuarterlyRequest;
import io.github.darius.autoaccountant.dto.TaxCalculationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quarter")
public class QuarterController {

    @PostMapping("/summary")
    public ResponseEntity<QuarterlySummary> calculateQuarter(@RequestBody QuarterlyRequest request) {
        List<IncomeEntry> income = request.income().stream()
                .map(IncomeRequest::toDomain)
                .toList();

        List<ProcessedExpense> expenses = request.expenses().stream()
                .filter(TaxCalculationResponse::canBeAssignedToQuarter)
                .map(TaxCalculationResponse::toDomain)
                .toList();

        QuarterlySummary summary = QuarterlySummary.calculate(
                income, expenses, request.profile().toDomain(), request.quarter(), request.year()
        );

        return ResponseEntity.ok(summary);
    }
}

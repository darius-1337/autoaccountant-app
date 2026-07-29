package io.github.darius.autoaccountant.domain;

import java.time.LocalDate;
import java.time.Month;

/* Trimeste fiscal español, segun el modelo 303 y 130
presentados por trimestres naturales en ingles "Quarter".*/
public enum FiscalQuarter {

    Q1(Month.JANUARY, Month.MARCH),
    Q2(Month.APRIL, Month.JUNE),
    Q3(Month.JULY, Month.SEPTEMBER),
    Q4(Month.OCTOBER, Month.DECEMBER);

    private final Month firstMonth;
    private final Month lastMonth;

    FiscalQuarter(Month firstMonth, Month lastMonth) {
        this.firstMonth = firstMonth;
        this.lastMonth = lastMonth;
    }

    public static FiscalQuarter of(LocalDate date) {
        return values()[(date.getMonthValue() - 1) / 3];
    }

    public LocalDate firstDay(int year) {
        return LocalDate.of(year, firstMonth, 1);
    }

    public LocalDate lastDay(int year) {
        return LocalDate.of(year, lastMonth, lastMonth.length(LocalDate.of(year, 1, 1).isLeapYear()));
    }

    public boolean contains(LocalDate date, int year) {
        return date.getYear() == year && of(date) == this;
    }

    public LocalDate deadLine(int year) {
        return this == Q4 ? LocalDate.of(year + 1, Month.JANUARY, 30) : LocalDate.of(year, lastMonth.plus(1), 20);
    }

    public String label() {
        return name() + firstMonth.getValue() + lastMonth.getValue();
    }

}

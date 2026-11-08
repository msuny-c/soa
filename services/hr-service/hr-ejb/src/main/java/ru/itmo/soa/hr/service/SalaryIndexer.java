package ru.itmo.soa.hr.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class SalaryIndexer {

    private static final BigDecimal MAX_SALARY = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigDecimal MIN_ROUNDED_TO_ONE = new BigDecimal("0.5");
    private static final int MAX_PLAIN_DIGITS = 40;

    private SalaryIndexer() {
    }

    public static Outcome index(long oldSalary, BigDecimal coeff) {
        BigDecimal product = BigDecimal.valueOf(oldSalary).multiply(coeff);
        if (product.compareTo(MAX_SALARY) > 0) {
            return Outcome.tooLarge(product);
        }
        if (product.compareTo(MIN_ROUNDED_TO_ONE) < 0) {
            return Outcome.notPositive(product);
        }
        BigDecimal rounded = product.setScale(0, RoundingMode.HALF_UP);
        if (rounded.compareTo(MAX_SALARY) > 0) {
            return Outcome.tooLarge(rounded);
        }
        return new Outcome(rounded.longValueExact(), null, null);
    }

    private static String print(BigDecimal value) {
        return value.precision() - value.scale() <= MAX_PLAIN_DIGITS
                ? value.setScale(0, RoundingMode.HALF_UP).toPlainString()
                : value.round(new MathContext(MAX_PLAIN_DIGITS)).toString();
    }

    public record Outcome(Long newSalary, String message, String issue) {

        static Outcome tooLarge(BigDecimal value) {
            return new Outcome(null,
                    "Результат индексации превышает максимально допустимое значение",
                    "Значение " + print(value) + " превышает лимит int64");
        }

        static Outcome notPositive(BigDecimal value) {
            return new Outcome(null,
                    "Результат индексации должен быть строго больше 0",
                    "Значение " + value.round(new MathContext(6)).toString() + " округляется до 0");
        }

        public boolean isValid() {
            return newSalary != null;
        }
    }
}

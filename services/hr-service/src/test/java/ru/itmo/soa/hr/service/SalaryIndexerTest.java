package ru.itmo.soa.hr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SalaryIndexerTest {

    @ParameterizedTest
    @CsvSource({
            "85000, 1.1, 93500",
            "100, 0.5, 50",
            "3, 0.5, 2",
            "1, 0.5, 1",
            "10, 1.05, 11",
            "9223372036854775807, 1, 9223372036854775807",
            "1000, 1e-3, 1"
    })
    void roundsHalfUp(long oldSalary, String coeff, long expected) {
        SalaryIndexer.Outcome outcome = SalaryIndexer.index(oldSalary, new BigDecimal(coeff));

        assertEquals(expected, outcome.newSalary());
    }

    @ParameterizedTest
    @CsvSource({
            "9223372036854775807, 1.0000001",
            "9223372036854775807, 1e999999999",
            "1, 0.49",
            "1000, 1e-999999999"
    })
    void rejectsOverflowAndNonPositiveResults(long oldSalary, String coeff) {
        SalaryIndexer.Outcome outcome = SalaryIndexer.index(oldSalary, new BigDecimal(coeff));

        assertFalse(outcome.isValid());
    }

    @ParameterizedTest
    @CsvSource({"9223372036854775807, 2, Значение 18446744073709551614 превышает лимит int64"})
    void describesOverflow(long oldSalary, String coeff, String issue) {
        assertEquals(issue, SalaryIndexer.index(oldSalary, new BigDecimal(coeff)).issue());
    }
}

package org.ikigaidigital.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDepositMapperTest {

    @Test
    void decimalBecomesTheSameDouble() {
        assertThat(TimeDepositMapper.toDouble(new BigDecimal("1000.00"))).isEqualTo(1000.0);
        assertThat(TimeDepositMapper.toDouble(new BigDecimal("1234567.00"))).isEqualTo(1234567.0);
    }

    @Test
    void doubleBecomesDecimalWithTwoDecimals() {
        assertThat(TimeDepositMapper.toDecimal(1000.83)).isEqualByComparingTo("1000.83").hasToString("1000.83");
        assertThat(TimeDepositMapper.toDecimal(1000.0)).hasToString("1000.00");
    }

    @Test
    void floatNoiseFromTheCalculatorIsDropped() {
        // 1234567.00 + 1028.81 in double math
        double calculated = 1234567.00 + 1028.81;

        assertThat(TimeDepositMapper.toDecimal(calculated)).hasToString("1235595.81");
    }
}

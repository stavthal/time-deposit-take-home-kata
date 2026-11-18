package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Characterization tests. They pin the current behavior of
 * {@link TimeDepositCalculator#updateBalance(List)} before any refactor.
 * Expected values are what the code returns today, compared with exact double equality.
 * Do not "fix" an expected value: a change here is a behavior change.
 *
 * <p>Hand calculation format in the table: {@code start * rate / 12 = raw -> rounded}.
 * {@code rounded} is {@code new BigDecimal(raw).setScale(2, HALF_UP)}, and
 * {@code expected = start + rounded} in double math.
 */
class TimeDepositCalculatorTest {

    private static final double BALANCE = 1000.00;

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0} day {1} balance {2} -> {3}")
    @CsvSource(textBlock = """
            # plan,  days, start,      expected

            # Day 30/31, every plan: interest needs `getDays() > 30`
            # 30 is not > 30, interest 0
            basic,     30, 1000.00,    1000.00
            # 1000 * 0.01 / 12 = 0.8333 -> 0.83
            basic,     31, 1000.00,    1000.83
            # 30 is not > 30, interest 0
            student,   30, 1000.00,    1000.00
            # 1000 * 0.03 / 12 = 2.50 -> 2.50
            student,   31, 1000.00,    1002.50
            # 30 is not > 30, interest 0
            premium,   30, 1000.00,    1000.00
            # 31 > 30, but premium also needs > 45, interest 0
            premium,   31, 1000.00,    1000.00

            # Premium 45/46: `getDays() > 45`
            # 45 is not > 45, interest 0
            premium,   45, 1000.00,    1000.00
            # 1000 * 0.05 / 12 = 4.1666 -> 4.17
            premium,   46, 1000.00,    1004.17

            # Student 365/366: `getDays() < 366`
            # 365 < 366: 1000 * 0.03 / 12 = 2.50 -> 2.50
            student,  365, 1000.00,    1002.50
            # 366 is not < 366, interest 0
            student,  366, 1000.00,    1000.00

            # Day 0, every plan: 0 is not > 30, interest 0
            basic,      0, 1000.00,    1000.00
            student,    0, 1000.00,    1000.00
            premium,    0, 1000.00,    1000.00

            # Basic and premium have no end date
            # 1000 * 0.01 / 12 = 0.8333 -> 0.83
            basic,   1000, 1000.00,    1000.83
            # 1000 * 0.05 / 12 = 4.1666 -> 4.17
            premium,  400, 1000.00,    1004.17

            # Unknown plan: no branch matches, interest 0
            gold,     100, 1000.00,    1000.00
            # Plan names are case sensitive (`equals`), interest 0
            Basic,    100, 1000.00,    1000.00
            PREMIUM,  100, 1000.00,    1000.00

            # Balance 0: 0 * rate / 12 = 0 -> 0.00
            basic,     31,    0.00,       0.00
            student,   31,    0.00,       0.00
            premium,   46,    0.00,       0.00

            # Half cent interest
            # 150 * 0.01 / 12 = 0.125 exactly in double -> HALF_UP 0.13 (HALF_EVEN would give 0.12)
            basic,     31,  150.00,     150.13
            # 18 * 0.01 / 12 looks like 0.015, the double is 0.01499999... -> 0.01, not 0.02
            basic,     31,   18.00,      18.01

            # Large balances round to cents
            # 1234567 * 0.01 / 12 = 1028.8058 -> 1028.81
            basic,     45, 1234567.00, 1235595.81
            # 1234567 * 0.03 / 12 = 3086.4175 -> 3086.42
            student,   45, 1234567.00, 1237653.42
            # 1234567 * 0.05 / 12 = 5144.0291 -> 5144.03
            premium,   46, 1234567.00, 1239711.03

            # Double noise stays in the result
            # 6.10 * 0.01 / 12 = 0.00508 -> 0.01, and 6.10 + 0.01 in double is 6.109999999999999
            basic,     31,    6.10,   6.109999999999999
            """)
    void pinsBalanceAfterOneUpdate(String planType, int days, double start, double expected) {
        TimeDeposit deposit = new TimeDeposit(1, planType, start, days);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expected);
    }

    @Test
    void updatesEveryDepositInTheList() {
        TimeDeposit basic = new TimeDeposit(1, "basic", BALANCE, 31);
        TimeDeposit student = new TimeDeposit(2, "student", BALANCE, 366);
        TimeDeposit premium = new TimeDeposit(3, "premium", BALANCE, 46);
        TimeDeposit unknown = new TimeDeposit(4, "gold", BALANCE, 100);

        calculator.updateBalance(List.of(basic, student, premium, unknown));

        // 1000 * 0.01 / 12 = 0.8333 -> 0.83
        assertThat(basic.getBalance()).isEqualTo(1000.83);
        // 366 is not < 366, interest 0
        assertThat(student.getBalance()).isEqualTo(1000.00);
        // 1000 * 0.05 / 12 = 4.1666 -> 4.17
        assertThat(premium.getBalance()).isEqualTo(1004.17);
        // unknown plan, interest 0
        assertThat(unknown.getBalance()).isEqualTo(1000.00);
    }

    @Test
    void changesOnlyTheBalance() {
        TimeDeposit deposit = new TimeDeposit(7, "basic", BALANCE, 31);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getId()).isEqualTo(7);
        assertThat(deposit.getPlanType()).isEqualTo("basic");
        assertThat(deposit.getDays()).isEqualTo(31);
    }

    @Test
    void eachCallAddsOneMoreMonthOnTheNewBalance() {
        TimeDeposit deposit = new TimeDeposit(1, "premium", BALANCE, 46);

        calculator.updateBalance(List.of(deposit));
        calculator.updateBalance(List.of(deposit));

        // First call: 1000 * 0.05 / 12 = 4.1666 -> 4.17, balance 1004.17.
        // Second call: 1004.17 * 0.05 / 12 = 4.1840 -> 4.18.
        // 1004.17 + 4.18 in double is 1008.3499999999999, not 1008.35.
        assertThat(deposit.getBalance()).isEqualTo(1008.3499999999999);
    }

    @Test
    void emptyListIsAccepted() {
        assertThatCode(() -> calculator.updateBalance(List.of())).doesNotThrowAnyException();
    }
}

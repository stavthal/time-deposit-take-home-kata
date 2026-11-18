package org.ikigaidigital;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Characterization tests. They pin the current behavior of
 * {@link TimeDepositCalculator#updateBalance(List)} before any refactor.
 * Expected values are what the code returns today, compared with exact double equality.
 * Do not "fix" an expected value: a change here is a behavior change.
 */
class TimeDepositCalculatorTest {

    private static final double BALANCE = 1000.00;

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    // No interest in the first 30 days: `getDays() > 30`

    @Test
    void basicGetsNoInterestOnDay30() {
        assertThat(balanceAfterUpdate("basic", BALANCE, 30)).isEqualTo(1000.00);
    }

    @Test
    void basicGetsInterestFromDay31() {
        assertThat(balanceAfterUpdate("basic", BALANCE, 31)).isEqualTo(1000.83);
    }

    @Test
    void studentGetsNoInterestOnDay30() {
        assertThat(balanceAfterUpdate("student", BALANCE, 30)).isEqualTo(1000.00);
    }

    @Test
    void studentGetsInterestFromDay31() {
        assertThat(balanceAfterUpdate("student", BALANCE, 31)).isEqualTo(1002.50);
    }

    @Test
    void premiumGetsNoInterestOnDay30() {
        assertThat(balanceAfterUpdate("premium", BALANCE, 30)).isEqualTo(1000.00);
    }

    @Test
    void noPlanGetsInterestOnDay0() {
        assertThat(balanceAfterUpdate("basic", BALANCE, 0)).isEqualTo(1000.00);
        assertThat(balanceAfterUpdate("student", BALANCE, 0)).isEqualTo(1000.00);
        assertThat(balanceAfterUpdate("premium", BALANCE, 0)).isEqualTo(1000.00);
    }

    // Basic: 1% a year, no end

    @Test
    void basicKeepsGettingInterestAfterOneYear() {
        assertThat(balanceAfterUpdate("basic", BALANCE, 1000)).isEqualTo(1000.83);
    }

    // Premium: 5% a year, starts after 45 days: `getDays() > 45`

    @Test
    void premiumGetsNoInterestOnDay31() {
        assertThat(balanceAfterUpdate("premium", BALANCE, 31)).isEqualTo(1000.00);
    }

    @Test
    void premiumGetsNoInterestOnDay45() {
        assertThat(balanceAfterUpdate("premium", BALANCE, 45)).isEqualTo(1000.00);
    }

    @Test
    void premiumGetsInterestFromDay46() {
        assertThat(balanceAfterUpdate("premium", BALANCE, 46)).isEqualTo(1004.17);
    }

    @Test
    void premiumKeepsGettingInterestAfterOneYear() {
        assertThat(balanceAfterUpdate("premium", BALANCE, 400)).isEqualTo(1004.17);
    }

    // Student: 3% a year, ends after one year: `getDays() < 366`

    @Test
    void studentGetsInterestOnDay365() {
        assertThat(balanceAfterUpdate("student", BALANCE, 365)).isEqualTo(1002.50);
    }

    @Test
    void studentGetsNoInterestFromDay366() {
        assertThat(balanceAfterUpdate("student", BALANCE, 366)).isEqualTo(1000.00);
    }

    // Plan names

    @Test
    void unknownPlanGetsNoInterest() {
        assertThat(balanceAfterUpdate("gold", BALANCE, 100)).isEqualTo(1000.00);
    }

    @Test
    void planNameIsCaseSensitive() {
        assertThat(balanceAfterUpdate("Basic", BALANCE, 100)).isEqualTo(1000.00);
        assertThat(balanceAfterUpdate("PREMIUM", BALANCE, 100)).isEqualTo(1000.00);
    }

    // Rounding

    @Test
    void largeBalancesRoundInterestHalfUpToCents() {
        // basic: 1234567.00 * 0.01 / 12 = 1028.8058... rounds to 1028.81
        assertThat(balanceAfterUpdate("basic", 1234567.00, 45)).isEqualTo(1235595.81);
        // student: 3086.4175 rounds to 3086.42
        assertThat(balanceAfterUpdate("student", 1234567.00, 45)).isEqualTo(1237653.42);
        // premium: 5144.0291... rounds to 5144.03
        assertThat(balanceAfterUpdate("premium", 1234567.00, 46)).isEqualTo(1239711.03);
    }

    @Test
    void exactHalfCentInterestRoundsUp() {
        // 150.00 * 0.01 / 12 is exactly 0.125 in double. HALF_UP gives 0.13 (HALF_EVEN would give 0.12).
        assertThat(balanceAfterUpdate("basic", 150.00, 31)).isEqualTo(150.13);
    }

    @Test
    void halfCentInterestRoundsDownBecauseOfBinaryDouble() {
        // 18.00 * 0.01 / 12 looks like 0.015, but the double is 0.01499999...
        // `new BigDecimal(double)` keeps that exact value, so HALF_UP gives 0.01, not 0.02.
        assertThat(balanceAfterUpdate("basic", 18.00, 31)).isEqualTo(18.01);
    }

    @Test
    void resultKeepsDoubleNoise() {
        // 6.10 + 0.01 in double is 6.109999999999999, not 6.11.
        assertThat(balanceAfterUpdate("basic", 6.10, 31)).isEqualTo(6.109999999999999);
    }

    // List handling and untouched fields

    @Test
    void updatesEveryDepositInTheList() {
        TimeDeposit basic = new TimeDeposit(1, "basic", BALANCE, 31);
        TimeDeposit student = new TimeDeposit(2, "student", BALANCE, 31);
        TimeDeposit premium = new TimeDeposit(3, "premium", BALANCE, 46);

        calculator.updateBalance(List.of(basic, student, premium));

        assertThat(basic.getBalance()).isEqualTo(1000.83);
        assertThat(student.getBalance()).isEqualTo(1002.50);
        assertThat(premium.getBalance()).isEqualTo(1004.17);
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

        // 1004.17 * 0.05 / 12 = 4.1840... rounds to 4.18.
        // 1004.17 + 4.18 in double is 1008.3499999999999, not 1008.35.
        assertThat(deposit.getBalance()).isEqualTo(1008.3499999999999);
    }

    @Test
    void emptyListIsAccepted() {
        assertThatCode(() -> calculator.updateBalance(List.of())).doesNotThrowAnyException();
    }

    private double balanceAfterUpdate(String planType, double balance, int days) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        calculator.updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}

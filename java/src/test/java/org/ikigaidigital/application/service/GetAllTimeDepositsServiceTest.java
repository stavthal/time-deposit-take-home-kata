package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GetAllTimeDepositsServiceTest {

    @Test
    void returnsEveryDepositWithItsWithdrawals() {
        TimeDepositDetails withWithdrawal = new TimeDepositDetails(
                new TimeDeposit(1, "basic", 1000.00, 31),
                List.of(new Withdrawal(10, new BigDecimal("50.00"), LocalDate.of(2026, 1, 15))));
        TimeDepositDetails withoutWithdrawal = new TimeDepositDetails(
                new TimeDeposit(2, "student", 500.00, 40), List.of());
        InMemoryTimeDepositRepository repository = new InMemoryTimeDepositRepository()
                .withDetails(withWithdrawal)
                .withDetails(withoutWithdrawal);

        List<TimeDepositDetails> result = new GetAllTimeDepositsService(repository).getAllTimeDeposits();

        assertThat(result).containsExactly(withWithdrawal, withoutWithdrawal);
    }
}

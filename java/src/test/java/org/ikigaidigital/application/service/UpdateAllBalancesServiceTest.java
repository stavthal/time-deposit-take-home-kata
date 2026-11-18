package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateAllBalancesServiceTest {

    @Test
    void appliesOneMonthOfInterestAndSavesEveryDeposit() {
        InMemoryTimeDepositRepository repository = new InMemoryTimeDepositRepository()
                .withDeposit(new TimeDeposit(1, "basic", 1000.00, 31))
                .withDeposit(new TimeDeposit(2, "premium", 1000.00, 45))
                .withDeposit(new TimeDeposit(3, "student", 1000.00, 365));

        new UpdateAllBalancesService(repository, new TimeDepositCalculator()).updateAllBalances();

        // 1000 * 0.01 / 12 = 0.8333 -> 0.83
        assertThat(repository.stored(1).getBalance()).isEqualTo(1000.83);
        // premium needs > 45 days, interest 0
        assertThat(repository.stored(2).getBalance()).isEqualTo(1000.00);
        // 1000 * 0.03 / 12 = 2.50
        assertThat(repository.stored(3).getBalance()).isEqualTo(1002.50);
    }
}

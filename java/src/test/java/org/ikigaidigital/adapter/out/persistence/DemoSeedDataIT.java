package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.PostgresIntegrationTest;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresIntegrationTest
class DemoSeedDataIT {

    @Autowired
    private TimeDepositPersistenceAdapter adapter;

    @Test
    void flywayLoadsOneDepositPerBoundaryWithWithdrawals() {
        List<TimeDepositDetails> details = adapter.findAllWithWithdrawals();

        assertThat(details).extracting(d -> d.deposit().getPlanType() + " " + d.deposit().getDays())
                .containsExactly("basic 30", "basic 31", "student 365", "student 366",
                        "premium 45", "premium 46", "basic 45");
        assertThat(details).extracting(d -> d.withdrawals().size())
                .containsExactly(0, 2, 1, 0, 0, 0, 1);
    }
}

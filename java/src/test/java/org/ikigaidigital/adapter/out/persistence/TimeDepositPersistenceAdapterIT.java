package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.ikigaidigital.PostgresIntegrationTest;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Runs against real Postgres with the Flyway schema. */
@PostgresIntegrationTest
class TimeDepositPersistenceAdapterIT {

    @Autowired
    private TimeDepositPersistenceAdapter adapter;
    @Autowired
    private UpdateAllBalancesUseCase updateAllBalances;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void replaceRowsWithTestData() {
        jdbc.update("DELETE FROM \"withdrawals\"");
        jdbc.update("DELETE FROM \"timeDeposits\"");
        jdbc.update("""
                INSERT INTO "timeDeposits" ("id", "planType", "days", "balance") VALUES
                (1, 'basic', 31, 1234567.00),
                (2, 'student', 365, 1000.00),
                (3, 'premium', 45, 1000.00)""");
        jdbc.update("""
                INSERT INTO "withdrawals" ("id", "timeDepositId", "amount", "date") VALUES
                (11, 1, 100.50, DATE '2026-01-10'),
                (12, 1, 20.25, DATE '2026-02-10'),
                (21, 2, 5.00, DATE '2026-03-01')""");
    }

    @Test
    void findAllWithWithdrawalsLoadsEverythingInOneStatement() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<TimeDepositDetails> details = adapter.findAllWithWithdrawals();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(details).extracting(d -> d.deposit().getId()).containsExactly(1, 2, 3);
        assertThat(details.get(0).deposit().getBalance()).isEqualTo(1234567.0);
        assertThat(details.get(0).withdrawals()).containsExactly(
                new Withdrawal(11, new BigDecimal("100.50"), LocalDate.of(2026, 1, 10)),
                new Withdrawal(12, new BigDecimal("20.25"), LocalDate.of(2026, 2, 10)));
        assertThat(details.get(1).withdrawals()).hasSize(1);
        assertThat(details.get(2).withdrawals()).isEmpty();
    }

    @Test
    void findAllMapsEveryDeposit() {
        List<TimeDeposit> deposits = adapter.findAll();

        assertThat(deposits).extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getBalance, TimeDeposit::getDays)
                .containsExactly(
                        tuple(1, "basic", 1234567.0, 31),
                        tuple(2, "student", 1000.0, 365),
                        tuple(3, "premium", 1000.0, 45));
    }

    @Test
    void updateAllBalancesStoresRoundedBalances() {
        updateAllBalances.updateAllBalances();
        entityManager.flush();

        List<BigDecimal> balances = jdbc.queryForList(
                "SELECT \"balance\" FROM \"timeDeposits\" ORDER BY \"id\"", BigDecimal.class);
        // 1234567.00 * 0.01 / 12 = 1028.8058 -> 1028.81; 1000 * 0.03 / 12 = 2.50; premium day 45 gets 0
        assertThat(balances).containsExactly(
                new BigDecimal("1235595.81"), new BigDecimal("1002.50"), new BigDecimal("1000.00"));
    }
}

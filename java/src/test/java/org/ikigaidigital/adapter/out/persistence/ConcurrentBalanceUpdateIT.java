package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.PostgresTestcontainer;
import org.ikigaidigital.app.TimeDepositApplication;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Balance updates that run at the same time must not lose a month of interest.
 * Not {@code @PostgresIntegrationTest}: its rollback transaction would make every thread share one
 * transaction and hide the race. Same Spring setup, so the context and container are reused.
 * The test commits, so it puts the seed balances back afterwards.
 */
@SpringBootTest(classes = TimeDepositApplication.class, properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(PostgresTestcontainer.class)
class ConcurrentBalanceUpdateIT {

    private static final int UPDATES = 8;

    @Autowired
    private UpdateAllBalancesUseCase updateAllBalances;
    @Autowired
    private JdbcTemplate jdbc;

    private List<Map<String, Object>> seedBalances;

    @BeforeEach
    void rememberSeedBalances() {
        seedBalances = jdbc.queryForList("SELECT \"id\", \"balance\" FROM \"timeDeposits\"");
    }

    @AfterEach
    void restoreSeedBalances() {
        seedBalances.forEach(row -> jdbc.update(
                "UPDATE \"timeDeposits\" SET \"balance\" = ? WHERE \"id\" = ?", row.get("balance"), row.get("id")));
    }

    @Test
    void updatesAtTheSameTimeEachApplyOneMonth() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<Void> update = () -> {
            start.await();
            updateAllBalances.updateAllBalances();
            return null;
        };
        ExecutorService pool = Executors.newFixedThreadPool(UPDATES);
        try {
            List<Future<Void>> results = IntStream.range(0, UPDATES).mapToObj(i -> pool.submit(update)).toList();
            start.countDown();
            for (Future<Void> result : results) {
                result.get();
            }
        } finally {
            pool.shutdown();
        }

        // 8 months, each stored rounded to cents, as 8 POSTs one after another would give.
        assertThat(balanceOf(2)).isEqualByComparingTo("1006.69");  // basic, day 31
        assertThat(balanceOf(3)).isEqualByComparingTo("1020.18");  // student, day 365
        assertThat(balanceOf(6)).isEqualByComparingTo("1033.82");  // premium, day 46
    }

    private BigDecimal balanceOf(int id) {
        return jdbc.queryForObject("SELECT \"balance\" FROM \"timeDeposits\" WHERE \"id\" = ?", BigDecimal.class, id);
    }
}

package org.ikigaidigital.config;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The whole use case runs in one transaction. Hand written fake manager, no database. */
class TransactionalUpdateAllBalancesUseCaseTest {

    private final List<String> events = new ArrayList<>();

    private final PlatformTransactionManager recordingManager = new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            events.add("begin");
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            events.add("commit");
        }

        @Override
        public void rollback(TransactionStatus status) {
            events.add("rollback");
        }
    };

    @Test
    void runsTheUseCaseInsideOneCommittedTransaction() {
        var useCase = new TransactionalUpdateAllBalancesUseCase(() -> events.add("update"), recordingManager);

        useCase.updateAllBalances();

        assertThat(events).containsExactly("begin", "update", "commit");
    }

    @Test
    void rollsBackWhenTheUseCaseFails() {
        var useCase = new TransactionalUpdateAllBalancesUseCase(() -> {
            throw new IllegalStateException("boom");
        }, recordingManager);

        assertThatThrownBy(useCase::updateAllBalances).hasMessage("boom");
        assertThat(events).containsExactly("begin", "rollback");
    }
}

package org.ikigaidigital.config;

import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the whole use case, read, calculate and write, in one transaction.
 * A decorator here keeps the application layer Spring free.
 */
class TransactionalUpdateAllBalancesUseCase implements UpdateAllBalancesUseCase {

    private final UpdateAllBalancesUseCase delegate;
    private final TransactionTemplate transaction;

    TransactionalUpdateAllBalancesUseCase(UpdateAllBalancesUseCase delegate, PlatformTransactionManager transactionManager) {
        this.delegate = delegate;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    // Assumption: two updates at once are out of scope for a monthly batch, so no row locks.
    @Override
    public void updateAllBalances() {
        transaction.executeWithoutResult(status -> delegate.updateAllBalances());
    }
}

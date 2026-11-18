package org.ikigaidigital.application.port.in;

public interface UpdateAllBalancesUseCase {
    /** Applies one month of interest to every stored deposit. */
    void updateAllBalances();
}

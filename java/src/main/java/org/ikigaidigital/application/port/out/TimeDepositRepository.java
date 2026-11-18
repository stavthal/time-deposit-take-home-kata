package org.ikigaidigital.application.port.out;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositDetails;

import java.util.List;

public interface TimeDepositRepository {
    List<TimeDeposit> findAll();

    List<TimeDepositDetails> findAllWithWithdrawals();

    /** Stores the balance of each given deposit. */
    void saveAll(List<TimeDeposit> deposits);
}

package org.ikigaidigital.application.port.in;

import org.ikigaidigital.domain.TimeDepositDetails;

import java.util.List;

public interface GetAllTimeDepositsUseCase {
    List<TimeDepositDetails> getAllTimeDeposits();
}

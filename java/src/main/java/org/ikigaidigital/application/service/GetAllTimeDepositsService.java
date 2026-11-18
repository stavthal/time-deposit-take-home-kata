package org.ikigaidigital.application.service;

import org.ikigaidigital.application.port.in.GetAllTimeDepositsUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;

import java.util.List;

public class GetAllTimeDepositsService implements GetAllTimeDepositsUseCase {

    private final TimeDepositRepository repository;

    public GetAllTimeDepositsService(TimeDepositRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TimeDepositDetails> getAllTimeDeposits() {
        return repository.findAllWithWithdrawals();
    }
}

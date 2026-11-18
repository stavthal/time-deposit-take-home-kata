package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;

import java.util.List;

public class UpdateAllBalancesService implements UpdateAllBalancesUseCase {

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public UpdateAllBalancesService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    // Assumption: days are not advanced. The frozen calculator never changes them,
    // and the README does not say what moves a deposit to its next day.
    @Override
    public void updateAllBalances() {
        List<TimeDeposit> deposits = repository.findAll();
        calculator.updateBalance(deposits);
        repository.saveAll(deposits);
    }
}

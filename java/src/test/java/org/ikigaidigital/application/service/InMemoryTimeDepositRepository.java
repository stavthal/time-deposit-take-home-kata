package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Test fake for the outbound port. Stores balances by id, like the real adapter. */
class InMemoryTimeDepositRepository implements TimeDepositRepository {

    private final Map<Integer, TimeDeposit> deposits = new TreeMap<>();
    private final List<TimeDepositDetails> details = new ArrayList<>();

    InMemoryTimeDepositRepository withDeposit(TimeDeposit deposit) {
        deposits.put(deposit.getId(), copy(deposit));
        return this;
    }

    InMemoryTimeDepositRepository withDetails(TimeDepositDetails entry) {
        details.add(entry);
        return this;
    }

    TimeDeposit stored(int id) {
        return deposits.get(id);
    }

    @Override
    public List<TimeDeposit> findAll() {
        return deposits.values().stream().map(InMemoryTimeDepositRepository::copy).toList();
    }

    @Override
    public List<TimeDepositDetails> findAllWithWithdrawals() {
        return List.copyOf(details);
    }

    @Override
    public void saveAll(List<TimeDeposit> updated) {
        updated.forEach(deposit -> deposits.get(deposit.getId()).setBalance(deposit.getBalance()));
    }

    private static TimeDeposit copy(TimeDeposit deposit) {
        return new TimeDeposit(deposit.getId(), deposit.getPlanType(), deposit.getBalance(), deposit.getDays());
    }
}

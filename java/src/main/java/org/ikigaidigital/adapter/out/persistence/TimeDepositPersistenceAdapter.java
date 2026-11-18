package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class TimeDepositPersistenceAdapter implements TimeDepositRepository {

    private final TimeDepositJpaRepository jpaRepository;

    TimeDepositPersistenceAdapter(TimeDepositJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeDeposit> findAll() {
        return jpaRepository.findAllByOrderByIdAsc().stream().map(TimeDepositMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeDepositDetails> findAllWithWithdrawals() {
        return jpaRepository.findAllWithWithdrawalsByOrderByIdAsc().stream().map(TimeDepositMapper::toDetails).toList();
    }

    // Assumption: the write is one transaction here, so the application layer stays Spring free.
    // Reading and writing are two transactions; concurrent balance updates are out of scope.
    @Override
    @Transactional
    public void saveAll(List<TimeDeposit> deposits) {
        Map<Integer, TimeDepositEntity> entities = jpaRepository
                .findAllById(deposits.stream().map(TimeDeposit::getId).toList())
                .stream()
                .collect(Collectors.toMap(TimeDepositEntity::getId, Function.identity()));
        deposits.forEach(deposit ->
                entities.get(deposit.getId()).setBalance(TimeDepositMapper.toDecimal(deposit.getBalance())));
    }
}

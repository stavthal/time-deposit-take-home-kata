package org.ikigaidigital.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

    List<TimeDepositEntity> findAllByOrderByIdAsc();

    /** One SQL with a left join on withdrawals, so no extra query per deposit. */
    @EntityGraph(attributePaths = "withdrawals")
    List<TimeDepositEntity> findAllWithWithdrawalsByOrderByIdAsc();
}

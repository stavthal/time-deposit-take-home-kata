package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

    /**
     * Locks the rows (Postgres: FOR NO KEY UPDATE). A second balance update waits until the first commits,
     * so no month is lost.
     * Rows are locked in id order, so two updates cannot deadlock. A plain GET never waits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TimeDepositEntity> findAllByOrderByIdAsc();

    /** One SQL with a left join on withdrawals, so no extra query per deposit. */
    @EntityGraph(attributePaths = "withdrawals")
    List<TimeDepositEntity> findAllWithWithdrawalsByOrderByIdAsc();
}

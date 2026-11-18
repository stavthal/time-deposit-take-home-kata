package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.domain.Withdrawal;

import java.math.BigDecimal;
import java.time.LocalDate;

record WithdrawalResponse(int id, BigDecimal amount, LocalDate date) {

    static WithdrawalResponse from(Withdrawal withdrawal) {
        return new WithdrawalResponse(withdrawal.id(), withdrawal.amount(), withdrawal.date());
    }
}

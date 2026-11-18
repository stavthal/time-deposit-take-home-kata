package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositDetails;

import java.util.List;

/** GET schema from the README: id, planType, balance, days, withdrawals. */
record TimeDepositResponse(int id, String planType, Double balance, int days, List<WithdrawalResponse> withdrawals) {

    static TimeDepositResponse from(TimeDepositDetails details) {
        TimeDeposit deposit = details.deposit();
        return new TimeDepositResponse(
                deposit.getId(),
                deposit.getPlanType(),
                deposit.getBalance(),
                deposit.getDays(),
                details.withdrawals().stream().map(WithdrawalResponse::from).toList());
    }
}

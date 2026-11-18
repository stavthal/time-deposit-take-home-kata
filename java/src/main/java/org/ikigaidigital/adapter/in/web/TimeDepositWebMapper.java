package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.adapter.in.web.model.TimeDepositDto;
import org.ikigaidigital.adapter.in.web.model.WithdrawalDto;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;

/** Converts the domain read model into the DTOs generated from openapi.yaml. */
final class TimeDepositWebMapper {

    private TimeDepositWebMapper() {
    }

    static TimeDepositDto toDto(TimeDepositDetails details) {
        TimeDeposit deposit = details.deposit();
        return new TimeDepositDto(
                deposit.getId(),
                deposit.getPlanType(),
                deposit.getBalance(),
                deposit.getDays(),
                details.withdrawals().stream().map(TimeDepositWebMapper::toDto).toList());
    }

    static WithdrawalDto toDto(Withdrawal withdrawal) {
        return new WithdrawalDto(withdrawal.id(), withdrawal.amount(), withdrawal.date());
    }
}

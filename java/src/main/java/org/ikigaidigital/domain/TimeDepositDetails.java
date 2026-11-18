package org.ikigaidigital.domain;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/**
 * Read model for listing deposits: a deposit and its withdrawals.
 * Wraps {@link TimeDeposit} so the frozen shared class needs no new field.
 */
public record TimeDepositDetails(TimeDeposit deposit, List<Withdrawal> withdrawals) {
}

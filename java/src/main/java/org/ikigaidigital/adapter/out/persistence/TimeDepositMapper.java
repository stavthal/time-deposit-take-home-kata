package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts between JPA entities and the domain.
 * The database keeps money as DECIMAL(19,2); the frozen {@link TimeDeposit} keeps a Double.
 */
final class TimeDepositMapper {

    private TimeDepositMapper() {
    }

    static TimeDeposit toDomain(TimeDepositEntity entity) {
        return new TimeDeposit(entity.getId(), entity.getPlanType(), toDouble(entity.getBalance()), entity.getDays());
    }

    static TimeDepositDetails toDetails(TimeDepositEntity entity) {
        return new TimeDepositDetails(
                toDomain(entity),
                entity.getWithdrawals().stream().map(TimeDepositMapper::toDomain).toList());
    }

    static Withdrawal toDomain(WithdrawalEntity entity) {
        return new Withdrawal(entity.getId(), entity.getAmount(), entity.getDate());
    }

    static Double toDouble(BigDecimal amount) {
        return amount.doubleValue();
    }

    // Assumption: a balance is stored with 2 decimals, HALF_UP, to match DECIMAL(19,2).
    // valueOf uses the shortest decimal form of the double, so 1235595.8100000001 is stored as 1235595.81.
    static BigDecimal toDecimal(Double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}

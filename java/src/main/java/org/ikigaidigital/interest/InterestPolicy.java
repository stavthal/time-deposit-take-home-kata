package org.ikigaidigital.interest;

import org.ikigaidigital.TimeDeposit;

/**
 * Interest rule for one plan type.
 * The calculator calls it only for deposits past the 30 day grace period.
 */
public interface InterestPolicy {
    double monthlyInterest(TimeDeposit deposit);
}

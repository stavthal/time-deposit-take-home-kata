package org.ikigaidigital.interest;

import org.ikigaidigital.TimeDeposit;

public class BasicInterestPolicy implements InterestPolicy {
    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        return deposit.getBalance() * 0.01 / 12;
    }
}

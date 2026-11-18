package org.ikigaidigital.interest;

import org.ikigaidigital.TimeDeposit;

public class PremiumInterestPolicy implements InterestPolicy {
    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() > 45) {
            return deposit.getBalance() * 0.05 / 12;
        }
        return 0;
    }
}

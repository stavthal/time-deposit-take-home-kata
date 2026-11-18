package org.ikigaidigital.interest;

import org.ikigaidigital.TimeDeposit;

public class StudentInterestPolicy implements InterestPolicy {
    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() < 366) {
            return deposit.getBalance() * 0.03 / 12;
        }
        return 0;
    }
}

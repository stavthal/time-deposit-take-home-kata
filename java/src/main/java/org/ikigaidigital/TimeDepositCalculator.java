package org.ikigaidigital;

import org.ikigaidigital.interest.BasicInterestPolicy;
import org.ikigaidigital.interest.InterestPolicy;
import org.ikigaidigital.interest.PremiumInterestPolicy;
import org.ikigaidigital.interest.StudentInterestPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class TimeDepositCalculator {
    // Assumption: a plan type with no policy earns zero interest, as in the original if/else chain.
    private static final InterestPolicy NO_INTEREST = deposit -> 0;
    private static final Map<String, InterestPolicy> POLICIES = Map.of(
            "basic", new BasicInterestPolicy(),
            "student", new StudentInterestPolicy(),
            "premium", new PremiumInterestPolicy());

    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            double interest = 0;

            if (deposit.getDays() > 30) {
                interest += POLICIES.getOrDefault(deposit.getPlanType(), NO_INTEREST)
                        .monthlyInterest(deposit);
            }

            double newBalance = deposit.getBalance() + (new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)).doubleValue();
            deposit.setBalance(newBalance);
        }
    }
}

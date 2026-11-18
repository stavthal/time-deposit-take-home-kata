package org.ikigaidigital;

import org.ikigaidigital.interest.BasicInterestPolicy;
import org.ikigaidigital.interest.InterestPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class TimeDepositCalculator {
    private static final InterestPolicy NO_INTEREST = deposit -> 0;
    private static final Map<String, InterestPolicy> POLICIES = Map.of(
            "basic", new BasicInterestPolicy());

    public void updateBalance(List<TimeDeposit> xs) {
        for (int i = 0; i < xs.size(); i++) {
            double interest = 0;

            if (xs.get(i).getDays() > 30) {
                if (xs.get(i).getPlanType().equals("student")) {
                    if (xs.get(i).getDays() < 366) {
                        interest += xs.get(i).getBalance() * 0.03 / 12;
                    }
                } else if (xs.get(i).getPlanType().equals("premium")) {
                    if (xs.get(i).getDays() > 45) {
                        interest += xs.get(i).getBalance() * 0.05 / 12;
                    }
                } else {
                    interest += POLICIES.getOrDefault(xs.get(i).getPlanType(), NO_INTEREST)
                            .monthlyInterest(xs.get(i));
                }
            }

            double a2d = xs.get(i).getBalance() + (new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)).doubleValue();
            xs.get(i).setBalance(a2d);
        }
    }
}

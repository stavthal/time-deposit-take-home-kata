package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timeDeposits")
class TimeDepositEntity {

    // Assumption: ids are assigned by the data, not generated. No endpoint creates deposits.
    @Id
    private Integer id;

    private String planType;

    private Integer days;

    private BigDecimal balance;

    @OneToMany(mappedBy = "timeDeposit")
    @OrderBy("id")
    private List<WithdrawalEntity> withdrawals = new ArrayList<>();

    protected TimeDepositEntity() {
    }

    Integer getId() {
        return id;
    }

    String getPlanType() {
        return planType;
    }

    Integer getDays() {
        return days;
    }

    BigDecimal getBalance() {
        return balance;
    }

    void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    List<WithdrawalEntity> getWithdrawals() {
        return withdrawals;
    }
}

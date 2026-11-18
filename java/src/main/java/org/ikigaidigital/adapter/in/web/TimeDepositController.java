package org.ikigaidigital.adapter.in.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import org.ikigaidigital.application.port.in.GetAllTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The only two endpoints of the service. */
@OpenAPIDefinition(info = @Info(title = "XA Bank Time Deposit API", version = "1.0"))
@RestController
@RequestMapping("/time-deposits")
class TimeDepositController {

    private final UpdateAllBalancesUseCase updateAllBalances;
    private final GetAllTimeDepositsUseCase getAllTimeDeposits;

    TimeDepositController(UpdateAllBalancesUseCase updateAllBalances, GetAllTimeDepositsUseCase getAllTimeDeposits) {
        this.updateAllBalances = updateAllBalances;
        this.getAllTimeDeposits = getAllTimeDeposits;
    }

    // Assumption: each call applies one month of interest, so it is not idempotent. POST fits that.
    // 204 because the new balances are read with GET.
    @Operation(summary = "Apply one month of interest to every time deposit")
    @PostMapping("/balance-updates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateAllBalances() {
        updateAllBalances.updateAllBalances();
    }

    @Operation(summary = "List every time deposit with its withdrawals")
    @GetMapping
    List<TimeDepositResponse> getAllTimeDeposits() {
        return getAllTimeDeposits.getAllTimeDeposits().stream().map(TimeDepositResponse::from).toList();
    }
}

package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.adapter.in.web.api.TimeDepositsApi;
import org.ikigaidigital.adapter.in.web.model.TimeDepositDto;
import org.ikigaidigital.application.port.in.GetAllTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The only two endpoints of the service. Paths and status codes come from
 * {@link TimeDepositsApi}, generated from src/main/resources/openapi.yaml.
 */
// Assumption: Swagger UI and the OpenAPI spec pages are docs, not endpoints. No Actuator.
@RestController
class TimeDepositController implements TimeDepositsApi {

    private final UpdateAllBalancesUseCase updateAllBalances;
    private final GetAllTimeDepositsUseCase getAllTimeDeposits;

    TimeDepositController(UpdateAllBalancesUseCase updateAllBalances, GetAllTimeDepositsUseCase getAllTimeDeposits) {
        this.updateAllBalances = updateAllBalances;
        this.getAllTimeDeposits = getAllTimeDeposits;
    }

    // Assumption: each call applies one month of interest, so it is not idempotent. POST fits that.
    // 204 because the new balances are read with GET.
    @Override
    public ResponseEntity<Void> updateAllBalances() {
        updateAllBalances.updateAllBalances();
        return ResponseEntity.noContent().build();
    }

    // Assumption: withdrawals are a list of {id, amount, date}, so no withdrawal field is lost.
    @Override
    public ResponseEntity<List<TimeDepositDto>> getAllTimeDeposits() {
        return ResponseEntity.ok(getAllTimeDeposits.getAllTimeDeposits().stream().map(TimeDepositWebMapper::toDto).toList());
    }
}

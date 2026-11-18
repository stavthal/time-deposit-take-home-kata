package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.app.TimeDepositApplication;
import org.ikigaidigital.application.port.in.GetAllTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.domain.TimeDepositDetails;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web slice with hand written fake use cases. No database. */
@WebMvcTest(TimeDepositController.class)
@ContextConfiguration(classes = TimeDepositApplication.class)
@Import(TimeDepositControllerTest.FakeUseCases.class)
class TimeDepositControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AtomicInteger updateCalls;

    @Test
    void postRunsTheUpdateOnceAndReturnsNoContent() throws Exception {
        int before = updateCalls.get();

        mockMvc.perform(post("/time-deposits/balance-updates"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(updateCalls.get()).isEqualTo(before + 1);
    }

    @Test
    void getReturnsTheReadmeSchema() throws Exception {
        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"id": 1, "planType": "basic", "balance": 1000.83, "days": 31,
                           "withdrawals": [{"id": 10, "amount": 50.25, "date": "2026-10-01"}]},
                          {"id": 2, "planType": "student", "balance": 500.0, "days": 40, "withdrawals": []}
                        ]""", true));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeUseCases {

        @Bean
        AtomicInteger updateCalls() {
            return new AtomicInteger();
        }

        @Bean
        UpdateAllBalancesUseCase updateAllBalancesUseCase(AtomicInteger updateCalls) {
            return updateCalls::incrementAndGet;
        }

        @Bean
        GetAllTimeDepositsUseCase getAllTimeDepositsUseCase() {
            return () -> List.of(
                    new TimeDepositDetails(new TimeDeposit(1, "basic", 1000.83, 31),
                            List.of(new Withdrawal(10, new BigDecimal("50.25"), LocalDate.of(2026, 10, 1)))),
                    new TimeDepositDetails(new TimeDeposit(2, "student", 500.0, 40), List.of()));
        }
    }
}

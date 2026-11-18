package org.ikigaidigital.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.ikigaidigital.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Both endpoints end to end on the V2 seed data. */
@PostgresIntegrationTest
class TimeDepositApiIT {

    @Autowired
    private MockMvc mockMvc;

    // Strict: fails on a missing, extra or renamed field, and on a wrong order.
    @Test
    void getReturnsTheSeedDataWithWithdrawals() throws Exception {
        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"id": 1, "planType": "basic", "balance": 1000.00, "days": 30, "withdrawals": []},
                          {"id": 2, "planType": "basic", "balance": 1000.00, "days": 31, "withdrawals": [
                            {"id": 1, "amount": 100.00, "date": "2026-09-15"},
                            {"id": 2, "amount": 50.25, "date": "2026-10-01"}]},
                          {"id": 3, "planType": "student", "balance": 1000.00, "days": 365, "withdrawals": [
                            {"id": 3, "amount": 20.00, "date": "2026-10-20"}]},
                          {"id": 4, "planType": "student", "balance": 1000.00, "days": 366, "withdrawals": []},
                          {"id": 5, "planType": "premium", "balance": 1000.00, "days": 45, "withdrawals": []},
                          {"id": 6, "planType": "premium", "balance": 1000.00, "days": 46, "withdrawals": []},
                          {"id": 7, "planType": "basic", "balance": 1234567.00, "days": 45, "withdrawals": [
                            {"id": 4, "amount": 10000.00, "date": "2026-11-02"}]}
                        ]""", true));
    }

    // One earning deposit per plan. Boundary days are pinned in TimeDepositCalculatorTest.
    @Test
    void postAppliesOneMonthOfInterestThenGetShowsIt() throws Exception {
        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());

        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                // basic: 1234567.00 * 0.01 / 12 = 1028.8058 -> +1028.81
                .andExpect(jsonPath("$[?(@.id == 7)].balance").value(1235595.81))
                // student: 1000.00 * 0.03 / 12 = 2.50
                .andExpect(jsonPath("$[?(@.id == 3)].balance").value(1002.5))
                // premium: 1000.00 * 0.05 / 12 = 4.1666 -> +4.17
                .andExpect(jsonPath("$[?(@.id == 6)].balance").value(1004.17));
    }

    @Test
    void handWrittenContractHasExactlyTheTwoEndpoints() throws Exception {
        String body = mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(operations(new YAMLMapper().readTree(body)))
                .containsExactlyInAnyOrder("get /time-deposits", "post /time-deposits/balance-updates");
    }

    // Fails if a route is added in code without being added to openapi.yaml.
    @Test
    void runningAppServesExactlyTheTwoEndpoints() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(operations(new ObjectMapper().readTree(body)))
                .containsExactlyInAnyOrder("get /time-deposits", "post /time-deposits/balance-updates");
    }

    @Test
    void docsPathOpensSwaggerUi() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
    }

    @Test
    void swaggerUiShowsTheHandWrittenContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/openapi.yaml"));
    }

    private static List<String> operations(JsonNode spec) {
        List<String> operations = new ArrayList<>();
        spec.get("paths").fields().forEachRemaining(path ->
                path.getValue().fieldNames().forEachRemaining(verb -> operations.add(verb + " " + path.getKey())));
        return operations;
    }
}

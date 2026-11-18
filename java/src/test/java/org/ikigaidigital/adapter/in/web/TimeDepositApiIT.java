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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Both endpoints end to end on the V2 seed data. */
@PostgresIntegrationTest
class TimeDepositApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getReturnsTheSeedDataWithWithdrawals() throws Exception {
        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[1].planType").value("basic"))
                .andExpect(jsonPath("$[1].balance").value(1000.0))
                .andExpect(jsonPath("$[1].days").value(31))
                .andExpect(jsonPath("$[1].withdrawals.length()").value(2))
                .andExpect(jsonPath("$[1].withdrawals[1].amount").value(50.25))
                .andExpect(jsonPath("$[1].withdrawals[1].date").value("2026-10-01"))
                .andExpect(jsonPath("$[0].withdrawals").isEmpty());
    }

    @Test
    void postAppliesOneMonthOfInterestThenGetShowsIt() throws Exception {
        mockMvc.perform(post("/time-deposits/balance-updates")).andExpect(status().isNoContent());

        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                // basic 30: none; basic 31: +0.83; student 365: +2.50; student 366: none
                .andExpect(jsonPath("$[0].balance").value(1000.0))
                .andExpect(jsonPath("$[1].balance").value(1000.83))
                .andExpect(jsonPath("$[2].balance").value(1002.5))
                .andExpect(jsonPath("$[3].balance").value(1000.0))
                // premium 45: none; premium 46: +4.17; basic 1234567.00: +1028.81
                .andExpect(jsonPath("$[4].balance").value(1000.0))
                .andExpect(jsonPath("$[5].balance").value(1004.17))
                .andExpect(jsonPath("$[6].balance").value(1235595.81));
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

package org.ikigaidigital.app;

import org.ikigaidigital.PostgresTestcontainer;
import org.springframework.boot.SpringApplication;

/** Runs the app against a Testcontainers Postgres: {@code mvn spring-boot:test-run}. Needs only Docker. */
public class TestTimeDepositApplication {

    public static void main(String[] args) {
        SpringApplication.from(TimeDepositApplication::main).with(PostgresTestcontainer.class).run(args);
    }
}

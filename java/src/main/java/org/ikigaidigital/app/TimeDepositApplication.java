package org.ikigaidigital.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Wires the adapters around the core. Lives outside the core package so the core stays Spring free. */
@SpringBootApplication(scanBasePackages = "org.ikigaidigital")
public class TimeDepositApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeDepositApplication.class, args);
    }
}

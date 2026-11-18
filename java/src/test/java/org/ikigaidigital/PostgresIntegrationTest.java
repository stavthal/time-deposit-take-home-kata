package org.ikigaidigital;

import org.ikigaidigital.app.TimeDepositApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Full app on a Testcontainers Postgres. Each test rolls back.
 * One shared setup, so Spring reuses one context and one container for every IT.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = TimeDepositApplication.class, properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(PostgresTestcontainer.class)
@Transactional
public @interface PostgresIntegrationTest {
}

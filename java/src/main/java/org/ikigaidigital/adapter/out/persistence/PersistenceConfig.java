package org.ikigaidigital.adapter.out.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** The startup class is not above this package, so entities and repositories are registered here. */
@Configuration(proxyBeanMethods = false)
@EntityScan
@EnableJpaRepositories
class PersistenceConfig {
}

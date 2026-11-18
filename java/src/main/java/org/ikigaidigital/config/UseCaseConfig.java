package org.ikigaidigital.config;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.GetAllTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.application.service.GetAllTimeDepositsService;
import org.ikigaidigital.application.service.UpdateAllBalancesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the Spring free use case services as beans. */
@Configuration(proxyBeanMethods = false)
class UseCaseConfig {

    @Bean
    TimeDepositCalculator timeDepositCalculator() {
        return new TimeDepositCalculator();
    }

    @Bean
    UpdateAllBalancesUseCase updateAllBalancesUseCase(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        return new UpdateAllBalancesService(repository, calculator);
    }

    @Bean
    GetAllTimeDepositsUseCase getAllTimeDepositsUseCase(TimeDepositRepository repository) {
        return new GetAllTimeDepositsService(repository);
    }
}

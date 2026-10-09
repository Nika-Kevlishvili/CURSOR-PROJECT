package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.product.termination.serviceContract.ServiceContractTerminationManager;
import bg.energo.phoenix.service.contract.product.termination.serviceContract.ServiceContractTerminator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ServiceContractTerminationScheduler {
    private final List<ServiceContractTerminator> serviceContractTerminators;
    private final ServiceContractTerminationManager serviceContractTerminationManager;

    @Scheduled(cron = "${termination.job.service-contract.cron}")
    @ExecutionTimeLogger(value = "ServiceContractTerminationScheduler")
    public void start() throws InterruptedException {
        for (ServiceContractTerminator productContractTerminator : serviceContractTerminators) {
            serviceContractTerminationManager.processServiceContractsForTermination(productContractTerminator);
        }
    }
}


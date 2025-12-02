package bg.energo.phoenix.service.contract.product.termination.serviceContract;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev","test"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ServiceContractTerminationScheduler {
    private final List<ServiceContractTerminator> serviceContractTerminators;
    private final ServiceContractTerminationManager serviceContractTerminationManager;

    //    @Scheduled
    public void start() throws InterruptedException {
        for (ServiceContractTerminator productContractTerminator : serviceContractTerminators) {
            serviceContractTerminationManager.processServiceContractsForTermination(productContractTerminator);
        }
    }
}


package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.product.termination.ProductContractTerminationManager;
import bg.energo.phoenix.service.contract.product.termination.ProductContractTerminator;
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
public class ProductContractTerminationScheduler {
    private final List<ProductContractTerminator> productContractTerminators;
    private final ProductContractTerminationManager terminatorManager;

    @Scheduled(cron = "${termination.job.product-contract.cron}")
    @ExecutionTimeLogger(value = "ProductContractTerminationScheduler")
    public void start() throws InterruptedException {
        log.info("Starting product contract termination scheduler");
        for (ProductContractTerminator productContractTerminator : productContractTerminators) {
            terminatorManager.processContractsForTermination(productContractTerminator);
        }
    }

}

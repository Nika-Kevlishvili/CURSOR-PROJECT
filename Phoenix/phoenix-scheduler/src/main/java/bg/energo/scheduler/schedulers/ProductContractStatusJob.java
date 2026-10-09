package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.product.ProductContractService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ProductContractStatusJob {
    private final ProductContractService productContractService;

//    @PostConstruct
//    public void init() {
//        new Thread(this::work).start();
//    }
    @Scheduled(cron = "0 0 0 * * *")
    @ExecutionTimeLogger(value = "ProductContractStatusJob")
    public void work() {
        productContractService.updateProductContractsFromSchedulerJob();
    }
}

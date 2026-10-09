package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.product.ProductContractFilesService;
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
public class ProductContractFileCleanerService {
    private final ProductContractFilesService productContractFilesService;

    @Scheduled(cron = "${product-contract.file.cleaner.cron}")
    @ExecutionTimeLogger(value = "ProductContractFileCleanerService")
    public void cleanProductContractFiles() {
        try {
            log.debug("Starting cleaning product contract files");
            productContractFilesService.cleanupOutDatedFiles();
        } catch (Exception e) {
            log.error("Exception handled while trying to cleanup product contract files");
        }
    }
}

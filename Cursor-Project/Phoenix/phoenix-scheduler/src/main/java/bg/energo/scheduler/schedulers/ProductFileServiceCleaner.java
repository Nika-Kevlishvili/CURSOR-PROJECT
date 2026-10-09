package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.product.product.ProductFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ProductFileServiceCleaner {

    private final ProductFileService productFileService;

    @Scheduled(cron = "${product.file.cleaner.cron}")
    @ExecutionTimeLogger(value = "ProductFileServiceCleaner")
    public void cleanProductFileData() {
        try {
            log.debug("Starting cleaning of product files tables");
            productFileService.cleanupProductFileData();

        } catch (Exception e) {
            log.error("Error while deleting product files", e);
        }
    }
}

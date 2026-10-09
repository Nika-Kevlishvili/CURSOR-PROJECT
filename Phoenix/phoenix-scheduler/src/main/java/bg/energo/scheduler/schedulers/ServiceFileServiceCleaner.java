package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.product.service.ServiceFileService;
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
public class ServiceFileServiceCleaner {
    private final ServiceFileService serviceFileService;

    @Scheduled(cron = "${service.file.cleaner.cron}")
    @ExecutionTimeLogger(value = "ServiceFileServiceCleaner")
    public void cleanServiceFiles() {
        try {
            log.debug("Starting cleaning service files");
            serviceFileService.cleanupOutDatedServiceFiles();
        } catch (Exception e) {
            log.error("Error while deleting service files");
        }
    }
}

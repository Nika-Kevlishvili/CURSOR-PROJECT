package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.activity.SystemActivityFileService;
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
public class SystemActivityFileCleaner {
    private final SystemActivityFileService systemActivityFileService;

    @Scheduled(cron = "${system.activity.file.cleaner.cron}")
    @ExecutionTimeLogger(value = "SystemActivityFileCleaner")
    public void cleanSystemActivityFiles() {
        try {
            log.debug("Starting cleaning system activity files");
            systemActivityFileService.cleanupOutDatedFiles();
        } catch (Exception e) {
            log.error("Error while deleting system activity files.", e);
        }
    }
}

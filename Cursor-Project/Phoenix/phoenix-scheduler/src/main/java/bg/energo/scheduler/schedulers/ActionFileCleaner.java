package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.action.file.ActionFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@RequiredArgsConstructor
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ActionFileCleaner {
    private final ActionFileService actionFileService;

    @Scheduled(cron = "${action.file.cleaner.cron}")
    @ExecutionTimeLogger(value = "ActionFileCleaner")
    public void cleanActionFiles() {
        try {
            log.debug("Starting cleaning action files");
            actionFileService.cleanupOutdatedFiles();
        } catch (Exception e) {
            log.error("Error while deleting action files.", e);
        }
    }
}

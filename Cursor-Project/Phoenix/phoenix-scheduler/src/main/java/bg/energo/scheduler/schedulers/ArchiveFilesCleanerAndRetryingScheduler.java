package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.archivation.edms.ArchiveFilesCleanerAndRetryingService;
import bg.energo.phoenix.service.archivation.edms.JobStatusService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ArchiveFilesCleanerAndRetryingScheduler {
    private final ArchiveFilesCleanerAndRetryingService archiveFilesCleanerAndRetryingService;
    private final JobStatusService jobStatusService;


    @Scheduled(cron = "@daily")
    @ExecutionTimeLogger(value = "ArchiveFilesCleanerAndRetryingScheduler")
    public void cleanupArchivedFiles() {
        archiveFilesCleanerAndRetryingService.cleanupArchivedFiles();
    }

    @Scheduled(cron = "@daily")
    @ExecutionTimeLogger(value = "ArchiveFilesCleanerAndRetryingScheduler")
    public void retryArchivingFiles() {

        while (!jobStatusService.isPreviousJobCompleted()) {
            log.info("Previous retryArchivingFiles job is still running.");
            try {
                TimeUnit.MINUTES.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Thread interrupted while waiting.");
                return;
            }
        }

        log.info("Previous job completed. Starting today's retryArchivingFiles job.");
        jobStatusService.markJobAsRunning();

        try {
            archiveFilesCleanerAndRetryingService.retryArchivingFiles();
        } finally {
            jobStatusService.markJobAsCompleted();
        }
    }


}

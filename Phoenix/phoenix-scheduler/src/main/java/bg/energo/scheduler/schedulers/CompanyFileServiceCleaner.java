package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.billing.companyDetails.CompanyDetailFileService;
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
public class CompanyFileServiceCleaner {
    private final CompanyDetailFileService companyDetailFileService;

    @Scheduled(cron = "${service.file.cleaner.cron}")
    @ExecutionTimeLogger(value = "CompanyFileServiceCleaner")
    public void cleanServiceFiles() {
        try {
            log.debug("Starting cleaning company files");
            companyDetailFileService.cleanupCompanyFileData();
        } catch (Exception e) {
            log.error("Error while deleting company files");
        }
    }
}

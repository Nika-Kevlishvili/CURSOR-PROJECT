package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.order.service.ServiceOrderProxyFileService;
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
public class ServiceOrderProxyFileCleaner {
    private final ServiceOrderProxyFileService serviceOrderProxyFileService;

    @Scheduled(cron = "${service.order.proxy.file.cleaner.cron}")
    @ExecutionTimeLogger(value = "ServiceOrderProxyFileCleaner")
    public void cleanSystemActivityFiles() {
        try {
            log.debug("Starting cleaning service order proxy files");
            serviceOrderProxyFileService.cleanupOutDatedFiles();
        } catch (Exception e) {
            log.error("Error while deleting service order proxy files.", e);
        }
    }
}

package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.signing.qes.QesSigningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
//@Profile({"dev", "test"})
//@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
@Slf4j
public class QesScheduler {

    private final QesSigningService qesSigningService;

    @Scheduled(fixedDelayString = "${qes.move.job.delay}",timeUnit = TimeUnit.SECONDS)
    @ExecutionTimeLogger(value = "QesScheduler")
    public void moveFromShared(){
        log.debug("StartedQesScheduler");
        qesSigningService.moveFromShared();
    }

    @Scheduled(cron = "${qes.delete.job.cron}")
    @ExecutionTimeLogger(value = "QesScheduler")
    public void deleteUntrackedFolders() {
        log.debug("RunningDeleteTrackedFolders {}", LocalDateTime.now());
        qesSigningService.deleteUntrackedFolders();
    }
}

package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.crm.smsCommunication.SmsCommunicationStatusCheckJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
@RequiredArgsConstructor
public class SmsCommunicationStatusCheckJob {
    private final SmsCommunicationStatusCheckJobService smsCommunicationStatusCheckJobService;

    @Scheduled(fixedDelay = 300000)
    @ExecutionTimeLogger(value = "SmsCommunicationStatusCheckJob")
    public void execute() {
        log.info("Starting SMS communication status check job");
        smsCommunicationStatusCheckJobService.executeSmsStatusCheck();
    }

    @Scheduled(fixedDelay = 300000)
    @ExecutionTimeLogger(value = "SmsCommunicationResendJob")
    public void resend() {
        log.info("Starting SMS communication resend job");
        smsCommunicationStatusCheckJobService.executeSmsResend();
    }

    @Scheduled(fixedDelay = 3600000)
    @ExecutionTimeLogger(value = "SmsCommunicationTimeoutJob")
    public void expireTimedOut() {
        log.info("Starting SMS communication timeout-expiry job");
        smsCommunicationStatusCheckJobService.updateTimedOutSmsCommunications();
    }
}

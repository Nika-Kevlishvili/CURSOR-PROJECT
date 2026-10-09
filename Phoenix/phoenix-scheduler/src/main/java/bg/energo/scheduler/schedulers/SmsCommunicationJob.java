package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.crm.smsCommunication.SmsCommunicationJobService;
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
public class SmsCommunicationJob {
    private final SmsCommunicationJobService smsCommunicationJobService;

    @Scheduled(fixedDelay = 60000)
    @ExecutionTimeLogger(value = "SmsCommunicationJob")
    public void execute() {
        log.info("Starting mass-sms generation process");
        smsCommunicationJobService.generateBody();
    }
}

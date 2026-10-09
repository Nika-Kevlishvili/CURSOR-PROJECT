package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
//@Profile({"dev", "test"})
//@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class PowerSupplyDisconnectionReminderJobService {
    private final PowerSupplyDisconnectionReminderService powerSupplyDisconnectionReminderService;

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    @ExecutionTimeLogger(value = "PowerSupplyDisconnectionReminderJobService")
    public void start() {
        powerSupplyDisconnectionReminderService.documentGenerationJob();
    }

    // EMAIL_SMS_ON_PAPER cascade: the delivery-report delays (receivable.rfd_delay) are applied by the jobs' query
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    @ExecutionTimeLogger(value = "PowerSupplyDisconnectionReminderEmailInProgressJob")
    public void emailInProgress() {
        powerSupplyDisconnectionReminderService.emailInProgressJob();
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    @ExecutionTimeLogger(value = "PowerSupplyDisconnectionReminderSmsInProgressJob")
    public void smsInProgress() {
        powerSupplyDisconnectionReminderService.smsInProgressJob();
    }

}

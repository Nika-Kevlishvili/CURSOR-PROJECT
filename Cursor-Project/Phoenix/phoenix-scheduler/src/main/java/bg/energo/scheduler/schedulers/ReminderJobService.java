package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.event.Event;
import bg.energo.phoenix.event.EventFactory;
import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.process.model.entity.Process;
import bg.energo.phoenix.repository.receivable.reminder.ReminderRepository;
import bg.energo.phoenix.service.receivable.reminder.ReminderJobProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@RequiredArgsConstructor
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ReminderJobService {

    private final ReminderRepository reminderRepository;
    private final EventFactory eventFactory;
    private final ReminderJobProxyService reminderJobProxyService;



    @Scheduled(fixedRate = 5 * 60 * 1000)
    @ExecutionTimeLogger(value = "ReminderJobService")
    public void start() {
        log.debug("Start reminder job");
        reminderJobProxyService.job();
        log.debug("End reminder job");
    }


    private Event getProcessCreatedEvent(Process process) {
        return eventFactory.createProcessCreatedEvent(EventType.REMINDER_PROCESS, process);
    }

    public Set<Long> checkByPeriodicity() {
        return reminderRepository.findAllReminderWitchShouldBeRun();
    }

}

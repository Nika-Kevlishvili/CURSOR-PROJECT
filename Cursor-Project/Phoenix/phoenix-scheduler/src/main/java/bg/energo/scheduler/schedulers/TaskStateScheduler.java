package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.task.TaskStateUpdaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class TaskStateScheduler {
    private final TaskStateUpdaterService taskStateUpdaterService;

//    @PostConstruct
//    public void init() {
//        new Thread(this::work).start();
//    }

    @Scheduled(cron = "0 0 0 * * *")
    @ExecutionTimeLogger(value = "TaskStateScheduler")
    public void work() {
        Runnable runnable = () -> {
            taskStateUpdaterService.update();
            taskStateUpdaterService.findExpiredJobs();
            taskStateUpdaterService.overdueEveryDay();
        };
        new Thread(runnable).start();
    }
}

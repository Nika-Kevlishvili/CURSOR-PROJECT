package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.action.ActionService;
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
public class ActionStatusTransitionScheduledService {

    private final ActionService actionService;

    @Scheduled(cron = "${action.status.transition.cron}")
    @ExecutionTimeLogger(value = "ActionStatusTransitionScheduledService")
    public void transitionActionStatus() {
        log.debug("Starting transition action status job");
        actionService.transitionStatusOfEligibleActionsToExecuted();
    }

}

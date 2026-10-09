package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.action.calculation.ActionPenaltyCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@RequiredArgsConstructor
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ActionPenaltyCalculationScheduledService {

    private final ActionPenaltyCalculationService actionPenaltyCalculationService;

    @Value("${action.penalty.calculation.number-of-threads}")
    private Integer numberOfThreads;

    @Value("${action.penalty.calculation.query-batch-size}")
    private Integer queryBatchSize;

    @Value("${action.penalty.calculation.batch-size}")
    private Integer batchSize;

    @Scheduled(cron = "${action.penalty.calculation.cron}")
    @ExecutionTimeLogger(value = "ActionPenaltyCalculationScheduledService")
    public void calculateActionPenalties() {
        log.debug("Starting calculating action penalty job");
        actionPenaltyCalculationService.calculatePenaltiesForEligibleActions(
                numberOfThreads,
                queryBatchSize,
                batchSize
        );
    }
}

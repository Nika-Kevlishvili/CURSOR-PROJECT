package bg.energo.phoenix.billing.scheduler;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.service.billing.billingRun.actions.startGeneration.BillingRunStartGenerationService;
import bg.energo.phoenix.utils.LoggerUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingRunStartGeneratingScheduler {
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private final BillingRunStartGenerationService generationService;
    private final BillingRunRepository billingRunRepository;

    @PostConstruct
    private void resumeNonFinishedProcesses() {
        log.debug(LoggerUtils.prettyLogging("Resuming non finished document generation processes"));
        new Thread(() -> {
            try {
                log.debug("Resuming non finished billing runs");
                List<BillingRun> unfinishedBillingRuns = billingRunRepository.findBillingRunByStatus(BillingStatus.IN_PROGRESS_GENERATION);
                log.debug("Found {} unfinished billing runs", unfinishedBillingRuns.size());

                for (BillingRun unfinishedBillingRun : unfinishedBillingRuns) {
                    executeAsynchronously(unfinishedBillingRun.getId());
                }
            } catch (Exception e) {
                log.error("Exception handled while trying finish generations");
            } finally {
                log.debug("Shutting down executor");
                executor.shutdown();
            }
        }).start();
    }

    private void executeAsynchronously(long billingRunId) {
        log.debug("Starting billing run {} generation", billingRunId);
        executor.submit(() -> generationService.execute(billingRunId, true));
    }
}

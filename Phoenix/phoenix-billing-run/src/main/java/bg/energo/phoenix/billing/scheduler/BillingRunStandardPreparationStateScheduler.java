package bg.energo.phoenix.billing.scheduler;

import bg.energo.phoenix.billingRun.generation.service.BillingRunStandardPreparationStateHandler;
import bg.energo.phoenix.utils.LoggerUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingRunStandardPreparationStateScheduler {
    private final BillingRunStandardPreparationStateHandler stateHandler;

    @PostConstruct
    private void resumeNonFinishedProcesses() {
        log.debug(LoggerUtils.prettyLogging("Resuming non finished billing runs"));
        try {
            ExecutorService processExecutor = Executors.newSingleThreadExecutor();
            processExecutor.submit(stateHandler::resumeNonFinishedProcesses);
            processExecutor.shutdown();
        } catch (Exception e) {
            log.error("Exception handled while trying to resume non finished processes", e);
        }
    }

    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
    public void stateListener() {
        log.debug(LoggerUtils.prettyLogging("Billing run state listener iteration started %s".formatted(Instant.now())));
        ExecutorService processExecutor = Executors.newSingleThreadExecutor();
        processExecutor.submit(stateHandler::finishStandardBillingProcessing);
        processExecutor.submit(stateHandler::startStandardBillingProcessing);
        processExecutor.shutdown();
    }
}

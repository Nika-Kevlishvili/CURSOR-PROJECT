package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.response.billing.billingRun.BillingWithStatusShortResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.service.billing.billingRun.actions.startBilling.BillingRunStartBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OneTimeBillingProcessInvokeService {
    private final BillingRunRepository billingRunRepository;
    private final BillingRunService billingRunService;
    private final BillingRunStartBillingService billingRunStartBillingService;
    @Value("${billing.start-one-time.number-of-threads}")
    private Integer numberOfThreads;

    /**
     * Processes one-time billings by retrieving them from the database, grouping them by status, and executing the appropriate actions for each status (starting, resuming, or pausing the billings).
     * This method is executed periodically to handle one-time billings.
     */
    @ExecutionTimeLogger
    public void process() {
        List<BillingWithStatusShortResponse> billings = billingRunRepository.getOneTimeBillingsFromPeriodicity();
        Map<BillingStatus, List<Long>> billingsGroupedByStatus = billings.stream()
                .collect(Collectors.groupingBy(
                        BillingWithStatusShortResponse::getStatus,
                        Collectors.mapping(BillingWithStatusShortResponse::getId, Collectors.toList())
                ));
        log.debug("billings retrieved: {}", billings.stream()
                .map(b -> String.format("[id: %s, status: %s]", b.getId(), b.getStatus()))
                .collect(Collectors.joining(", ")));
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        List<Callable<Boolean>> callables = getCallables(billingsGroupedByStatus);
        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            log.error("Error while invoking callables", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves a list of callable tasks based on the billing statuses in the provided map.
     * The tasks will either start, resume, or pause the billings depending on their status.
     *
     * @param billingsGroupedByStatus a map of billing IDs grouped by their status
     * @return a list of callable tasks to be executed
     */
    private List<Callable<Boolean>> getCallables(Map<BillingStatus, List<Long>> billingsGroupedByStatus) {
        List<Callable<Boolean>> callables = new ArrayList<>();
        for (Map.Entry<BillingStatus, List<Long>> b : billingsGroupedByStatus.entrySet()) {
            if (b.getKey().equals(BillingStatus.INITIAL)) {
                for (Long id : b.getValue()) {
                    Callable<Boolean> callableTask = () -> {
                        billingRunStartBillingService.execute(id, false, false);
                        log.debug("billing to be started: {}", id);
                        return true;
                    };
                    callables.add(callableTask);
                }
            } else if (b.getKey().equals(BillingStatus.PAUSED)) {
                for (Long id : b.getValue()) {
                    Callable<Boolean> callableTask = () -> {
                        billingRunService.resume(id, false);
                        log.debug("billing to be resumed: {}", id);
                        return true;
                    };
                    callables.add(callableTask);
                }
            } else if (List.of(BillingStatus.IN_PROGRESS_ACCOUNTING, BillingStatus.IN_PROGRESS_DRAFT, BillingStatus.IN_PROGRESS_GENERATION).contains(b.getKey())) {
                for (Long id : b.getValue()) {
                    Callable<Boolean> callableTask = () -> {
                        billingRunService.pause(id, false);
                        log.debug("billing to be paused: {}", id);
                        return true;
                    };
                    callables.add(callableTask);
                }
            }
        }
        return callables;
    }
}

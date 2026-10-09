package bg.energo.phoenix.billing.scheduler;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.billing.billingRun.OneTimeBillingProcessInvokeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OneTimeBillingRunScheduledService {
    private final OneTimeBillingProcessInvokeService oneTimeBillingProcessInvokeService;

    /**
     * Invokes the one-time billing process on a scheduled basis.
     * The billing process is invoked at a fixed delay specified by the `billing.start-one-time.delay` property,
     * with an initial delay specified by the `billing.start-one-time.initial-delay` property.
     * If any exceptions occur during the billing process invocation, they are logged with an error message.
     */
    @ExecutionTimeLogger
    @Scheduled(fixedDelayString = "${billing.start-one-time.delay}", initialDelayString = "${billing.start-one-time.initial-delay}")
    public void invokeOneTimeBillingsProcess() {
        try {
            oneTimeBillingProcessInvokeService.process();
        } catch (Exception e) {
            log.error("exception happened during billing process invoke: {}", e.getMessage());
        }
    }
}

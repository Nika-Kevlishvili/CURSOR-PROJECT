package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class InvoiceNumberGenerationJob {
    private final InvoiceNumberService invoiceNumberService;

    @Scheduled(fixedDelay = 15,timeUnit = TimeUnit.MINUTES)
    @ExecutionTimeLogger(value = "InvoiceNumberGenerationJob")
    public void saveNumbers(){
        MDC.put("scheduledId", UUID.randomUUID().toString());
        log.debug("RunningSaveMissedNumbers");
        invoiceNumberService.saveMissedNumbers();
        log.debug("FinishedSaveMissedNumbers");
    }
}

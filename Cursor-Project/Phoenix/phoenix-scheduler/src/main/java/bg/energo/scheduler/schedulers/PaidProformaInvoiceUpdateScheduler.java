package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.util.order.OrderInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class PaidProformaInvoiceUpdateScheduler {

    private final OrderInvoiceService orderInvoiceService;

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    @ExecutionTimeLogger(value = "PaidProformaInvoiceUpdateScheduler")
    public void execute() {
        try {
            log.debug("start checking liability amounts for proforma invoices");
            orderInvoiceService.checkAndUpdatePaidProformaInvoices();
        } catch (Exception e) {
            log.error("Error while updating paid proforma invoices: {}", e.getMessage());
        }
    }
}

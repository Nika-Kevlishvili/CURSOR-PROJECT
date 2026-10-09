package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.receivable.disconnectionOfPowerSupply.PaidReconnectionProformaInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * PHN-3752 Job N1: every 10 seconds convert paid reconnection proforma invoices to final invoices.
 * Sibling of {@link PaidProformaInvoiceUpdateScheduler} — does not modify order proforma conversion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class PaidReconnectionProformaInvoiceUpdateScheduler {

    private final PaidReconnectionProformaInvoiceService paidReconnectionProformaInvoiceService;

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    @ExecutionTimeLogger(value = "PaidReconnectionProformaInvoiceUpdateScheduler")
    public void execute() {
        try {
            log.debug("start checking liability amounts for reconnection proforma invoices");
            paidReconnectionProformaInvoiceService.checkAndUpdatePaidReconnectionProformaInvoices();
        } catch (Exception e) {
            log.error("Error while updating paid reconnection proforma invoices: {}", e.getMessage());
        }
    }
}

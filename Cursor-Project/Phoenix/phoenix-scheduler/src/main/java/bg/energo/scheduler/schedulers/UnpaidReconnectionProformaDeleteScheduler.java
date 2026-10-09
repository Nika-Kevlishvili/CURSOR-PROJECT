package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.receivable.disconnectionOfPowerSupply.UnpaidReconnectionProformaDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * PHN-3854 Job N2: daily at 21:30 Europe/Sofia hard-delete unpaid reconnection proformas
 * when Express is unchecked and the regular reconnection fee is fully paid.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "test", "preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class UnpaidReconnectionProformaDeleteScheduler {

    private final UnpaidReconnectionProformaDeleteService unpaidReconnectionProformaDeleteService;

    @Scheduled(cron = "${reconnection.proforma.delete.cron}", zone = "Europe/Sofia")
    @ExecutionTimeLogger(value = "UnpaidReconnectionProformaDeleteScheduler")
    public void execute() {
        try {
            log.debug("start deleting unpaid reconnection proforma invoices");
            unpaidReconnectionProformaDeleteService.deleteUnpaidReconnectionProformas();
        } catch (Exception e) {
            log.error("Error while deleting unpaid reconnection proforma invoices: {}", e.getMessage());
        }
    }
}

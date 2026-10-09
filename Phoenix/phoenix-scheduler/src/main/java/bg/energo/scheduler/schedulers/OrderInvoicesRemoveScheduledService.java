package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.order.goods.GoodsOrderProcessService;
import bg.energo.phoenix.service.contract.order.service.ServiceOrderProcessService;
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
public class OrderInvoicesRemoveScheduledService {

    private final GoodsOrderProcessService goodsOrderProcessService;
    private final ServiceOrderProcessService serviceOrderProcessService;

    /**
     * checks pro forma real invoices that are overdue for goods order,
     * updates order status as 'deleted',
     * deletes invoices from database.
     */
    @Scheduled(cron = "${goods.order.invoices.remover.cron}")
    @ExecutionTimeLogger(value = "OrderInvoicesRemoveScheduledService")
    public void checkOverdueInvoicesForGoodsOrders() {
        try {
            log.debug("start - check goods order overdue proforma invoices");
            goodsOrderProcessService.checkOverdueProformaInvoicesForOrders();
        } catch (Exception e) {
            log.error("error while removing goods order invoices: {}", e.getMessage());
        }
    }

    /**
     * checks pro forma real invoices that are overdue for service order,
     * updates order status as 'deleted',
     * deletes invoices from database.
     */
    @Scheduled(cron = "${service.order.invoices.remover.cron}")
    @ExecutionTimeLogger(value = "OrderInvoicesRemoveScheduledService")
    public void checkOverdueInvoicesForServiceOrders() {
        try {
            log.debug("start - check service order overdue proforma invoices");
            serviceOrderProcessService.checkOverdueProformaInvoicesForOrders();
        } catch (Exception e) {
            log.error("error while removing service order invoices: {}", e.getMessage());
        }
    }
}

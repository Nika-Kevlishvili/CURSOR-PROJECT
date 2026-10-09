package bg.energo.phoenix.billing.listener;

import bg.energo.phoenix.billingRun.generation.service.BillingRunStandardInvoiceGenerationService;
import bg.energo.phoenix.event.billing.BillingRunStartAccountingEvent;
import bg.energo.phoenix.event.billing.BillingRunStartGenerationEvent;
import bg.energo.phoenix.service.billing.billingRun.actions.startAccounting.BillingRunStartAccountingService;
import bg.energo.phoenix.service.billing.billingRun.actions.startGeneration.BillingRunStartGenerationService;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingRunListener {
    private final BillingRunStartGenerationService billingRunStartGenerationService;
    private final BillingRunStartAccountingService billingRunStartAccountingService;
    private final BillingRunStandardInvoiceGenerationService billingRunStandardInvoiceGenerationService;
    private final InvoiceReversalProcessService invoiceReversalProcessService;

    @RabbitListener(queues = "${rabbit.billing-run.generation.queue}")
    public void listenToStartGeneration(BillingRunStartGenerationEvent event) {
        log.debug("Received event: {}", event);
        billingRunStartGenerationService.execute(event.getBillingRunId(), event.isResumeProcess());
    }

    @RabbitListener(queues = "${rabbit.billing-run.accounting.queue}")
    public void listenToStartAccounting(BillingRunStartAccountingEvent event) {
        log.debug("Received event: {}", event);
        billingRunStartAccountingService.execute(event.getBillingRunId(), event.isResumeProcess());
    }


}

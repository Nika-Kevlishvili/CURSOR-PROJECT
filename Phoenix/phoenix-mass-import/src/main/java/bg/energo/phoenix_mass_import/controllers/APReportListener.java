package bg.energo.phoenix_mass_import.controllers;

import bg.energo.phoenix.event.accountingPeriod.AccountingPeriodReportEvent;
import bg.energo.phoenix.service.billing.accountingPeriods.AccountingPeriodReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class APReportListener {
    private final AccountingPeriodReportService accountingPeriodReportService;

    @RabbitListener(queues = "${rabbit.ap-report.queue}")
    public void listenToAccountingPeriodReportGeneration(AccountingPeriodReportEvent event) {
        log.debug("APReportListener: received report event! id: {}, previousGenerationFailed: {}", event.getAccountingPeriodId(), event.isGenerationFailed());
        accountingPeriodReportService.generateReportsAndUpdateStatus(event.getAccountingPeriodId(), event.isGenerationFailed());

    }
}

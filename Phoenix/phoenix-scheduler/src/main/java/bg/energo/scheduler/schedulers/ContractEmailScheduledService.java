package bg.energo.scheduler.schedulers;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.service.contract.AbstractContractEmailJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Profile({"dev", "test","preProd", "prod"})
@ConditionalOnExpression("${app.cfg.schedulers.enabled:true}")
public class ContractEmailScheduledService {
    @Qualifier("productContractEmailJobService")
    private final AbstractContractEmailJobService productContractEmailJobService;

    @Qualifier("serviceContractEmailJobService")
    private final AbstractContractEmailJobService serviceContractEmailJobService;

    public ContractEmailScheduledService(
            @Qualifier("productContractEmailJobService") AbstractContractEmailJobService productContractEmailJobService,
            @Qualifier("serviceContractEmailJobService") AbstractContractEmailJobService serviceContractEmailJobService) {
        this.productContractEmailJobService = productContractEmailJobService;
        this.serviceContractEmailJobService = serviceContractEmailJobService;
    }

    @Scheduled(fixedDelayString = "${contract-email.delay}",timeUnit = TimeUnit.MINUTES)
    @ExecutionTimeLogger(value = "ContractEmailScheduledService")
    public void execute() {
        log.debug("Starting contract email sending job");

        try {
            log.debug("Processing product contract emails");
            productContractEmailJobService.sendEmailsForAllSignedDocuments();
        } catch (Exception e) {
            log.error("Error processing product contract emails: {}", e.getMessage(), e);
        }

        try {
            log.debug("Processing service contract emails");
            serviceContractEmailJobService.sendEmailsForAllSignedDocuments();
        } catch (Exception e) {
            log.error("Error processing service contract emails: {}", e.getMessage(), e);
        }

        log.debug("Completed contract email sending job");
    }
}

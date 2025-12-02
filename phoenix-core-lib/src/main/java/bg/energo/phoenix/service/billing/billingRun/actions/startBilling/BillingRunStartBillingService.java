package bg.energo.phoenix.service.billing.billingRun.actions.startBilling;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingErrorDataRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.billing.billingRun.actions.AbstractBillingRunActionService;
import bg.energo.phoenix.service.billing.billingRun.actions.startGeneration.BillingRunStartGenerationService;
import bg.energo.phoenix.service.billing.invoice.InvoiceService;
import bg.energo.phoenix.service.notifications.enums.NotificationState;
import bg.energo.phoenix.service.notifications.enums.NotificationType;
import bg.energo.phoenix.service.notifications.events.NotificationEvent;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class BillingRunStartBillingService extends AbstractBillingRunActionService {
    private final InvoiceService invoiceService;
    private final BillingRunStartBillingInvokeService startBillingInvokeService;
    private final BillingRunStartGenerationService billingRunStartGenerationService;

    public BillingRunStartBillingService(PermissionService permissionService,
                                         BillingRunRepository billingRunRepository,
                                         BillingRunStartGenerationService billingRunStartGenerationService,
                                         NotificationEventPublisher notificationEventPublisher,
                                         BillingErrorDataRepository billingErrorDataRepository,
                                         InvoiceService invoiceService,
                                         BillingRunStartBillingInvokeService startBillingInvokeService) {
        super(permissionService, billingRunRepository, notificationEventPublisher, billingErrorDataRepository);
        this.billingRunStartGenerationService = billingRunStartGenerationService;
        this.invoiceService = invoiceService;
        this.startBillingInvokeService = startBillingInvokeService;
    }

    @Override
    public AbstractBillingRunActionService getNextJobInChain() {
        return billingRunStartGenerationService;
    }

    /**
     * Starts the billing process for a given billing run ID.
     *
     * @param billingRunId The ID of the billing run to start.
     * @throws DomainEntityNotFoundException if the billing run does not exist with the provided ID.
     * @throws ClientException               if the billing run is not in the 'INITIAL' status.
     */
    public void execute(Long billingRunId, boolean isResumeProcess, boolean mustCheckPermission) {
        log.debug("Starting billing run start billing process for billing run with id: {}", billingRunId);
        // finding billing run
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));

        if (!isResumeProcess) {
            // billing run must be in initial status to start billing run, else throw exception
            if (!Objects.equals(billingRun.getStatus(), BillingStatus.INITIAL)) {
                log.error("Billing run must be in status 'INITIAL' to start billing run process");
                throw new ClientException("Billing run must be in status 'INITIAL' to start billing run process", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

        if (mustCheckPermission && missingPermission(BillingPermissions.START, billingRun.getType())) {
            log.debug("Can't start billing run without permission");
            throw new ClientException("Can't start billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        log.debug("Setting billing run status to 'IN_PROGRESS_DRAFT'");
        billingRun.setStatus(BillingStatus.IN_PROGRESS_DRAFT);
        log.debug("Setting billing run process stage to 'DRAFT'");
        billingRun.setProcessStage(BillingRunProcessStage.DRAFT);

        billingRunRepository.save(billingRun);

        try {
            notificationEventPublisher.publishNotification(new NotificationEvent(billingRunId, NotificationType.BILLING_RUN_STARTUP, billingRunRepository, NotificationState.STARTUP));

            log.debug("Invoking start billing for billing run with id: %s".formatted(billingRunId));
            startBillingInvokeService.invoke(billingRun);

            try {
                log.debug("Executing next job in chain for billing run with id: {}", billingRunId);
                if (!Objects.equals(billingRun.getType(), BillingType.STANDARD_BILLING)) {
                    log.debug("Generating excel from billing run with id: {}", billingRunId);

                    try {
                        invoiceService.generateExcel(billingRunId, InvoiceStatus.DRAFT);
                    } catch (Exception e) {
                        log.error("Exception handled while trying to generate excel", e);
                    }

                    try {
                        List<RunStage> runStages = billingRun.getRunStages();
                        if (CollectionUtils.isNotEmpty(runStages)) {
                            if (runStages.contains(RunStage.GENERATE_AND_SIGN)) {
                                getNextJobInChain().execute(billingRunId, false, false);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception handled while trying execute next process", e);
                    }
                }
            } catch (Exception e) {
                log.error("Exception handled while trying to execute next job in chain for billing run with id: %s".formatted(billingRunId), e);
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to start billing for billing run with id: %s".formatted(billingRunId), e);

            log.debug("Resetting billing run status to 'INITIAL'");
            billingRun.setStatus(BillingStatus.INITIAL);
            billingRunRepository.save(billingRun);

            notificationEventPublisher.publishNotification(new NotificationEvent(billingRunId, NotificationType.BILLING_RUN_ERROR, billingRunRepository, NotificationState.ERROR));

            throw new ClientException("Exception handled while trying to start billing for billing run with id: %s, %s".formatted(billingRunId, e.getMessage()), ErrorCode.APPLICATION_ERROR);
        }
    }
}

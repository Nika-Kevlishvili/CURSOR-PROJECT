package bg.energo.phoenix.service.billing.billingRun.actions.startGeneration;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.billing.billings.BillingPermissions;
import bg.energo.phoenix.model.enums.billing.billings.BillingRunProcessStage;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.billing.billings.RunStage;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.repository.billing.billingRun.BillingErrorDataRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.billing.billingRun.actions.AbstractBillingRunActionService;
import bg.energo.phoenix.service.billing.billingRun.actions.startAccounting.BillingRunStartAccountingService;
import bg.energo.phoenix.service.billing.invoice.InvoiceService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class BillingRunStartGenerationService extends AbstractBillingRunActionService {
    private final BillingRunStartAccountingService billingRunStartAccountingService;
    private final BillingRunStartGenerationInvokeService billingRunStartGenerationInvokeService;
    private final InvoiceService invoiceService;

    public BillingRunStartGenerationService(PermissionService permissionService,
                                            BillingRunRepository billingRunRepository,
                                            BillingRunStartAccountingService billingRunStartAccountingService,
                                            NotificationEventPublisher notificationEventPublisher,
                                            BillingErrorDataRepository billingErrorDataRepository,
                                            BillingRunStartGenerationInvokeService billingRunStartGenerationInvokeService,
                                            InvoiceService invoiceService) {
        super(permissionService, billingRunRepository, notificationEventPublisher, billingErrorDataRepository);
        this.billingRunStartAccountingService = billingRunStartAccountingService;
        this.billingRunStartGenerationInvokeService = billingRunStartGenerationInvokeService;
        this.invoiceService = invoiceService;
    }

    @Override
    public AbstractBillingRunActionService getNextJobInChain() {
        return billingRunStartAccountingService;
    }

    /**
     * Starts the generating process for a billing run.
     *
     * @param billingRunId the ID of the billing run to start generating
     * @throws DomainEntityNotFoundException if no billing run exists with the provided ID
     * @throws ClientException               if the billing run is not in the 'DRAFT' status
     */
    public void execute(Long billingRunId, boolean isResumeProcess, boolean mustCheckPermission) {
        log.debug("Start generating PDF documents");

        log.debug("Fetching billing run with id: [%s]".formatted(billingRunId));
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));
        log.debug("Billing run with id: [%s] fetched".formatted(billingRunId));
        if (mustCheckPermission && missingPermission(BillingPermissions.START_GENERATING, billingRun.getType())) {
            log.error("Can't start generating for billing run without permission");
            throw new ClientException("Can't start generating for billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (!isResumeProcess) {
            BillingStatus billingRunInitialStatus = billingRun.getStatus();
            if (!Objects.equals(billingRunInitialStatus, BillingStatus.DRAFT)) {
                log.error("Billing run must be in status 'DRAFT' to start generating process");
                throw new ClientException("Billing run must be in status 'DRAFT' to start generating process", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

        billingRun.setProcessStage(BillingRunProcessStage.DRAFT_DOCUMENT);
        log.debug("Setting billing run status IN_PROGRESS_GENERATION");
        billingRun.setStatus(BillingStatus.IN_PROGRESS_GENERATION);
        billingRunRepository.save(billingRun);

        try {
            new Thread(() -> {
                try {
                    billingRunStartGenerationInvokeService.invoke(billingRun);
                } catch (Exception e) {
                    log.debug("Setting billing run status DRAFT");
                    billingRun.setStatus(BillingStatus.DRAFT);
                    billingRunRepository.save(billingRun);

                    log.error("Exception handled while invoking start generation service", e);
                    throw new ClientException(e.getMessage(), ErrorCode.APPLICATION_ERROR);
                }

                log.debug("Setting billing run status GENERATED");
                billingRun.setStatus(BillingStatus.GENERATED);
                billingRunRepository.save(billingRun);

                try {
                    invoiceService.generateExcel(billingRun.getId(), InvoiceStatus.DRAFT_GENERATED);
                } catch (Exception e) {
                    log.error("Exception handled while trying to generate excel", e);
                }

                try {
                    List<RunStage> runStages = ListUtils.emptyIfNull(billingRun.getRunStages());
                    if (CollectionUtils.isNotEmpty(runStages)) {
                        if (runStages.contains(RunStage.AUTOMATICALLY_ACCOUNTING)) {
                            getNextJobInChain().execute(billingRunId, false, false);
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception handled while trying execute next process", e);
                }
            }).start();
        } catch (Exception e) {
            log.error("Exception handled while trying to generate documents", e);
        }
    }
}

package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.accountingPeriod.AccountingPeriods;
import bg.energo.phoenix.model.entity.billing.billingRun.*;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicity;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.ProcessPeriodicityIncompatibleProcesses;
import bg.energo.phoenix.model.entity.contract.billing.BillingInvoicesFile;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.contract.TaskType;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityBillingProcessStart;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.task.PerformerType;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.billing.billingRun.*;
import bg.energo.phoenix.model.request.billing.billingRun.create.BillingRunCreateRequest;
import bg.energo.phoenix.model.request.billing.billingRun.create.InvoiceReversalParameters;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.CustomerContractsAndOrdersRequest;
import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.ManualInvoiceImportResponse;
import bg.energo.phoenix.model.request.billing.billingRun.edit.BillingRunEditRequest;
import bg.energo.phoenix.model.request.billing.billingRun.iap.InterimAndAdvancePaymentParameters;
import bg.energo.phoenix.model.request.billing.billingRun.manualCreditOrDebitNote.BillingRunInvoiceRequest;
import bg.energo.phoenix.model.request.billing.communicationData.BillingCommunicationDataListRequest;
import bg.energo.phoenix.model.response.billing.accountingPeriods.AccountingPeriodsResponse;
import bg.energo.phoenix.model.response.billing.billingRun.*;
import bg.energo.phoenix.model.response.billing.billingRun.condition.BillingRunConditionValidateResponse;
import bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote.BillingRunInvoiceResponse;
import bg.energo.phoenix.model.response.billing.file.BillingFileResponse;
import bg.energo.phoenix.model.response.contract.biling.BillingGroupListingResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.CustomerContractOrderResponse;
import bg.energo.phoenix.model.response.nomenclature.contract.TaskTypeShortResponse;
import bg.energo.phoenix.model.response.product.ProductShortResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.billingRun.*;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.processPeriodicity.ProcessPeriodicityIncompatibleProcessesRepository;
import bg.energo.phoenix.repository.billing.processPeriodicity.ProcessPeriodicityRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.PortalTagRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.TaskTypeRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.billing.accountingPeriods.AccountingPeriodService;
import bg.energo.phoenix.service.billing.billingRun.actions.startBilling.BillingRunStartBillingService;
import bg.energo.phoenix.service.billing.billingRun.errors.BillingProtocol;
import bg.energo.phoenix.service.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteService;
import bg.energo.phoenix.service.billing.billingRun.manualInterimAndAdvancePayment.ManualInterimAndAdvancePaymentMapperService;
import bg.energo.phoenix.service.billing.billingRun.manualInvoice.ManualInvoiceService;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalProcessService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.excel.MultiSheetExcelService;
import bg.energo.phoenix.service.excel.MultiSheetExcelType;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.UrlEncodingUtil;
import bg.energo.phoenix.util.billing.BillingVatRateUtil;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import io.lettuce.core.KeyValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.sql.CallableStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.model.enums.billing.billings.BillingCriteria.CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS;
import static bg.energo.phoenix.model.enums.billing.billings.BillingCriteria.LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS;
import static bg.energo.phoenix.model.enums.billing.billings.BillingType.INVOICE_REVERSAL;
import static bg.energo.phoenix.model.enums.billing.billings.BillingType.STANDARD_BILLING;
import static bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityBillingProcessStart.DATE_AND_TIME;
import static bg.energo.phoenix.model.enums.template.ContractTemplatePurposes.INVOICE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.BILLING_RUN;
import static bg.energo.phoenix.permissions.PermissionEnum.CREATE_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE;
import static bg.energo.phoenix.permissions.PermissionEnum.EDIT_BILLING_RUN_WITH_STATUS;
import static bg.energo.phoenix.util.epb.EPBFinalFields.BILLING_RUN_NUMBER_PREFIX;
import static bg.energo.phoenix.util.epb.EPBFinalFields.MAX_BILLING_RUN_NUMBER_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunService {
    private final static String FOLDER_PATH = "invoice_files";
    private final BillingSumFileRepository sumFileRepository;
    private final DocumentsRepository documentRepository;
    private final ProcessPeriodicityRepository processPeriodicityRepository;
    private final AccountingPeriodsRepository accountingPeriodsRepository;
    private final BillingRunRepository billingRunRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final BillingProcessPeriodicityRepository billingProcessPeriodicityRepository;
    private final PermissionService permissionService;
    private final TaskTypeRepository taskTypeRepository;
    private final InterestRateRepository interestRateRepository;
    private final BankRepository bankRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ContractBillingGroupRepository billingGroupRepository;
    private final BillingRunBillingGroupRepository billingRunBillingGroupRepository;
    private final CustomerRepository customerRepository;
    private final BillingRunTasksRepository billingRunTasksRepository;
    private final TaskRepository taskRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final ProcessPeriodicityIncompatibleProcessesRepository processPeriodicityIncompatibleProcessesRepository;
    private final ManualInvoiceService manualInvoiceService;
    private final BillingInvoicesFileRepository billingInvoicesFileRepository;
    private final TemplateRepository templateRepository;
    private final FileService fileService;
    private final BillingRunInvoicesRepository billingRunInvoicesRepository;
    private final ManualCreditOrDebitNoteService manualCreditOrDebitNoteService;
    private final InvoiceRepository invoiceRepository;
    private final TaskService taskService;
    private final ManualInterimAndAdvancePaymentMapperService manualInterimAndAdvancePaymentMapperService;
    private final BillingRunConditionService billingRunConditionService;
    private final BillingRunCommonService billingRunCommonService;
    private final BillingSummaryDataRepository billingSummaryDataRepository;
    private final BillingDetailedDataRepository billingDetailedDataRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final BillingRunDraftInvoicesMarkRepository billingRunDraftInvoicesMarkRepository;
    private final BillingRunDraftPdfInvoicesMarkRepository billingRunDraftPdfInvoicesMarkRepository;
    private final TransactionTemplate transactionTemplate;
    private final InvoiceReversalProcessService invoiceReversalProcessService;
    private final MultiSheetExcelService multiSheetExcelService;
    private final BillingRunStartBillingService billingRunStartBillingService;
    private final BillingRunProcessHelper billingRunProcessHelper;
    private final ContractTemplateRepository contractTemplateRepository;
    private final PortalTagRepository portalTagRepository;
    private final BillingNotificationRepository billingNotificationRepository;
    private final BillingVatRateUtil billingVatRateUtil;
    private final ProductRepository productRepository;
    private final BillingRunOutdatedDocumentService billingRunOutdatedDocumentService;
    private final BillingInvoiceCorrectionInvoiceRepository billingInvoiceCorrectionInvoiceRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    /**
     * Extracts a list of enum values from a comma-separated string representation.
     *
     * @param enums A string representation of enum values, enclosed in curly braces.
     * @param type  The enum class type to use for the extraction.
     * @return A list of enum values extracted from the input string, or null if the input string is null.
     */
    private static <T extends Enum<T>> List<T> extractEnums(String enums, Class<T> type) {
        if (enums == null) {
            return null;
        }
        return Arrays.stream(enums.replaceAll("[{}]", "")
                .split(",")).map(String::trim).filter(x -> !x.isEmpty()).map(x -> Enum.valueOf(type, x)).toList();
    }

    /**
     * Creates a new billing run based on the provided request.
     *
     * @param billingRunCreateRequest the request containing the details for the new billing run
     * @return the ID of the newly created billing run
     * @throws ClientException if the user does not have permission to create a new billing run
     */
    public Long create(BillingRunCreateRequest billingRunCreateRequest) {
        log.info("creating billing run with id: %s;".formatted(billingRunCreateRequest));
        List<String> errorMessages = new ArrayList<>();
        if (!checkBillingPermission(BillingPermissions.CREATE, billingRunCreateRequest.getBillingType())) {
            throw new ClientException("Can't create billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        if (!isPriceVolumeChangeValid(billingRunCreateRequest.getBillingType(), billingRunCreateRequest.getInvoiceCorrectionParameters())) {
            throw new IllegalArgumentsProvidedException("Price or Volume change - at least one of them should be true");
        }
        BillingRun billingRun = mapCommonParameters(billingRunCreateRequest, errorMessages);

        transactionTemplate.executeWithoutResult((a) -> {
            BillingRunCommonParameters commonParameters = billingRunCreateRequest.getCommonParameters();
            switch (billingRunCreateRequest.getBillingType()) {
                case MANUAL_INTERIM_AND_ADVANCE_PAYMENT ->
                        mapInterimAndAdvancePaymentParameters(billingRun, billingRunCreateRequest.getInterimAndAdvancePaymentParameters(), errorMessages, false);
                case STANDARD_BILLING -> {
                    mapStandardBillingParameters(billingRunCreateRequest, billingRun, errorMessages);
                    checkTaxEventDate(commonParameters, errorMessages);
                }
                case MANUAL_INVOICE -> {
                    manualInvoiceService.mapManualInvoiceParameters(billingRunCreateRequest, billingRun, errorMessages);
                    checkTaxEventDate(commonParameters, errorMessages);
                }
                case INVOICE_CORRECTION -> {
                    checkTaxEventDate(commonParameters, errorMessages);
                    checkInvoiceDate(commonParameters, errorMessages);
                    mapInvoiceCorrectionParameters(billingRunCreateRequest.getInvoiceCorrectionParameters(), billingRun, errorMessages);
                }
                case MANUAL_CREDIT_OR_DEBIT_NOTE -> {
                    checkPermission(CREATE_BILLING_RUN_MANUAL_CREDIT_OR_DEBIT_NOTE);
                    manualCreditOrDebitNoteService.create(billingRunCreateRequest, billingRun, errorMessages);
                }
                case INVOICE_REVERSAL ->
                        mapInvoiceReversalParameters(billingRunCreateRequest.getInvoiceReversalParameters(), billingRun, errorMessages);
            }
            creteProcessPeriodicity(commonParameters.getProcessPeriodicityIds(), billingRun, errorMessages);
            if (BillingRunPeriodicity.PERIODIC.equals(billingRun.getRunPeriodicity())) {
                createOneTimeFromPeriodicity(billingRun);
            }
            validateAndSetTemplate(commonParameters.getTemplateId(), billingRun, errorMessages);
            validateAndSetEmailTemplate(commonParameters.getEmailTemplateId(), billingRun, errorMessages);
            setBillingNotifications(commonParameters.getBillingNotifications(), billingRun.getId(), errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            billingRunRepository.save(billingRun);
        });

        if (Objects.equals(ExecutionType.IMMEDIATELY, billingRun.getExecutionType())) {
            BillingType billingRunType = billingRun.getType();
            switch (billingRunType) {
                case MANUAL_INVOICE, MANUAL_CREDIT_OR_DEBIT_NOTE, MANUAL_INTERIM_AND_ADVANCE_PAYMENT ->
                        billingRunStartBillingService.execute(billingRun.getId(), false, false);
                default ->
                        Executors.newSingleThreadExecutor().submit(() -> billingRunStartBillingService.execute(billingRun.getId(), false, false));
            }
        }

        return billingRun.getId();
    }

    /**
     * Checks if the current user has the necessary permission to perform the specified billing type.
     *
     * @param billingPermissions The billing permissions to check.
     * @param billingType        The billing type to check the permission for.
     * @return True if the user has the necessary permission, false otherwise.
     */
    private boolean checkBillingPermission(BillingPermissions billingPermissions, BillingType billingType) {
        return permissionService.getPermissionsFromContext(BILLING_RUN).contains(billingPermissions.getRelevantPermission(billingType).getId());
    }

    /**
     * Creates a one-time billing run based on the first billing process periodicity found for the given billing run that is scheduled for today.
     *
     * @param billingRun The billing run to create the one-time run for.
     */
    private void createOneTimeFromPeriodicity(BillingRun billingRun) {
        List<ProcessPeriodicity> forToday = processPeriodicityRepository.findAllByBillingRunForToday(billingRun.getId());
        Optional<ProcessPeriodicity> first = forToday.stream().findFirst();
        if (first.isEmpty()) {
            return;
        }
        ProcessPeriodicity processPeriodicity = first.get();
        createOneTime(billingRun, processPeriodicity.getProcessPeriodicityBillingProcessStart(), processPeriodicity.getBillingProcessStartDate(), processPeriodicity.getId());
    }

    private void createOneTime(BillingRun billingRun, ProcessPeriodicityBillingProcessStart executionType, LocalDateTime executionTime, Long periodicityId) {
        BillingRun cloned = billingRunRepository.saveAndFlush(cloneBillingRun(billingRun, executionType, executionTime));
        cloned.setPeriodicityCreatedFromId(periodicityId);
        cloneSubObjects(cloned, billingRun);
    }

    /**
     * Clones the sub-objects associated with a billing run, such as tasks, billing groups, and periodicity.
     *
     * @param cloned The new billing run to which the sub-objects should be cloned.
     * @param oldRun The original billing run from which the sub-objects should be cloned.
     */
    private void cloneSubObjects(BillingRun cloned, BillingRun oldRun) {
        //Todo add templates
//        List<BillingProcessPeriodicity> oldPeriodicity = billingProcessPeriodicityRepository.findByBillingIdAndStatus(oldRun.getId(), EntityStatus.ACTIVE);
//        List<BillingProcessPeriodicity> newPeriodicity = oldPeriodicity.stream().map(x -> new BillingProcessPeriodicity(x, cloned.getId())).toList();
//        billingProcessPeriodicityRepository.saveAll(newPeriodicity);
        List<BillingRunTasks> tasks = billingRunTasksRepository.findByBillingIdAndStatusIn(oldRun.getId(), List.of(EntityStatus.ACTIVE));
        List<BillingRunTasks> newTasks = tasks.stream().map(x -> new BillingRunTasks(cloned.getId(), x)).toList();
        billingRunTasksRepository.saveAll(newTasks);
        List<BillingRunBillingGroup> billingGroups = billingRunBillingGroupRepository.findByBillingRunId(oldRun.getId(), EntityStatus.ACTIVE);
        List<BillingRunBillingGroup> newBillingGroups = billingGroups.stream().map(x -> new BillingRunBillingGroup(x, cloned.getId())).toList();
        billingRunBillingGroupRepository.saveAll(newBillingGroups);
    }

    /**
     * Clones a billing run, creating a new instance with updated execution details.
     *
     * @param billingRun    The original billing run to clone.
     * @param executionType The execution type for the new billing run.
     * @param executionTime The execution time for the new billing run.
     * @return The cloned billing run.
     */
    private BillingRun cloneBillingRun(BillingRun billingRun, ProcessPeriodicityBillingProcessStart executionType, LocalDateTime executionTime) {
        BillingRun newRun = new BillingRun();
        newRun.setBillingNumber(generateBillingNumber());
        newRun.setPeriodicBillingCreatedFrom(billingRun.getId());
        newRun.setRunPeriodicity(BillingRunPeriodicity.STANDARD);
        newRun.setAdditionalInfo(billingRun.getAdditionalInfo());
        newRun.setType(billingRun.getType());
        newRun.setStatus(BillingStatus.INITIAL);
        newRun.setEmployeeId(billingRun.getEmployeeId());
        newRun.setTemplateId(billingRun.getTemplateId());
        newRun.setMaxEndDate(generatePeriodicEndDate(billingRun.getPeriodicMaxEndDate(), billingRun.getPeriodicMaxEndDateValue()));
        //Todo check with tiko
        newRun.setTaxEventDate(LocalDate.now());
        newRun.setInvoiceDate(LocalDate.now());
        Optional<Long> currentMonthsAccountingPeriodId = accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId();
        //TOdo check this maybe I have to throw exception
        currentMonthsAccountingPeriodId.ifPresent(newRun::setAccountingPeriodId);
        newRun.setInvoiceDueDateType(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT);
        newRun.setInvoiceDueDate(LocalDate.now());
        newRun.setApplicationModelType(billingRun.getApplicationModelType());
        newRun.setSendingAnInvoice(billingRun.getSendingAnInvoice());
        if (executionType.equals(ProcessPeriodicityBillingProcessStart.MANUAL)) {
            newRun.setExecutionType(ExecutionType.MANUAL);
        } else if (executionType.equals(DATE_AND_TIME)) {
            newRun.setExecutionType(ExecutionType.EXACT_DATE);
            newRun.setExecutionDate(executionTime);
        } else {
            newRun.setExecutionType(ExecutionType.EXACT_DATE);
            newRun.setExecutionDate(LocalDateTime.now().plusMinutes(30));
        }
        newRun.setBillingCriteria(billingRun.getBillingCriteria());
        newRun.setApplicationLevel(billingRun.getApplicationLevel());
        newRun.setCustomerContractOrPodConditions(billingRun.getCustomerContractOrPodConditions());
        newRun.setCustomerContractOrPodList(billingRun.getCustomerContractOrPodList());
        newRun.setRunStages(billingRun.getRunStages());
        newRun.setVatRateId(billingRun.getVatRateId());
        newRun.setGlobalVatRate(billingRun.getGlobalVatRate());
        newRun.setInterestRateId(billingRun.getInterestRateId());
        newRun.setBankId(billingRun.getBankId());
        newRun.setIban(billingRun.getIban());
        newRun.setAmountExcludingVat(billingRun.getAmountExcludingVat());
        newRun.setIssuingForTheMonthToCurrent(billingRun.getIssuingForTheMonthToCurrent());
        newRun.setIssuedSeparateInvoices(billingRun.getIssuedSeparateInvoices());
        newRun.setCurrencyId(billingRun.getCurrencyId());
        newRun.setDeductionFrom(billingRun.getDeductionFrom());
        newRun.setNumberOfIncomeAccount(billingRun.getNumberOfIncomeAccount());
        newRun.setCostCenterControllingOrder(billingRun.getCostCenterControllingOrder());
        newRun.setCustomerDetailId(billingRun.getCustomerDetailId());
        newRun.setProductContractId(billingRun.getProductContractId());
        newRun.setServiceContractId(billingRun.getServiceContractId());
        newRun.setDocumentType(billingRun.getDocumentType());
        newRun.setGoodsOrderId(billingRun.getGoodsOrderId());
        newRun.setServiceOrderId(billingRun.getServiceOrderId());
        newRun.setBasisForIssuing(billingRun.getBasisForIssuing());
        newRun.setManualInvoiceType(billingRun.getManualInvoiceType());
        newRun.setDirectDebit(billingRun.getDirectDebit());
        newRun.setCustomerCommunicationId(billingRun.getCustomerCommunicationId());
        newRun.setListOfInvoices(billingRun.getListOfInvoices());
        newRun.setPriceChange(billingRun.getPriceChange());
        return newRun;
    }

    /**
     * Saves the list of {@link BillingProcessPeriodicity} entities to the database if the list is not empty and there are no error messages.
     */
    private void creteProcessPeriodicity(List<Long> processPeriodicityIds, BillingRun billingRun, List<String> errorMassages) {
        List<BillingProcessPeriodicity> billingProcessPeriodicities = new ArrayList<>();
        if (CollectionUtils.isEmpty(processPeriodicityIds)) {
            return;
        }
        for (int i = 0; i < processPeriodicityIds.size(); i++) {
            BillingProcessPeriodicity billingProcessPeriodicity = new BillingProcessPeriodicity();
            billingProcessPeriodicity.setBillingId(billingRun.getId());
            billingProcessPeriodicity.setProcessPeriodicityId(checkProcessPeriodicity(processPeriodicityIds.get(i), i, errorMassages));
            billingProcessPeriodicity.setStatus(EntityStatus.ACTIVE);
            billingProcessPeriodicities.add(billingProcessPeriodicity);
        }
        if (!CollectionUtils.isEmpty(billingProcessPeriodicities) && CollectionUtils.isEmpty(errorMassages)) {
            billingProcessPeriodicityRepository.saveAll(billingProcessPeriodicities);
        }
    }

    /**
     * Checks if the given process periodicity ID is valid and active. If not, adds an error message to the provided list.
     *
     * @param id            The ID of the process periodicity to check.
     * @param i             The index of the process periodicity in the list.
     * @param errorMassages The list to add error messages to if the process periodicity is not found or not active.
     * @return The ID of the active process periodicity if found, or null if not found.
     */
    private Long checkProcessPeriodicity(Long id, int i, List<String> errorMassages) {
        Optional<ProcessPeriodicity> processPeriodicityOptional = processPeriodicityRepository.findByIdAndStatus(id, EntityStatus.ACTIVE);
        if (processPeriodicityOptional.isPresent()) {
            return processPeriodicityOptional.get().getId();
        } else {
            errorMassages.add("processPeriodicityIds[%s]-ProcessPeriodicity Id can't be found;".formatted(i));
            return null;
        }
    }

    /**
     * Maps the standard billing parameters from the {@link BillingRunCreateRequest} to the {@link BillingRun} entity.
     *
     * @param billingRunCreateRequest The request object containing the billing run parameters.
     * @param billingRun              The billing run entity to be updated.
     * @param errorMassages           The list to add any error messages encountered during the mapping process.
     */
    private void mapStandardBillingParameters(BillingRunCreateRequest billingRunCreateRequest, BillingRun billingRun, List<String> errorMassages) {
        StandardBillingParameters standardBillingParameters = billingRunCreateRequest.getBasicParameters();
        checkCondition(billingRunCreateRequest.getBasicParameters().getBillingCriteria(),
                billingRunCreateRequest.getBasicParameters().getCustomersContractOrPODConditions(),
                errorMassages);
        billingRun.setBillingCriteria(standardBillingParameters.getBillingCriteria());
        billingRun.setApplicationLevel(standardBillingParameters.getBillingApplicationLevel());
        billingRun.setApplicationModelType(standardBillingParameters.getApplicationModelType());
        billingRun.setMaxEndDate(standardBillingParameters.getMaxEndDate());
        billingRun.setPeriodicMaxEndDate(standardBillingParameters.getPeriodicMaxEndDate());
        billingRun.setPeriodicMaxEndDateValue(standardBillingParameters.getPeriodicMaxEndDateValue());
        billingRun.setCustomerContractOrPodList(checkCustomerContractOrPod(standardBillingParameters.getBillingApplicationLevel(), standardBillingParameters.getListOfCustomersContractsOrPOD(), errorMassages, standardBillingParameters.getBillingCriteria()));
        billingRun.setCustomerContractOrPodConditions(standardBillingParameters.getCustomersContractOrPODConditions());
    }

    /**
     * Maps the invoice correction parameters from the {@link InvoiceCorrectionParameters} to the {@link BillingRun} entity.
     *
     * @param invoiceCorrectionParameters The invoice correction parameters to be mapped.
     * @param billingRun                  The billing run entity to be updated.
     * @param errorMessages               The list to add any error messages encountered during the mapping process.
     */
    private void mapInvoiceCorrectionParameters(InvoiceCorrectionParameters invoiceCorrectionParameters, BillingRun billingRun, List<String> errorMessages) {
        Optional<BillingInvoicesFile> billingInvoicesFileOld = billingInvoicesFileRepository.
                findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
        if (billingInvoicesFileOld.isPresent() && !Objects.equals(billingInvoicesFileOld.get().getId(), invoiceCorrectionParameters.getFileId())) {
            billingInvoicesFileOld.get().setStatus(EntityStatus.DELETED);
            billingInvoicesFileRepository.saveAndFlush(billingInvoicesFileOld.get());
        }
        Optional<BillingInvoicesFile> fileByBillingId = billingInvoicesFileRepository.findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
        fileByBillingId.ifPresent(x -> {
            x.setBillingId(null);
            billingInvoicesFileRepository.save(x);
        });
        if (invoiceCorrectionParameters.getFileId() != null) {
            Optional<BillingInvoicesFile> billingInvoicesFile = billingInvoicesFileRepository.
                    findByIdAndStatusIn(invoiceCorrectionParameters.getFileId(), List.of(EntityStatus.ACTIVE));

            if (billingInvoicesFile.isEmpty()) {
                errorMessages.add("invoiceCorrectionParameters.billingInvoicesFile-File with Id: %s can't be found;".formatted(invoiceCorrectionParameters.getFileId()));
            }
            billingInvoicesFile.ifPresent(invoicesFile -> invoicesFile.setBillingId(billingRun.getId()));
        }

        parseInvoiceCorrectionFile(billingRun, errorMessages);

        billingRun.setListOfInvoices(invoiceCorrectionParameters.getListOfInvoices());
        billingRun.setPriceChange(invoiceCorrectionParameters.isPriceChange());
        billingRun.setVolumeChange(invoiceCorrectionParameters.isVolumeChange());
    }

    private List<InvoiceCorrectionFileContent> parseInvoiceCorrectionFile(BillingRun billingRun, List<String> errorMessages) {
        billingInvoiceCorrectionInvoiceRepository.deleteAllByBillingId(billingRun.getId());
        List<BillingInvoiceCorrectionInvoice> invoiceCorrectionInvoices = new ArrayList<>();

        Optional<BillingInvoicesFile> billingInvoicesFileOptional = billingInvoicesFileRepository.
                findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);

        if (billingInvoicesFileOptional.isPresent()) {
            BillingInvoicesFile billingInvoicesFile = billingInvoicesFileOptional.get();

            ByteArrayResource invoiceCorrectionFileResource = fileService.downloadFile(billingInvoicesFile.getFileUrl());

            try (InputStream fis = invoiceCorrectionFileResource.getInputStream(); Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet invoiceCorrectionSheet = workbook.getSheetAt(0);
                int physicalNumberOfRows = invoiceCorrectionSheet.getPhysicalNumberOfRows();

                for (int i = 1; i < physicalNumberOfRows; i++) {
                    Row row = invoiceCorrectionSheet.getRow(i);
                    String invoiceNumber = EPBExcelUtils.getStringValue(0, row);

                    if (Objects.nonNull(invoiceNumber)) {
                        invoiceCorrectionInvoices.add(
                                BillingInvoiceCorrectionInvoice
                                        .builder()
                                        .billingId(billingRun.getId())
                                        .invoiceNumber(invoiceNumber)
                                        .build()
                        );
                    } else {
                        log.error("Error while parsing invoice correction file: %s".formatted(invoiceCorrectionFileResource.getFilename()));
                        errorMessages.add("invoiceCorrectionParameters.fileId-Invoice correction file has invalid format;");
                    }
                }
            } catch (Exception e) {
                log.error("Exception handled while trying to parse the invoice correction file: %s;".formatted(e.getMessage()));
                errorMessages.add("invoiceCorrectionParameters.fileId-File with Id: %s can't be parsed;".formatted(billingInvoicesFile.getId()));
            }
        }

        billingInvoiceCorrectionInvoiceRepository.saveAll(invoiceCorrectionInvoices);

        return new ArrayList<>();
    }

    /**
     * Maps the invoice reversal parameters from the {@link InvoiceReversalParameters} to the {@link BillingRun} entity.
     *
     * @param invoiceReversalParameters The invoice reversal parameters to be mapped.
     * @param billingRun                The billing run entity to be updated.
     * @param errorMessages             The list to add any error messages encountered during the mapping process.
     */
    private void mapInvoiceReversalParameters(InvoiceReversalParameters invoiceReversalParameters, BillingRun billingRun, List<String> errorMessages) {
        Optional<BillingInvoicesFile> billingInvoicesFileOld = billingInvoicesFileRepository.
                findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
        if (billingInvoicesFileOld.isPresent() && !Objects.equals(billingInvoicesFileOld.get().getId(), invoiceReversalParameters.getFileId())) {
            billingInvoicesFileOld.get().setStatus(EntityStatus.DELETED);
            billingInvoicesFileRepository.saveAndFlush(billingInvoicesFileOld.get());
        }

        if (invoiceReversalParameters.getFileId() != null) {
            Optional<BillingInvoicesFile> billingInvoicesFile = billingInvoicesFileRepository.
                    findByIdAndStatusIn(invoiceReversalParameters.getFileId(), List.of(EntityStatus.ACTIVE));
            if (billingInvoicesFile.isEmpty()) {
                errorMessages.add("invoiceReversalParameters.billingInvoicesFile-File with Id: %s can't be found;".formatted(invoiceReversalParameters.getFileId()));
            }
            billingInvoicesFile.ifPresent(invoicesFile -> invoicesFile.setBillingId(billingRun.getId()));
        }
        //TODO CHECK INVOICE VALIDITY WHEN INVOICE LOGIC WILL BE IMPLEMENTED
        billingRun.setListOfInvoices(invoiceReversalParameters.getListOfInvoices());
    }

    /**
     * Maps the common parameters from the {@link BillingRunCreateRequest} to the {@link BillingRun} entity.
     *
     * @param billingRunCreateRequest The billing run creation request containing the common parameters.
     * @param errorMessages           The list to add any error messages encountered during the mapping process.
     * @return The newly created {@link BillingRun} entity with the common parameters mapped.
     */
    private BillingRun mapCommonParameters(BillingRunCreateRequest billingRunCreateRequest, List<String> errorMessages) {
        BillingRunCommonParameters commonParameters = billingRunCreateRequest.getCommonParameters();
        BillingRun billingRun = new BillingRun();
        billingRun.setBillingNumber(generateBillingNumber());
        billingRun.setStatus(BillingStatus.INITIAL);
        billingRun.setProcessStage(BillingRunProcessStage.DRAFT);
        billingRun.setRunPeriodicity(commonParameters.getPeriodicity());
        billingRun.setAdditionalInfo(commonParameters.getAdditionalInformation());
        billingRun.setType(billingRunCreateRequest.getBillingType());
        billingRun.setTaxEventDate(commonParameters.getTaxEventDate());
        checkAccountingPeriodAndInvoiceDate(billingRun, commonParameters.getAccountingPeriodId(), commonParameters.getInvoiceDate(), errorMessages);
        billingRun.setInvoiceDueDateType(commonParameters.getInvoiceDueDate());
        billingRun.setInvoiceDueDate(commonParameters.getDueDate());
        billingRun.setBillingNumber(generateBillingNumber());

        billingRun.setSendingAnInvoice(commonParameters.getSendingAnInvoice());
        billingRun.setExecutionType(commonParameters.getExecutionType());
        billingRun.setExecutionDate(commonParameters.getExecutionDateAndTime());
        billingRun.setRunStages(commonParameters.getRunStages());
        //This should be used on create only!;
        AccountManager accountManager = accountManagerRepository.findByUserName(permissionService.getLoggedInUserId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Logged in user is not account manager!;"));
        billingRun.setEmployeeId(accountManager.getId());
        return billingRunRepository.saveAndFlush(billingRun);
    }

    /**
     * Checks the accounting period and invoice date for the billing run.
     *
     * @param billingRun         The billing run entity.
     * @param accountingPeriodId The ID of the accounting period.
     * @param invoiceDate        The invoice date.
     * @param errorMessages      The list to add any error messages encountered during the check.
     */
    private void checkAccountingPeriodAndInvoiceDate(BillingRun billingRun, Long accountingPeriodId, LocalDate invoiceDate, List<String> errorMessages) {
        if (accountingPeriodId != null) {
            Optional<AccountingPeriodsResponse> accountingPeriodsOptional = accountingPeriodService.checkAvailableIdForBillingRun(accountingPeriodId);
            if (accountingPeriodsOptional.isPresent()) {
                billingRun.setAccountingPeriodId(accountingPeriodsOptional.get().getId());

                checkAccountingPeriodDateRange(errorMessages, accountingPeriodsOptional.get(), invoiceDate);

                billingRun.setInvoiceDate(invoiceDate);
            } else {
                errorMessages.add("commonParameters.accountingPeriodId-[accountingPeriodId] can't be found;");
            }
        }
    }

    /**
     * Maps the interim and advance payment parameters to the billing run entity.
     *
     * @param billingRun                         The billing run entity.
     * @param interimAndAdvancePaymentParameters The interim and advance payment parameters.
     * @param errorMessages                      The list to add any error messages encountered during the mapping.
     * @param isUpdate                           Indicates whether this is an update operation.
     */
    private void mapInterimAndAdvancePaymentParameters(BillingRun billingRun, InterimAndAdvancePaymentParameters interimAndAdvancePaymentParameters, List<String> errorMessages, boolean isUpdate) {
        checkInterimAdvancePaymentParameters(billingRun, interimAndAdvancePaymentParameters, errorMessages, isUpdate);
        billingRun.setBasisForIssuing(interimAndAdvancePaymentParameters.getBasisForIssuing());
        billingRun.setAmountExcludingVat(interimAndAdvancePaymentParameters.getAmountExcludingVat());
        billingRun.setIssuingForTheMonthToCurrent(interimAndAdvancePaymentParameters.getIssuingForTheMonthToCurrent());
        billingRun.setIssuedSeparateInvoices(interimAndAdvancePaymentParameters.getIssuedSeparateInvoices());
        billingRun.setDeductionFrom(interimAndAdvancePaymentParameters.getDeductionFrom());
        billingRun.setNumberOfIncomeAccount(interimAndAdvancePaymentParameters.getNumberOfIncomeAccount());
        billingRun.setCostCenterControllingOrder(interimAndAdvancePaymentParameters.getCostCenterControllingOrder());
        billingRun.setPrefixType(interimAndAdvancePaymentParameters.getPrefixType());
        billingRunCommonService.checkInvoiceCommunicationData(interimAndAdvancePaymentParameters.getInvoiceCommunicationDataId(), interimAndAdvancePaymentParameters.getContractId(), interimAndAdvancePaymentParameters.getCustomerDetailId(), interimAndAdvancePaymentParameters.getContractType() == null ? null : interimAndAdvancePaymentParameters.getContractType().toContractOrderType(), billingRun, errorMessages, "interimAndAdvancePaymentParameters");
    }

    /**
     * Checks the customer contract or POD conditions and performs the necessary validations.
     *
     * @param billingApplicationLevel          The billing application level.
     * @param customersContractOrPODConditions The customers contract or POD conditions.
     * @param errorMassages                    The list to add any error messages encountered during the check.
     * @param billingCriteria                  The billing criteria.
     * @return The customers contract or POD conditions.
     */
    private String checkCustomerContractOrPod(BillingApplicationLevel billingApplicationLevel, String customersContractOrPODConditions, List<String> errorMassages, BillingCriteria billingCriteria) {
        if (billingCriteria.equals(LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS) && billingApplicationLevel != null) {
            if (!StringUtils.isEmpty(customersContractOrPODConditions)) {
                Set<String> listOfNames = Arrays.stream(customersContractOrPODConditions.split(",")).collect(Collectors.toSet());
                switch (billingApplicationLevel) {
                    case POD -> {
                        checkPods(listOfNames, errorMassages);
                        break;
                    }
                    case CUSTOMER -> {
                        checkCustomer(listOfNames, errorMassages);
                        break;
                    }
                    case CONTRACT -> {
                        checkContracts(listOfNames, errorMassages);
                        break;
                    }
                }
            }
        }
        return customersContractOrPODConditions;
    }

    /**
     * Checks the existence of service contracts and product contracts in the list of contract numbers.
     *
     * @param listOfNames   The set of contract numbers to check.
     * @param errorMassages The list to add any error messages encountered during the check.
     */
    private void checkContracts(Set<String> listOfNames, List<String> errorMassages) {
        for (String item : listOfNames) {
            Optional<ServiceContracts> serviceContractsOptional = serviceContractsRepository.findByContractNumberAndStatus(item, EntityStatus.ACTIVE);
            Optional<ProductContract> productContractOptional = productContractRepository.findByContractNumberAndStatus(item, ProductContractStatus.ACTIVE);
            if (serviceContractsOptional.isEmpty() && productContractOptional.isEmpty()) {
                errorMassages.add("basicParameters.listOfCustomersContractsOrPOD-[listOfCustomersContractsOrPOD] contract with: %s can't be found;".formatted(item));
            }
        }
    }

    /**
     * Checks the existence of customers in the list of customer identifiers.
     *
     * @param listOfNames   The set of customer identifiers to check.
     * @param errorMassages The list to add any error messages encountered during the check.
     */
    private void checkCustomer(Set<String> listOfNames, List<String> errorMassages) {
        for (String item : listOfNames) {
            Optional<Customer> customer = customerRepository.findByIdentifierAndStatus(item, CustomerStatus.ACTIVE);
            if (customer.isEmpty()) {
                errorMassages.add("basicParameters.listOfCustomersContractsOrPOD-[listOfCustomersContractsOrPOD] customer with: %s can't be found;".formatted(item));
            }
        }
    }

    /**
     * Checks the existence of points of delivery (PODs) in the list of POD identifiers.
     *
     * @param listOfNames   The set of POD identifiers to check.
     * @param errorMassages The list to add any error messages encountered during the check.
     */
    private void checkPods(Set<String> listOfNames, List<String> errorMassages) {
        for (String item : listOfNames) {
            Optional<PointOfDelivery> pod = pointOfDeliveryRepository.findByIdentifierAndStatus(item, PodStatus.ACTIVE);
            if (pod.isEmpty()) {
                errorMassages.add("basicParameters.listOfCustomersContractsOrPOD-[listOfCustomersContractsOrPOD] pod with: %s can't be found;".formatted(item));
            }
        }
    }

    /**
     * Checks and validates the interim and advance payment parameters for a billing run.
     *
     * @param billingRun                         The billing run object.
     * @param interimAndAdvancePaymentParameters The interim and advance payment parameters object.
     * @param errorMessages                      The list to add any error messages encountered during the validation.
     * @param isUpdate                           Indicates whether the billing run is being updated.
     */
    private void checkInterimAdvancePaymentParameters(BillingRun billingRun, InterimAndAdvancePaymentParameters interimAndAdvancePaymentParameters, List<String> errorMessages, boolean isUpdate) {
        String interimAdvancePaymentParameters = Character.toLowerCase(InterimAndAdvancePaymentParameters.class.getSimpleName().charAt(0)) + InterimAndAdvancePaymentParameters.class.getSimpleName().substring(1);

        billingVatRateUtil.checkVatRateCommons(billingRun, interimAndAdvancePaymentParameters.isGlobalVatRate(), interimAndAdvancePaymentParameters.getVatRateId(), errorMessages, interimAdvancePaymentParameters);

        checkApplicableInterestRate(billingRun, interimAndAdvancePaymentParameters.getApplicableInterestRateId(), errorMessages, interimAdvancePaymentParameters);

        checkBank(billingRun, interimAndAdvancePaymentParameters.getBankId(), errorMessages, interimAdvancePaymentParameters);
        billingRun.setDirectDebit(!interimAndAdvancePaymentParameters.isDirectDebitManual() ? null : interimAndAdvancePaymentParameters.isDirectDebit());
        billingRun.setIban(interimAndAdvancePaymentParameters.getIban());
        if (checkCurrency(interimAndAdvancePaymentParameters.getCurrencyId(), billingRun, errorMessages, interimAdvancePaymentParameters)) {
            billingRun.setCurrencyId(interimAndAdvancePaymentParameters.getCurrencyId());
        }

        checkCustomerDetails(interimAndAdvancePaymentParameters.getCustomerDetailId(), errorMessages, billingRun, interimAdvancePaymentParameters);

        checkContractDetailForIap(billingRun, interimAndAdvancePaymentParameters.getContractId(), interimAndAdvancePaymentParameters.getContractType(), errorMessages);

        checkBillingGroupsForIap(billingRun, interimAndAdvancePaymentParameters.getBillingGroupIds(), errorMessages, isUpdate, interimAndAdvancePaymentParameters.getContractId());

    }

    /**
     * Checks and validates the billing groups for the interim and advance payment parameters of a billing run.
     *
     * @param billingRun        The billing run object.
     * @param billingGroupIds   The set of billing group IDs to check.
     * @param errorMessages     The list to add any error messages encountered during the validation.
     * @param isUpdate          Indicates whether the billing run is being updated.
     * @param productContractId The ID of the product contract associated with the billing run.
     */
    private void checkBillingGroupsForIap(BillingRun billingRun, Set<Long> billingGroupIds, List<String> errorMessages, boolean isUpdate, Long productContractId) {
        if (!CollectionUtils.isEmpty(billingGroupIds)) {
            List<BillingRunBillingGroup> billingRunBillingGroupsToSave = new ArrayList<>();
            boolean isValid = true;

            Set<BillingGroupListingResponse> billingGroupListingResponses = billingGroupRepository.findAllIdAndContractIdAndStatus(new ArrayList<>(billingGroupIds), productContractId, EntityStatus.ACTIVE);

            if (billingGroupListingResponses.isEmpty())
                throw new DomainEntityNotFoundException("Contract billing group not found for given contract with id: %s".formatted(productContractId));


            Set<Long> idsFromDatabase = billingGroupRepository.findByIds(billingGroupIds, EntityStatus.ACTIVE);

            List<BillingRunBillingGroup> existingBillingRunBillingGroups = billingRunBillingGroupRepository.findByBillingRunId(billingRun.getId(), EntityStatus.ACTIVE);

            for (Long id : billingGroupIds) {
                if (!idsFromDatabase.contains(id)) {
                    errorMessages.add(String.format("interimAndAdvancePaymentParameters.billingGroupIds-[billingGroupIds] billing group with id %s not found", id));
                    isValid = false;
                } else {
                    BillingRunBillingGroup billingRunBillingGroup = createBillingRunBillingGroup(id, billingRun.getId());
                    billingRunBillingGroupsToSave.add(billingRunBillingGroup);
                }
            }

            if (isUpdate) {
                List<BillingRunBillingGroup> billingGroupsToDelete = new ArrayList<>();
                for (BillingRunBillingGroup billingRunBillingGroup : existingBillingRunBillingGroups) {
                    if (!billingGroupIds.contains(billingRunBillingGroup.getBillingGroupId())) {
                        billingRunBillingGroup.setStatus(EntityStatus.DELETED);
                        billingGroupsToDelete.add(billingRunBillingGroup);
                    }
                }
                billingRunBillingGroupRepository.saveAll(billingGroupsToDelete);

            }

            if (isValid) {
                billingRunBillingGroupRepository.saveAll(billingRunBillingGroupsToSave);
            }
        }
    }

    /**
     * Checks and validates the contract details for the interim and advance payment parameters of a billing run.
     *
     * @param billingRun    The billing run object.
     * @param contractId    The ID of the contract associated with the billing run.
     * @param contractType  The type of the contract (SERVICE_CONTRACT or PRODUCT_CONTRACT).
     * @param errorMessages The list to add any error messages encountered during the validation.
     */

    private void checkContractDetailForIap(BillingRun billingRun, Long contractId, ContractType contractType, List<String> errorMessages) {
        if (contractId != null) {
            switch (contractType) {
                case SERVICE_CONTRACT -> {
                    if (!serviceContractsRepository.existsByIdAndStatusIn(contractId, List.of(EntityStatus.ACTIVE))) {
                        errorMessages.add("interimAndAdvancePaymentParameters.contractId-[contractId] service contract detail not found");
                    } else {
                        billingRun.setServiceContractId(contractId);
                    }
                }
                case PRODUCT_CONTRACT -> {
                    if (!productContractRepository.existsByIdAndStatusIn(contractId, List.of(ProductContractStatus.ACTIVE))) {
                        errorMessages.add("interimAndAdvancePaymentParameters.contractId-[contractId] service contract detail not found");
                    } else {
                        billingRun.setProductContractId(contractId);
                    }
                }
            }
        } else if (billingRun.getProductContractId() != null || billingRun.getServiceContractId() != null) {
            billingRun.setProductContractId(null);
            billingRun.setServiceContractId(null);
        }
    }

    /**
     * Checks if the provided customer detail ID exists and is active, and sets the customer detail ID on the provided billing run object.
     *
     * @param customerDetailId The ID of the customer detail to check.
     * @param errorMessages    The list to add any error messages encountered during the validation.
     * @param billingRun       The billing run object to update with the customer detail ID.
     * @param requestName      The name of the request, used in the error message.
     */
    private void checkCustomerDetails(Long customerDetailId, List<String> errorMessages, BillingRun billingRun, String requestName) {
        if (!customerDetailsRepository.existsByDetailIdAndCustomerStatus(customerDetailId, List.of(CustomerStatus.ACTIVE))) {
            errorMessages.add(requestName + ".customerDetailId-[customerDetailId] customer detail not found");
        } else {
            billingRun.setCustomerDetailId(customerDetailId);
        }
    }

    /**
     * Checks if the provided currency ID exists and is active, and updates the billing run object with the currency ID.
     *
     * @param currencyId    The ID of the currency to check.
     * @param billingRun    The billing run object to update with the currency ID.
     * @param errorMessages The list to add any error messages encountered during the validation.
     * @param requestName   The name of the request, used in the error message.
     * @return true if the currency is valid, false otherwise.
     */
    private boolean checkCurrency(Long currencyId, BillingRun billingRun, List<String> errorMessages, String requestName) {
        if (!currencyId.equals(billingRun.getCurrencyId()) && !currencyRepository.existsByIdAndStatus(currencyId, NomenclatureItemStatus.ACTIVE)) {
            errorMessages.add(requestName + ".currencyId-[currencyId] currency not found");
            return false;
        }
        return true;
    }

    /**
     * Checks if the provided bank ID exists and is active, and updates the billing run object with the bank ID.
     *
     * @param billingRun    The billing run object to update with the bank ID.
     * @param bankId        The ID of the bank to check.
     * @param errorMessages The list to add any error messages encountered during the validation.
     * @param requestName   The name of the request, used in the error message.
     */
    private void checkBank(BillingRun billingRun, Long bankId, List<String> errorMessages, String requestName) {
        if (bankId != null && !bankId.equals(billingRun.getBankId())) {
            if (!bankRepository.existsByIdAndStatusIn(bankId, List.of(NomenclatureItemStatus.ACTIVE))) {
                errorMessages.add(requestName + ".bankId-[bankId] bank not found");
            }
        }
        billingRun.setBankId(bankId);
    }

    /**
     * Checks if the provided applicable interest rate ID exists and is active, and updates the billing run object with the interest rate ID.
     *
     * @param billingRun               The billing run object to update with the interest rate ID.
     * @param applicableInterestRateId The ID of the applicable interest rate to check.
     * @param errorMessages            The list to add any error messages encountered during the validation.
     * @param requestName              The name of the request, used in the error message.
     */
    private void checkApplicableInterestRate(BillingRun billingRun, Long applicableInterestRateId, List<String> errorMessages, String requestName) {
        if (applicableInterestRateId != null) {
            if (!interestRateRepository.existsByIdAndStatusIn(applicableInterestRateId, List.of(InterestRateStatus.ACTIVE))) {
                errorMessages.add(requestName + ".applicableInterestRateId-[applicableInterestRateId] interest rate not found");
            }
        }
        billingRun.setInterestRateId(applicableInterestRateId);
    }

    /**
     * Creates a new {@link BillingRunBillingGroup} instance with the provided billing group ID and billing run ID, and sets the status to {@link EntityStatus#ACTIVE}.
     *
     * @param billingGroupId The ID of the billing group to associate with the billing run.
     * @param billingRunId   The ID of the billing run to associate with the billing group.
     * @return A new {@link BillingRunBillingGroup} instance with the provided IDs and active status.
     */
    private BillingRunBillingGroup createBillingRunBillingGroup(Long billingGroupId, Long billingRunId) {
        BillingRunBillingGroup billingRunBillingGroup = new BillingRunBillingGroup();
        billingRunBillingGroup.setBillingGroupId(billingGroupId);
        billingRunBillingGroup.setBillingRunId(billingRunId);
        billingRunBillingGroup.setStatus(EntityStatus.ACTIVE);
        return billingRunBillingGroup;
    }

    /**
     * Checks if the provided invoice date is within the accounting period date range.
     *
     * @param errorMessages             The list to add any error messages encountered during the validation.
     * @param accountingPeriodsResponse The response containing the accounting period start and end dates.
     * @param invoiceDate               The invoice date to check.
     */
    private void checkAccountingPeriodDateRange(List<String> errorMessages, AccountingPeriodsResponse accountingPeriodsResponse, LocalDate invoiceDate) {
        if (invoiceDate.isBefore(ChronoLocalDate.from(accountingPeriodsResponse.getStartDate())) ||
                invoiceDate.isAfter(ChronoLocalDate.from(accountingPeriodsResponse.getEndDate()))) {
            errorMessages.add("commonParameters.invoiceDate-[invoiceDate] should be in accounting period range: from %s to %s".formatted(accountingPeriodsResponse.getStartDate(), accountingPeriodsResponse.getEndDate()));
        }
    }

    /**
     * Checks if the provided tax event date is within the accounting period date range and adds an error message to the provided list if it is not.
     *
     * @param accountingPeriodsResponse The response containing the accounting period start and end dates.
     * @param taxEventDate              The tax event date to check.
     * @param errorMessages             The list to add any error messages encountered during the validation.
     */
    private void checkManualCreditOrDebitNoteTaxEventDate(AccountingPeriodsResponse accountingPeriodsResponse, LocalDate taxEventDate, List<String> errorMessages) {
        if (taxEventDate.isBefore(LocalDate.now()) && (taxEventDate.isBefore(ChronoLocalDate.from(accountingPeriodsResponse.getStartDate())) ||
                taxEventDate.isAfter(ChronoLocalDate.from(accountingPeriodsResponse.getEndDate())))) {
            errorMessages.add("commonParameters.taxEventDate- [taxEventDate] should be in accounting period range: from %s to %s".formatted(accountingPeriodsResponse.getStartDate(), accountingPeriodsResponse.getEndDate()));
        }
    }

    /**
     * Generates a unique billing run number based on the current date and a sequential counter.
     * <p>
     * The billing run number is generated in the format "BILLING_RUN_NUMBER_PREFIX + currentDate + sequentialNumber", where:
     * - BILLING_RUN_NUMBER_PREFIX is a constant prefix for the billing run number
     * - currentDate is the current date in the format "yyyyMMdd"
     * - sequentialNumber is a 4-digit sequential number starting from 1, padded with leading zeros if necessary
     * <p>
     * If the maximum sequential number (9999) has been reached for the current date, a ClientException is thrown.
     *
     * @return A unique billing run number in the format described above.
     * @throws ClientException if the maximum sequential number has been reached for the current date.
     */
    private String generateBillingNumber() {
        LocalDate currentDate = LocalDate.now();
        LocalDateTime startOfDay = currentDate.atStartOfDay();
        LocalDateTime startOfNextDay = currentDate.plusDays(1).atStartOfDay();
        Long count = billingRunRepository.countBillingRunByCreateDate(startOfDay, startOfNextDay);
        if (count >= MAX_BILLING_RUN_NUMBER_PREFIX) {
            throw new ClientException("Can't crete billing run number , because max billing run suffix can be 9999;", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        String nextSequenceValue = String.format("%04d", count + 1);
        String dataPadding = DateTimeFormatter.ofPattern("yyyyMMdd").format(currentDate);
        String numberPadding = "0".repeat(Math.max(0, 6 - nextSequenceValue.length()));
        return "%s%s%s".formatted(BILLING_RUN_NUMBER_PREFIX, dataPadding, nextSequenceValue);
    }

    /**
     * Retrieves the details of a billing run by its ID.
     * <p>
     * This method fetches the billing run with the specified ID from the database, and returns a {@link BillingRunResponse} object containing the common parameters, process periodicity, tasks, and billing run-specific parameters (e.g. standard billing, invoice correction, manual invoice, etc.) for the retrieved billing run.
     * <p>
     * If the billing run is not found, a {@link ClientException} is thrown with the error code {@link ErrorCode#CONFLICT}.
     * If the user does not have the necessary permission to view the billing run, a {@link ClientException} is thrown with the error code {@link ErrorCode#OPERATION_NOT_ALLOWED}.
     * If the billing run has been deleted and the user does not have the necessary permission to view deleted billing runs, a {@link ClientException} is thrown with the error code {@link ErrorCode#ACCESS_DENIED}.
     *
     * @param id The ID of the billing run to retrieve.
     * @return A {@link BillingRunResponse} object containing the details of the retrieved billing run.
     * @throws ClientException if the billing run is not found, the user does not have permission to view the billing run, or the user does not have permission to view a deleted billing run.
     */
    public BillingRunResponse view(Long id) {
        log.info("view for billing run with id: %s ".formatted(id));
        List<String> errorMessage = new ArrayList<>();
        BillingRun billingRun = billingRunRepository.findById(id)
                .orElseThrow(() -> new ClientException("Can't find Billing run with id: %s;".formatted(id), ErrorCode.CONFLICT));
        if (!checkBillingPermission(BillingPermissions.VIEW, billingRun.getType())) {
            throw new ClientException("Can't view billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        if (billingRun.getStatus().equals(BillingStatus.DELETED)) {
            if (!hasDeletedPermission()) {
                throw new ClientException("You don't have View deleted Billing run Permission;", ErrorCode.ACCESS_DENIED);
            }
        }
        BillingRunResponse billingRunResponse = new BillingRunResponse();
        billingRunResponse.setCommonParameters(getCommonParameters(billingRun));
        setProcessPeriodicity(billingRun, errorMessage, billingRunResponse.getCommonParameters());
        setTasks(billingRunResponse, billingRun);
        AccountManager accountManager = accountManagerRepository.findById(billingRun.getEmployeeId()).orElseThrow(() -> new DomainEntityNotFoundException("Employee not found!;"));
        billingRunResponse.setEmployeeId("%s, %s (%s)".formatted(accountManager.getLastName(), accountManager.getFirstName(), accountManager.getUserName()));

        switch (billingRun.getType()) {
            case STANDARD_BILLING -> billingRunResponse.setStandardBillingRunParameters(getBasicParameters(billingRun));
            case INVOICE_CORRECTION ->
                    billingRunResponse.setInvoiceCorrectionBillingRunParametersResponse(getInvoiceCorrectionParameters(billingRun));
            case MANUAL_INVOICE ->
                    billingRunResponse.setManualInvoiceBillingRunParameters(manualInvoiceService.getManualInvoiceParameters(billingRun));
            case MANUAL_CREDIT_OR_DEBIT_NOTE ->
                    billingRunResponse.setManualCreditOrDebitNoteBillingRunParametersResponse(manualCreditOrDebitNoteService.getManualCreditOrDebitNoteParameters(billingRun));
            case MANUAL_INTERIM_AND_ADVANCE_PAYMENT ->
                    billingRunResponse.setManualInterimAndAdvancePaymentParametersResponse(manualInterimAndAdvancePaymentMapperService.mapInterimAndAdvancePaymentParameters(billingRun));
            case INVOICE_REVERSAL ->
                    billingRunResponse.setInvoiceReversalBillingRunParametersResponse(getInvoiceReversalParameters(billingRun));
        }
        return billingRunResponse;
    }

    /**
     * Deletes a billing run by the specified ID.
     * <p>
     * This method first checks if the billing run exists and if the user has the necessary permission to delete it. If the billing run is already deleted, an {@link OperationNotAllowedException} is thrown.
     * <p>
     * For certain billing run types (MANUAL_CREDIT_OR_DEBIT_NOTE, MANUAL_INTERIM_AND_ADVANCE_PAYMENT, INVOICE_CORRECTION, INVOICE_REVERSAL, MANUAL_INVOICE, STANDARD_BILLING), the method checks if the billing run status is "INITIAL". If not, an {@link OperationNotAllowedException} is thrown.
     * <p>
     * For MANUAL_CREDIT_OR_DEBIT_NOTE billing runs, the method updates the status of the associated billing run invoices. For INVOICE_CORRECTION and INVOICE_REVERSAL billing runs, the method updates the file status of the associated billing invoices.
     * <p>
     * Finally, the method sets the status of the billing run to "DELETED" and saves the updated billing run.
     *
     * @param id The ID of the billing run to be deleted.
     * @return The ID of the deleted billing run.
     * @throws DomainEntityNotFoundException if the billing run is not found.
     * @throws ClientException               if the user does not have the necessary permission to delete the billing run.
     * @throws OperationNotAllowedException  if the billing run is already deleted or if the billing run status is not "INITIAL" for certain billing run types.
     */
    @Transactional
    public Long delete(Long id) {
        log.info("Deleting billing run with id: {}", id);

        BillingRun billingRun = billingRunRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run not found by ID %s;".formatted(id)));
        BillingType billingType = Objects.requireNonNull(billingRun.getType());
        if (!checkBillingPermission(BillingPermissions.DELETE, billingType)) {
            throw new ClientException("Can't delete billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        if (billingRun.getStatus().equals(BillingStatus.DELETED)) {
            log.error("Billing run with id: {} is already deleted", id);
            throw new OperationNotAllowedException("Billing run is already deleted;");
        }


        if (List.of(BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE, BillingType.MANUAL_INTERIM_AND_ADVANCE_PAYMENT,
                        BillingType.INVOICE_CORRECTION, INVOICE_REVERSAL, BillingType.MANUAL_INVOICE, BillingType.STANDARD_BILLING)
                .contains(billingType)) {
            if (!billingRun.getStatus().equals(BillingStatus.INITIAL)) {
                log.error("Deleting operation is enabled only if status of Billing Run is Initial");
                throw new OperationNotAllowedException("Deleting operation is enabled only if status of Billing Run is Initial;");
            }

            if (billingType.equals(BillingType.MANUAL_CREDIT_OR_DEBIT_NOTE)) {
                billingRunInvoicesRepository.updateStatusByBillingId(id);
            }

            if (billingType.equals(BillingType.INVOICE_CORRECTION) || billingType.equals(INVOICE_REVERSAL)) {
                billingInvoicesFileRepository.updateFileStatusByBillingId(id);
            }

            billingRun.setStatus(BillingStatus.DELETED);
            billingRunRepository.save(billingRun);
        }

        return id;
    }

    /**
     * Retrieves the common parameters for a billing run.
     *
     * @param billingRun The billing run to retrieve the common parameters for.
     * @return A BillingRunCommonParametersResponse object containing the common parameters for the billing run.
     */
    private BillingRunCommonParametersResponse getCommonParameters(BillingRun billingRun) {
        BillingRunCommonParametersResponse commonParameters = new BillingRunCommonParametersResponse();
        commonParameters.setId(billingRun.getId());
        commonParameters.setBillingNumber(billingRun.getBillingNumber());
        commonParameters.setRunPeriodicity(billingRun.getRunPeriodicity());
        commonParameters.setAdditionalInfo(billingRun.getAdditionalInfo());
        commonParameters.setType(billingRun.getType());
        commonParameters.setStatus(billingRun.getStatus());
        commonParameters.setTaxEventDate(billingRun.getTaxEventDate());
        commonParameters.setInvoiceDate(billingRun.getInvoiceDate());
        AccountingPeriods accountingPeriods = getAccountingPeriods(billingRun.getAccountingPeriodId());
        if (accountingPeriods != null) {
            commonParameters.setAccountingPeriodId(accountingPeriods.getId());
            commonParameters.setAccountingPeriodName(accountingPeriods.getName());
        }
        commonParameters.setInvoiceDueDateType(billingRun.getInvoiceDueDateType());
        commonParameters.setInvoiceDueDate(billingRun.getInvoiceDueDate());
        commonParameters.setSendingAnInvoice(billingRun.getSendingAnInvoice());
        commonParameters.setExecutionType(billingRun.getExecutionType());
        commonParameters.setExecutionDate(billingRun.getExecutionDate());
        commonParameters.setRunStages(billingRun.getRunStages());
        contractTemplateRepository.findTemplateResponseById(billingRun.getTemplateId(), LocalDate.now())
                .ifPresent(commonParameters::setTemplateResponse);
        contractTemplateRepository.findTemplateResponseById(billingRun.getEmailTemplateId(), LocalDate.now())
                .ifPresent(commonParameters::setEmailTemplateResponse);
        commonParameters.setNotificationResponses(getNotificationResponse(billingRun.getId()));
        return commonParameters;
    }

    /**
     * Sets the process periodicity for a billing run.
     *
     * @param billingRun               The billing run to set the process periodicity for.
     * @param errorMessage             A list to store any error messages encountered during the process.
     * @param commonParametersResponse The common parameters response object to set the process periodicity in.
     */
    private void setProcessPeriodicity(BillingRun billingRun, List<String> errorMessage, BillingRunCommonParametersResponse commonParametersResponse) {
        List<BillingRunProcessPeriodicityResponse> processPeriodicity = new ArrayList<>();
        List<BillingProcessPeriodicity> processPeriodicityList = billingProcessPeriodicityRepository.findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
        if (!CollectionUtils.isEmpty(processPeriodicityList)) {
            for (BillingProcessPeriodicity item : processPeriodicityList) {
                Optional<ProcessPeriodicity> processPeriodicityOptional = processPeriodicityRepository.findByIdAndStatus(item.getProcessPeriodicityId(), EntityStatus.ACTIVE);
                if (processPeriodicityOptional.isPresent()) {
                    BillingRunProcessPeriodicityResponse processPeriodicityResponse = new BillingRunProcessPeriodicityResponse();
                    processPeriodicityResponse.setId(item.getProcessPeriodicityId());
                    processPeriodicityResponse.setName(processPeriodicityOptional.get().getName());
                    processPeriodicity.add(processPeriodicityResponse);
                } else {
                    errorMessage.add("Can't find Active ProcessPeriodicity with id: %s;".formatted(item.getProcessPeriodicityId()));
                }
            }
/*            List<BillingRunProcessPeriodicityResponse> processPeriodicity = processPeriodicityList.stream()
                    .map(x -> new BillingRunProcessPeriodicityResponse(x.getId(), x.getBillingId()))
                    .collect(Collectors.toList());*/
        }
        if (!CollectionUtils.isEmpty(processPeriodicity)) {
            commonParametersResponse.setProcessPeriodicity(processPeriodicity);
        }
    }

    /**
     * Sets the tasks associated with a billing run.
     *
     * @param billingRunResponse The billing run response object to set the tasks in.
     * @param billingRun         The billing run to retrieve the tasks for.
     */
    private void setTasks(BillingRunResponse billingRunResponse, BillingRun billingRun) {
        List<BillingRunTasks> billingRunTasks = billingRunTasksRepository.findByBillingIdAndStatusIn(billingRun.getId(), List.of(EntityStatus.ACTIVE));
        if (!CollectionUtils.isEmpty(billingRunTasks)) {
            List<BillingRunTasksResponse> billingRunTasksResponses = new ArrayList<>();
            for (BillingRunTasks item : billingRunTasks) {
                BillingRunTasksResponse billingRunTasksResponse = new BillingRunTasksResponse();
                billingRunTasksResponse.setId(item.getId());
                billingRunTasksResponse.setCreateDate(item.getCreateDate());
                Task task = getTaskName(item.getTaskId());
                if (task != null) {
                    TaskType taskType = getTaskType(task.getTaskTypeId());
                    billingRunTasksResponse.setNumber(task.getNumber());
                    if (taskType != null) {
                        billingRunTasksResponse.setTaskType(new TaskTypeShortResponse(taskType));
                    }
                }
                billingRunTasksResponses.add(billingRunTasksResponse);
            }
            billingRunResponse.setBillingRunTasks(billingRunTasksResponses);
        }
    }

    /**
     * Retrieves the task type for the given task ID.
     *
     * @param id The ID of the task type to retrieve.
     * @return The task type if found, or null if not found.
     */
    private TaskType getTaskType(Long id) {
        Optional<TaskType> taskType = taskTypeRepository.findByIdAndStatusIn(id, List.of(NomenclatureItemStatus.ACTIVE));
        return taskType.orElse(null);
    }

    /**
     * Retrieves the task with the given ID.
     *
     * @param taskId The ID of the task to retrieve.
     * @return The task if found, or null if not found.
     */
    private Task getTaskName(Long taskId) {
        Optional<Task> task = taskRepository.findById(taskId);
        return task.orElse(null);
    }

    /**
     * Checks if the current user has permission to view deleted billing runs.
     *
     * @return true if the user has the "VIEW_DELETED_BILLING_RUN" permission, false otherwise.
     */
    private boolean hasDeletedPermission() {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.BILLING_RUN, List.of(PermissionEnum.VIEW_DELETED_BILLING_RUN));
    }

    /**
     * Sets additional parameters for the standard billing run, including conditions information, maximum end date, and periodic maximum end date and value.
     *
     * @param billingRun the billing run for which to set the parameters
     * @return a StandardBillingRunParametersResponse object containing the set parameters
     */
    private StandardBillingRunParametersResponse getBasicParameters(BillingRun billingRun) {
        StandardBillingRunParametersResponse basicParameters = new StandardBillingRunParametersResponse();
        basicParameters.setBillingCriteria(billingRun.getBillingCriteria());
        basicParameters.setApplicationLevel(billingRun.getApplicationLevel());
        basicParameters.setCustomerContractOrPodConditions(billingRun.getCustomerContractOrPodConditions());
        basicParameters.setCustomerContractOrPodList(billingRun.getCustomerContractOrPodList());
        basicParameters.setApplicationModelType(billingRun.getApplicationModelType());
        basicParameters.setConditionsInfo(billingRunConditionService.getConditionsInfo(billingRun.getCustomerContractOrPodConditions()));
        basicParameters.setMaxEndDate(billingRun.getMaxEndDate());
        basicParameters.setPeriodicMaxEndDate(billingRun.getPeriodicMaxEndDate());
        basicParameters.setPeriodicMaxEndDateValue(billingRun.getPeriodicMaxEndDateValue());
        basicParameters.setSumFiles(sumFileRepository.findAllByBillingId(billingRun.getId()).stream().map(x -> new ShortResponse(x.getId(), x.getName())).toList());
        return basicParameters;
    }

    /**
     * Retrieves the parameters for an invoice reversal billing run.
     *
     * @param billingRun the billing run for which to retrieve the invoice reversal parameters
     * @return an InvoiceReversalBillingRunParametersResponse object containing the retrieved parameters
     */
    private InvoiceReversalBillingRunParametersResponse getInvoiceReversalParameters(BillingRun billingRun) {
        InvoiceReversalBillingRunParametersResponse parameters = new InvoiceReversalBillingRunParametersResponse();
        parameters.setListOfInvoices(billingRun.getListOfInvoices());
        Optional<BillingInvoicesFile> billingInvoicesFile = billingInvoicesFileRepository.findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
        if (billingInvoicesFile.isPresent()) {
            parameters.setFileName(billingInvoicesFile.get().getName().split("_")[1]);
            parameters.setFileId(billingInvoicesFile.get().getId());
        }
        return parameters;
    }

    /**
     * Retrieves the parameters for an invoice correction billing run.
     *
     * @param billingRun the billing run for which to retrieve the invoice correction parameters
     * @return an InvoiceCorrectionBillingRunParametersResponse object containing the retrieved parameters
     */
    private InvoiceCorrectionBillingRunParametersResponse getInvoiceCorrectionParameters(BillingRun billingRun) {
        InvoiceCorrectionBillingRunParametersResponse parameters = new InvoiceCorrectionBillingRunParametersResponse();
        parameters.setListOfInvoices(billingRun.getListOfInvoices());
        parameters.setPriceChanges(billingRun.getPriceChange());
        parameters.setVolumeChange(billingRun.getVolumeChange());
        Optional<BillingInvoicesFile> billingInvoicesFile = billingInvoicesFileRepository.findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
        if (billingInvoicesFile.isPresent()) {
            parameters.setFileName(billingInvoicesFile.get().getName().split("_")[1]);
            parameters.setFileId(billingInvoicesFile.get().getId());
        }
        return parameters;
    }

    /**
     * Retrieves an {@link AccountingPeriods} entity by its ID, or returns null if not found.
     *
     * @param accountPeriodId the ID of the {@link AccountingPeriods} entity to retrieve
     * @return the {@link AccountingPeriods} entity if found, or null if not found
     */
    private AccountingPeriods getAccountingPeriods(Long accountPeriodId) {
        if (accountPeriodId != null) {
            Optional<AccountingPeriods> accountingPeriodsOptional = accountingPeriodsRepository.findById(accountPeriodId);
            return accountingPeriodsOptional.orElse(null);
        } else return null;
    }

    /**
     * Retrieves a paginated list of billing runs based on the provided request parameters.
     *
     * @param request the request parameters for filtering and sorting the billing run list
     * @return a page of {@link BillingRunListingResponse} objects representing the filtered and sorted billing runs
     */
    public Page<BillingRunListingResponse> standardList(BillingRunListingRequest request) {


        PageRequest pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(
                        new Sort.Order(request.getDirection(), getSorByEnum(request.getColumns()))
                )
        );

        List<BillingStatus> statuses = request.getStatuses();
        List<BillingType> billingTypes = CollectionUtils.isEmpty(request.getBillingTypes()) ?
                Arrays.stream(BillingType.values())
                        .filter(bt -> permissionService.getPermissionsFromContext(BILLING_RUN).contains(BillingPermissions.VIEW.getRelevantPermission(bt).getId()))
                        .toList() :
                request.getBillingTypes().stream()
                        .filter(bt -> permissionService.getPermissionsFromContext(BILLING_RUN).contains(BillingPermissions.VIEW.getRelevantPermission(bt).getId()))
                        .toList();
        if (CollectionUtils.isEmpty(billingTypes) && CollectionUtils.isNotEmpty(request.getBillingTypes())) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        if (statuses != null) {
            if (!hasDeletedPermission() && containsOnlyDeletedStatus(statuses)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            statuses = statuses.stream()
                    .filter(status -> status != BillingStatus.DELETED || hasDeletedPermission())
                    .toList();
        } else {
            statuses = Arrays.stream(BillingStatus.values())
                    .filter(status -> status != BillingStatus.DELETED || hasDeletedPermission())
                    .toList();
        }

        return billingRunRepository
                .filter(
                        getSearchByEnum(request.getSearchFields()),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBListUtils.convertEnumListToDBEnumArray(request.getAutomations()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getExecutionType()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(billingTypes),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getBillingCriteria()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getApplicationLevels()),
                        request.getInvoiceDueDateFrom(),
                        request.getInvoiceDueDateTo(),
                        request.getInvoiceDateFrom(),
                        request.getInvoiceDateTo(),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(statuses),
                        pageable
                )
                .map(BillingRunListingResponse::new);
    }

    /**
     * Checks if the provided list of billing statuses contains only the DELETED status.
     *
     * @param statuses the list of billing statuses to check
     * @return true if the list contains only the DELETED status, false otherwise
     */
    private boolean containsOnlyDeletedStatus(List<BillingStatus> statuses) {
        return statuses.size() == 1 && statuses.get(0).equals(BillingStatus.DELETED);
    }

    /**
     * Updates an existing billing run with the provided request.
     *
     * @param id      the ID of the billing run to update
     * @param request the request containing the updated billing run details
     * @return the ID of the updated billing run
     * @throws ClientException if the billing run cannot be found, the user does not have permission to edit the billing run, or there are any other errors during the update process
     */
    @Transactional
    public Long update(Long id, BillingRunEditRequest request) {
        log.info("updating billing run with id: %s;".formatted(request));
        List<String> errorMessages = new ArrayList<>();
        BillingRun billingRun = billingRunRepository.findById(id)
                .orElseThrow(() -> new ClientException("Can't find Billing run with id: %s;".formatted(id), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));
        if (!checkBillingPermission(BillingPermissions.EDIT, billingRun.getType())) {
            throw new ClientException("Can't edit billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (!isPriceVolumeChangeValid(request.getBillingType(), request.getInvoiceCorrectionParameters())) {
            throw new IllegalArgumentsProvidedException("Price or Volume change - at least one of them should be true");
        }

        checkEditRequestValidity(billingRun, request, errorMessages);
        checkBillingRunStatusAndPermission(billingRun, request.getBillingType());
        checkBillingRunType(request.getBillingType(), billingRun);

        if (billingRun.getStatus().equals(BillingStatus.INITIAL)) {
            updateCommonParametersForEdit(billingRun, request, errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            addEditRequestPeriodicity(billingRun, request, errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        }
        addEditTaskToBillingRun(billingRun, request.getTaskId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        BillingRunCommonParameters commonParameters = request.getCommonParameters();
        switch (request.getBillingType()) {
            case STANDARD_BILLING -> {
                checkTaxEventDate(commonParameters, errorMessages);
                checkCondition(request.getBasicParameters().getBillingCriteria(),
                        request.getBasicParameters().getCustomersContractOrPODConditions(),
                        errorMessages);
                updateBasicParametersForEdit(billingRun, request.getBasicParameters(), errorMessages);
            }
            case MANUAL_INVOICE -> {
                checkTaxEventDate(commonParameters, errorMessages);
                manualInvoiceService.updateManualInvoiceParametersForEdit(request.getManualInvoiceParameters(), billingRun, errorMessages);
            }
            case INVOICE_CORRECTION -> {
                mapInvoiceCorrectionParameters(request.getInvoiceCorrectionParameters(), billingRun, errorMessages);
                checkTaxEventDate(commonParameters, errorMessages);
                checkInvoiceDate(commonParameters, errorMessages);
            }
            case MANUAL_INTERIM_AND_ADVANCE_PAYMENT -> {
                checkTaxEventDate(commonParameters, errorMessages);
                mapInterimAndAdvancePaymentParameters(billingRun, request.getInterimAndAdvancePaymentParameters(), errorMessages, true);
            }
            case INVOICE_REVERSAL -> {
                mapInvoiceReversalParameters(request.getInvoiceReversalParameters(), billingRun, errorMessages);
            }
            case MANUAL_CREDIT_OR_DEBIT_NOTE -> {
                manualCreditOrDebitNoteService.edit(request, billingRun, errorMessages);
            }
        }
        validateAndSetTemplate(commonParameters.getTemplateId(), billingRun, errorMessages);
        validateAndSetEmailTemplate(commonParameters.getEmailTemplateId(), billingRun, errorMessages);
        updateBillingNotifications(commonParameters.getBillingNotifications(), billingRun, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        billingRunRepository.save(billingRun);
        return billingRun.getId();
    }

    /**
     * while editing billing run type, clear specific data for each type
     */
    private void checkBillingRunType(BillingType billingType, BillingRun billingRun) {
        if (!billingType.equals(billingRun.getType())) {
            switch (billingRun.getType()) {
                case STANDARD_BILLING -> {
                    billingRun.setApplicationModelType(null);
                    billingRun.setBillingCriteria(null);
                    billingRun.setApplicationLevel(null);
                    billingRun.setCustomerContractOrPodList(null);
                    billingRun.setCustomerContractOrPodConditions(null);
                }
                case MANUAL_INTERIM_AND_ADVANCE_PAYMENT -> {
                    billingRun.setAmountExcludingVat(null);
                    billingRun.setIssuingForTheMonthToCurrent(null);
                    billingRun.setIssuedSeparateInvoices(null);
                    billingRun.setCurrencyId(null);
                    billingRun.setDeductionFrom(null);
                    clearCommonData(billingRun);
                    billingRun.setCustomerDetailId(null);
                    billingRun.setProductContractId(null);
                    billingRun.setServiceContractId(null);
                    List<BillingRunBillingGroup> billingGroups = billingRunBillingGroupRepository.findByBillingRunId(billingRun.getId(), EntityStatus.ACTIVE);
                    billingGroups.forEach(group -> group.setStatus(EntityStatus.DELETED));
                    billingRunBillingGroupRepository.saveAll(billingGroups);
                    billingRun.setCustomerCommunicationId(null);
                }
                case MANUAL_INVOICE -> {
                    billingRun.setBasisForIssuing(null);
                    clearCommonData(billingRun);
                    billingRun.setCustomerDetailId(null);
                    billingRun.setGoodsOrderId(null);
                    billingRun.setServiceOrderId(null);
                    billingRun.setProductContractId(null);
                    billingRun.setServiceContractId(null);
                    Optional<BillingRunBillingGroup> billingRunBillingGroupOptional = billingRunBillingGroupRepository.findByBillingRunIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
                    if (billingRunBillingGroupOptional.isPresent()) {
                        BillingRunBillingGroup billingRunBillingGroup = billingRunBillingGroupOptional.get();
                        billingRunBillingGroup.setStatus(EntityStatus.DELETED);
                        billingRunBillingGroupRepository.save(billingRunBillingGroup);
                    }
                    billingRun.setCustomerCommunicationId(null);
                    List<BillingSummaryData> summaryData = billingSummaryDataRepository.findByBillingId(billingRun.getId());
                    List<BillingDetailedData> detailedData = billingDetailedDataRepository.findByBillingId(billingRun.getId());
                    billingSummaryDataRepository.deleteAll(summaryData);
                    billingDetailedDataRepository.deleteAll(detailedData);
                }
                case MANUAL_CREDIT_OR_DEBIT_NOTE -> {
                    billingRun.setBasisForIssuing(null);
                    clearCommonData(billingRun);
                    billingRun.setDocumentType(null);
                    List<BillingRunInvoices> billingInvoices = billingRunInvoicesRepository.findAllByBillingId(billingRun.getId());
                    billingInvoices.forEach(inv -> inv.setStatus(EntityStatus.DELETED));
                    billingRunInvoicesRepository.saveAll(billingInvoices);
                    List<BillingSummaryData> summaryData = billingSummaryDataRepository.findByBillingId(billingRun.getId());
                    List<BillingDetailedData> detailedData = billingDetailedDataRepository.findByBillingId(billingRun.getId());
                    billingSummaryDataRepository.deleteAll(summaryData);
                    billingDetailedDataRepository.deleteAll(detailedData);
                }
                case INVOICE_CORRECTION -> {
                    billingRun.setPriceChange(null);
                    billingRun.setListOfInvoices(null);
                    Optional<BillingInvoicesFile> billingInvoicesFile = billingInvoicesFileRepository.findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
                    billingInvoicesFile.ifPresent(file -> {
                        file.setStatus(EntityStatus.DELETED);
                        billingInvoicesFileRepository.save(file);
                    });
                }
                case INVOICE_REVERSAL -> {
                    billingRun.setListOfInvoices(null);
                    Optional<BillingInvoicesFile> billingInvoicesFile = billingInvoicesFileRepository.findByBillingIdAndStatus(billingRun.getId(), EntityStatus.ACTIVE);
                    billingInvoicesFile.ifPresent(file -> {
                        file.setStatus(EntityStatus.DELETED);
                        billingInvoicesFileRepository.save(file);
                    });
                }
            }
            billingRunRepository.save(billingRun);
        }
    }

    /**
     * Clears the common data fields of a BillingRun object.
     * <p>
     * This method sets the following fields of the BillingRun object to null:
     * - numberOfIncomeAccount
     * - costCenterControllingOrder
     * - vatRateId
     * - globalVatRate
     * - interestRateId
     * - directDebit
     * - bankId
     * - iban
     * <p>
     * This method is used to reset the common data fields of a BillingRun object when certain billing run types are processed.
     *
     * @param billingRun the BillingRun object to clear the common data fields for
     */
    private void clearCommonData(BillingRun billingRun) {
        billingRun.setNumberOfIncomeAccount(null);
        billingRun.setCostCenterControllingOrder(null);
        billingRun.setVatRateId(null);
        billingRun.setGlobalVatRate(null);
        billingRun.setInterestRateId(null);
        billingRun.setDirectDebit(null);
        billingRun.setBankId(null);
        billingRun.setIban(null);
    }

    /**
     * Updates the common parameters of a BillingRun object based on the provided BillingRunEditRequest.
     * <p>
     * This method sets the following fields of the BillingRun object:
     * - runPeriodicity
     * - additionalInfo
     * - type
     * - taxEventDate
     * - invoiceDueDateType
     * - invoiceDueDate
     * - sendingAnInvoice
     * - executionType
     * - executionDate
     * - runStages
     * <p>
     * It also checks the accounting period and invoice date based on the provided parameters and adds any errors to the errorMassages list.
     *
     * @param billingRun    the BillingRun object to update
     * @param request       the BillingRunEditRequest containing the new common parameters
     * @param errorMassages a list to store any error messages
     */
    private void updateCommonParametersForEdit(BillingRun billingRun, BillingRunEditRequest request, List<String> errorMassages) {
        BillingRunCommonParameters commonParameters = request.getCommonParameters();
        billingRun.setRunPeriodicity(commonParameters.getPeriodicity());
        billingRun.setAdditionalInfo(commonParameters.getAdditionalInformation());
        billingRun.setType(request.getBillingType());
        billingRun.setTaxEventDate(commonParameters.getTaxEventDate());
        checkAccountingPeriodAndInvoiceDate(billingRun, commonParameters.getAccountingPeriodId(), commonParameters.getInvoiceDate(), errorMassages);
        billingRun.setInvoiceDueDateType(commonParameters.getInvoiceDueDate());
        billingRun.setInvoiceDueDate(commonParameters.getDueDate());
        billingRun.setSendingAnInvoice(commonParameters.getSendingAnInvoice());
        billingRun.setExecutionType(commonParameters.getExecutionType());
        billingRun.setExecutionDate(commonParameters.getExecutionDateAndTime());
        billingRun.setRunStages(commonParameters.getRunStages());
    }

    /**
     * Adds or updates the process periodicities associated with the given BillingRun based on the provided BillingRunEditRequest.
     * <p>
     * This method performs the following actions:
     * 1. Removes all process periodicities associated with the BillingRun that are not included in the request.
     * 2. For each process periodicity ID in the request:
     * - Checks if the process periodicity is active and can be bound to the BillingRun.
     * - If the process periodicity is not already associated with the BillingRun, creates a new BillingProcessPeriodicity record.
     * - If the process periodicity is already associated with the BillingRun, updates the modification date of the existing BillingProcessPeriodicity record.
     * 3. Adds any error messages to the provided errorMassages list.
     *
     * @param updatedBillingRun the BillingRun object to update
     * @param request           the BillingRunEditRequest containing the new process periodicity IDs
     * @param errorMassages     a list to store any error messages
     */
    private void addEditRequestPeriodicity(BillingRun updatedBillingRun, BillingRunEditRequest request, List<String> errorMassages) {
        List<Long> processPeriodicityIds = request.getCommonParameters().getProcessPeriodicityIds();
        if (!CollectionUtils.isEmpty(processPeriodicityIds)) {
            removeAllProcessPeriodicitiesOtherThanRequestIds(processPeriodicityIds);
            for (int i = 0; i < processPeriodicityIds.size(); i++) {
                Long id = processPeriodicityIds.get(i);
                Optional<ProcessPeriodicity> processPeriodicityOptional =
                        processPeriodicityRepository.findByIdAndStatus(id, EntityStatus.ACTIVE);
                if (processPeriodicityOptional.isPresent()) {
                    ProcessPeriodicity processPeriodicity = processPeriodicityOptional.get();
                    Optional<BillingProcessPeriodicity> billingProcessPeriodicityOptional =
                            billingProcessPeriodicityRepository.findByBillingIdAndProcessPeriodicityIdAndStatusIn(
                                    updatedBillingRun.getId(),
                                    processPeriodicity.getId(),
                                    List.of(EntityStatus.ACTIVE)
                            );
                    checkProcessPeriodicityBoundToTheBillingRun(updatedBillingRun, processPeriodicity, errorMassages, i);
                    if (billingProcessPeriodicityOptional.isPresent()) {
                        BillingProcessPeriodicity billingProcessPeriodicity = billingProcessPeriodicityOptional.get();
                        billingProcessPeriodicity.setModifyDate(LocalDateTime.now());
                    } else {
                        createBillingRunProcessPeriodicity(updatedBillingRun, processPeriodicity);
                    }
                } else
                    errorMassages.add("processPeriodicityIds[%s]-Can't find active processPeriodicity with id:%s;".formatted(i, processPeriodicityIds));
            }
        }
    }

    /**
     * Checks if the given process periodicity can be bound to the updated billing run.
     * This method performs the following checks:
     * 1. Checks if there is an active process periodicity with the same ID that is already bound to the billing run.
     * 2. Checks if there are any active incompatible processes associated with the process periodicity and the billing run.
     * If either of these conditions is true, an error message is added to the provided errorMassages list.
     *
     * @param updatedBillingRun  the billing run to check the process periodicity against
     * @param processPeriodicity the process periodicity to check
     * @param errorMassages      a list to store any error messages
     * @param i                  the index of the process periodicity in the request
     */
    private void checkProcessPeriodicityBoundToTheBillingRun(BillingRun updatedBillingRun,
                                                             ProcessPeriodicity processPeriodicity,
                                                             List<String> errorMassages, int i) {
        List<ProcessPeriodicity> processPeriodicityOptional =
                processPeriodicityRepository.findByStartAfterProcessBillingIdAndIdAndStatus(
                        updatedBillingRun.getId(),
                        processPeriodicity.getId(),
                        EntityStatus.ACTIVE
                );
        if (!CollectionUtils.isEmpty(processPeriodicityOptional)) {
            errorMassages.add("processPeriodicityIds[%s]- Can't bound process periodicity id:%s to the billing run;"
                    .formatted(i, processPeriodicity.getId()));
        } else {
            List<ProcessPeriodicityIncompatibleProcesses> incompatibleProcesses =
                    processPeriodicityIncompatibleProcessesRepository.findByProcessPeriodicityIdAndIncompatibleBillingIdAndStatus(
                            processPeriodicity.getId(),
                            updatedBillingRun.getId(),
                            EntityStatus.ACTIVE
                    );
            if (!CollectionUtils.isEmpty(incompatibleProcesses)) {
                errorMassages.add("processPeriodicityIds[%s]- Can't bound process periodicity id:%s to the billing run;"
                        .formatted(i, processPeriodicity.getId()));
            }
        }
    }

    /**
     * Creates a new BillingProcessPeriodicity entity and saves it to the repository.
     *
     * @param updatedBillingRun  the BillingRun instance to associate the new BillingProcessPeriodicity with
     * @param processPeriodicity the ProcessPeriodicity instance to use for the new BillingProcessPeriodicity
     */
    private void createBillingRunProcessPeriodicity(BillingRun updatedBillingRun, ProcessPeriodicity processPeriodicity) {
        BillingProcessPeriodicity billingProcessPeriodicity = new BillingProcessPeriodicity();
        billingProcessPeriodicity.setBillingId(updatedBillingRun.getId());
        billingProcessPeriodicity.setProcessPeriodicityId(processPeriodicity.getId());
        billingProcessPeriodicity.setStatus(EntityStatus.ACTIVE);
        billingProcessPeriodicityRepository.save(billingProcessPeriodicity);
    }

    /**
     * Removes all active BillingProcessPeriodicity entities that are not associated with the provided process periodicity IDs.
     *
     * @param processPeriodicityIds the list of process periodicity IDs to keep associated with the billing run
     */
    private void removeAllProcessPeriodicitiesOtherThanRequestIds(List<Long> processPeriodicityIds) {
        List<BillingProcessPeriodicity> billingProcessPeriodicityList =
                billingProcessPeriodicityRepository.findByProcessPeriodicityIdNotInAndStatusIn(
                        processPeriodicityIds,
                        List.of(EntityStatus.ACTIVE));
        List<BillingProcessPeriodicity> billingProcessPeriodicitiesToDelete = new ArrayList<>();
        if (!CollectionUtils.isEmpty(billingProcessPeriodicityList)) {
            for (BillingProcessPeriodicity item : billingProcessPeriodicityList) {
                item.setStatus(EntityStatus.DELETED);
                billingProcessPeriodicitiesToDelete.add(item);
            }
            billingProcessPeriodicityRepository.saveAll(billingProcessPeriodicitiesToDelete);
        }
    }

    /**
     * Adds or updates tasks associated with a billing run.
     * <p>
     * This method first removes all tasks that are not in the provided list of task IDs. Then, for each task ID in the list, it checks if the task exists and is active. If the task is found, it checks if a billing run task already exists for the billing run and task. If it does, it updates the modification date of the existing billing run task. If it doesn't, it creates a new billing run task.
     * <p>
     * If the list of task IDs is empty, it removes all active billing run tasks associated with the billing run.
     *
     * @param updatedBillingRun the updated billing run instance
     * @param taskIds           the list of task IDs to associate with the billing run
     * @param errorMassages     a list to store any error messages encountered during the process
     */
    private void addEditTaskToBillingRun(BillingRun updatedBillingRun, List<Long> taskIds, List<String> errorMassages) {
        if (!CollectionUtils.isEmpty(taskIds)) {
            removeAllTasksOtherThanRequestTaskIds(taskIds);
            for (int i = 0; i < taskIds.size(); i++) {
                Long id = taskIds.get(i);
                Optional<Task> taskOptional = taskRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE));
                if (taskOptional.isPresent()) {
                    Task task = taskOptional.get();
                    Optional<BillingRunTasks> billingRunTasksOptional = billingRunTasksRepository.findByBillingIdAndTaskIdAndStatusIn(
                            updatedBillingRun.getId(),
                            task.getId(),
                            List.of(EntityStatus.ACTIVE));
                    if (billingRunTasksOptional.isPresent()) {
                        BillingRunTasks billingRunTasks = billingRunTasksOptional.get();
                        billingRunTasks.setModifyDate(LocalDateTime.now());
                    } else {
                        createBillingRunTask(updatedBillingRun, task);
                    }
                } else errorMassages.add("taskId[%s]-Can't find active Task with id:%s;".formatted(i, taskIds));
            }
        } else {
            List<BillingRunTasks> billingRunTasksList =
                    billingRunTasksRepository.findByBillingIdAndStatusIn(
                            updatedBillingRun.getId(),
                            List.of(EntityStatus.ACTIVE));
            List<BillingRunTasks> billingRunTasksToDelete = new ArrayList<>();
            if (!CollectionUtils.isEmpty(billingRunTasksList)) {
                for (BillingRunTasks item : billingRunTasksList) {
                    item.setStatus(EntityStatus.DELETED);
                    billingRunTasksToDelete.add(item);
                }
                if (!CollectionUtils.isEmpty(billingRunTasksToDelete)) {
                    billingRunTasksRepository.saveAll(billingRunTasksToDelete);
                }
            }
        }
    }

    /**
     * Removes all billing run tasks associated with the billing run that are not in the provided list of task IDs.
     * <p>
     * This method first retrieves all active billing run tasks that are not in the provided list of task IDs. It then sets the status of these tasks to "DELETED" and saves them to the database.
     *
     * @param taskIds the list of task IDs to keep associated with the billing run
     */
    private void removeAllTasksOtherThanRequestTaskIds(List<Long> taskIds) {
        List<BillingRunTasks> billingRunTasksList =
                billingRunTasksRepository.findByTaskIdNotInAndStatusIn(
                        taskIds,
                        List.of(EntityStatus.ACTIVE));
        List<BillingRunTasks> billingRunTasksListToDelete = new ArrayList<>();
        if (!CollectionUtils.isEmpty(billingRunTasksList)) {
            for (BillingRunTasks item : billingRunTasksList) {
                item.setStatus(EntityStatus.DELETED);
                billingRunTasksListToDelete.add(item);
            }
            billingRunTasksRepository.saveAll(billingRunTasksListToDelete);
        }
    }

    /**
     * Creates a new billing run task associated with the provided billing run.
     * <p>
     * This method creates a new {@link BillingRunTasks} entity and saves it to the database. The new task is associated with the provided {@link BillingRun} by setting the {@code billingId} field to the ID of the billing run. The {@code taskId} field is set to the ID of the provided {@link Task} object, and the {@code status} field is set to {@link EntityStatus#ACTIVE}.
     *
     * @param updatedBillingRun the billing run to associate the new task with
     * @param task              the task to create the new billing run task for
     */
    private void createBillingRunTask(BillingRun updatedBillingRun, Task task) {
        BillingRunTasks billingRunTasks = new BillingRunTasks();
        billingRunTasks.setBillingId(updatedBillingRun.getId());
        billingRunTasks.setTaskId(task.getId());
        billingRunTasks.setStatus(EntityStatus.ACTIVE);
        billingRunTasksRepository.save(billingRunTasks);
    }

    /**
     * Updates the basic parameters of a billing run for editing.
     * <p>
     * This method updates the following fields of the provided {@link BillingRun} object based on the values in the provided {@link StandardBillingParameters} object:
     * - {@code applicationModelType}
     * - {@code maxEndDate}
     * - {@code periodicMaxEndDate}
     * - {@code periodicMaxEndDateValue}
     * - {@code billingCriteria}
     * - {@code applicationLevel}
     * - {@code customerContractOrPodConditions}
     * - {@code customerContractOrPodList}
     * <p>
     * The method also saves the updated {@link BillingRun} object to the repository.
     *
     * @param billingRun      the billing run to update
     * @param basicParameters the new basic parameters to apply to the billing run
     * @param errorMassages   a list to store any error messages that occur during the update
     */
    private void updateBasicParametersForEdit(BillingRun billingRun, StandardBillingParameters basicParameters, List<String> errorMassages) {
        billingRun.setApplicationModelType(basicParameters.getApplicationModelType());
        billingRun.setMaxEndDate(basicParameters.getMaxEndDate());
        billingRun.setPeriodicMaxEndDate(basicParameters.getPeriodicMaxEndDate());
        billingRun.setPeriodicMaxEndDateValue(basicParameters.getPeriodicMaxEndDateValue());
        billingRun.setBillingCriteria(basicParameters.getBillingCriteria());
        billingRun.setApplicationLevel(basicParameters.getBillingApplicationLevel());
        billingRun.setCustomerContractOrPodConditions(basicParameters.getCustomersContractOrPODConditions());
        billingRun.setCustomerContractOrPodList(checkCustomerContractOrPod(basicParameters.getBillingApplicationLevel(), basicParameters.getListOfCustomersContractsOrPOD(), errorMassages, basicParameters.getBillingCriteria()));
        billingRunRepository.save(billingRun);
    }

    /**
     * Checks the validity of a billing run edit request.
     * <p>
     * This method checks the validity of a {@link BillingRunEditRequest} based on the current status of the {@link BillingRun} and the requested changes. If the billing run status is {@link BillingStatus#INITIAL}, the method checks that the request only contains changes to the basic parameters. If the requested periodicity does not match the billing run's periodicity, the method adds an error message to the provided {@code errorMassages} list.
     *
     * @param billingRun    the billing run being edited
     * @param request       the edit request to validate
     * @param errorMassages a list to store any error messages that occur during validation
     */
    private void checkEditRequestValidity(BillingRun billingRun, BillingRunEditRequest request, List<String> errorMassages) {
        BillingStatus status = billingRun.getStatus();
        //TODO if status is INITIAL request can't have anything other than basic Parameters - check that other tabs and parameters are null and empty
       /* if (status.equals(BillingStatus.INITIAL)) {
            errorMassages.add("When status is INITIAL , you can only edit BasicParametersTab fields;");
        }*/
        if (!request.getBillingType().equals(INVOICE_REVERSAL) && !request.getBillingType().equals(BillingType.INVOICE_CORRECTION) && !billingRun.getRunPeriodicity().equals(request.getCommonParameters().getPeriodicity())) {
            errorMassages.add("basicParameters.periodicity-[periodicity] you can't change periodicity field;");
        }
    }

    /**
     * Checks the billing run status and the user's permission to perform the requested action.
     * <p>
     * This method checks the status of the provided {@link BillingRun} object and the user's permission to perform the requested action based on the provided {@link BillingType}.
     * <p>
     * If the billing run is in the {@link BillingStatus#DELETED} state, a {@link ClientException} is thrown with an error code of {@link ErrorCode#ACCESS_DENIED}.
     * <p>
     * If the billing run is not in the {@link BillingStatus#INITIAL} state, the method checks the user's permission to perform the action:
     * - If the requested {@link BillingType} is {@link BillingType#STANDARD_BILLING}, the {@link #checkEditWIthStatusPermission()} method is called to check the user's permission.
     * - If the requested {@link BillingType} is not {@link BillingType#STANDARD_BILLING}, a {@link ClientException} is thrown with an error code of {@link ErrorCode#ACCESS_DENIED}.
     *
     * @param billingRun  the billing run to check
     * @param billingType the type of billing operation being performed
     * @throws ClientException if the billing run is in the {@link BillingStatus#DELETED} state or the user does not have permission to perform the requested action
     */
    private void checkBillingRunStatusAndPermission(BillingRun billingRun, BillingType billingType) {
        BillingStatus status = billingRun.getStatus();
        if (status.equals(BillingStatus.DELETED)) {
            throw new ClientException("billingRun with id: %s is DELETED;".formatted(billingRun.getId()), ErrorCode.ACCESS_DENIED);
        }
        if (!billingRun.getStatus().equals(BillingStatus.INITIAL)) {
            if (billingType.equals(STANDARD_BILLING)) {
                checkEditWIthStatusPermission();
            } else {
                throw new ClientException("You can not perform this action;", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    /**
     * Checks the user's permission to edit a billing run with a non-initial status.
     * <p>
     * This private method checks if the current user has the "EDIT_BILLING_RUN_WITH_STATUS" permission. If the user has the permission, the method returns without throwing an exception. If the user does not have the permission, a {@link ClientException} is thrown with an error code of {@link ErrorCode#ACCESS_DENIED}.
     */
    private void checkEditWIthStatusPermission() {
        List<String> context = permissionService.getPermissionsFromContext(BILLING_RUN);
        if (context.contains(EDIT_BILLING_RUN_WITH_STATUS.getId())) {
            return;
        }
        throw new ClientException("You can not perform this action;", ErrorCode.ACCESS_DENIED);
    }

    /**
     * Validates the provided billing run condition and returns a response indicating whether the validation was successful.
     * <p>
     * This method takes a {@link BillingRunConditionValidationRequest} object that contains the condition to be validated. It then calls the {@link BillingRunConditionService#validateBillingRunCondition(String, List, String)} method to perform the validation.
     * <p>
     * If the validation is successful, a {@link BillingRunConditionValidateResponse} object is returned with the `success` field set to `true`. If there are any error messages generated during the validation, they are added to the `errorMessages` list in the response.
     *
     * @param request the {@link BillingRunConditionValidationRequest} object containing the condition to be validated
     * @return a {@link BillingRunConditionValidateResponse} object indicating the result of the validation
     */
    public BillingRunConditionValidateResponse validateBillingRunCondition(BillingRunConditionValidationRequest request) {
        List<String> errorMessages = new ArrayList<>();
        billingRunConditionService.validateBillingRunCondition(request.getCondition(), errorMessages, "Illegal logical operator with variable");
        return new BillingRunConditionValidateResponse(true);
    }

    /**
     * Validates the provided billing criteria condition and adds any error messages to the provided list.
     * <p>
     * This private method checks if the provided {@link BillingCriteria} is {@link BillingCriteria#CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS}. If so, it calls the {@link BillingRunConditionService#validateBillingRunCondition(String, List, String)} method to validate the provided `customersContractOrPODConditions` string. Any error messages generated during the validation are added to the `errorMessages` list.
     *
     * @param billingCriteria                  the {@link BillingCriteria} to check
     * @param customersContractOrPODConditions the condition to be validated
     * @param errorMessages                    the list to add any error messages to
     */
    private void checkCondition(BillingCriteria billingCriteria, String customersContractOrPODConditions, List<String> errorMessages) {
        if (billingCriteria.equals(CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS)) {
            billingRunConditionService.validateBillingRunCondition(customersContractOrPODConditions, errorMessages, "basicParameters.customersContractOrPODConditions-customersContractOrPODConditions Illegal logical operator with variable");
        }
    }

    /**
     * Retrieves a list of customer communication data based on the provided request.
     * <p>
     * This method delegates the retrieval of the customer communication data list to the `manualInvoiceService`.
     *
     * @param request the {@link BillingCommunicationDataListRequest} containing the parameters to filter the communication data
     * @return a list of {@link CustomerCommunicationDataResponse} representing the customer communication data
     */
    public List<CustomerCommunicationDataResponse> billingCommunicationDataList(BillingCommunicationDataListRequest request) {
        return manualInvoiceService.getCommunicationDataList(request);
    }

    /**
     * Imports a file and returns a response containing the result of the import operation.
     * <p>
     * This method takes a {@link MultipartFile} and a {@link ManualInvoiceType} as input parameters. It then delegates the file import operation to the {@link ManualInvoiceService#importFile(MultipartFile, ManualInvoiceType)} method, and returns the resulting {@link ManualInvoiceImportResponse}.
     *
     * @param file the {@link MultipartFile} to be imported
     * @param type the {@link ManualInvoiceType} of the file to be imported
     * @return a {@link ManualInvoiceImportResponse} containing the result of the import operation
     */
    public ManualInvoiceImportResponse importFile(MultipartFile file, ManualInvoiceType type) {
        return manualInvoiceService.importFile(file, type);
    }

    /**
     * Retrieves the manual invoice template content for the specified invoice type.
     * <p>
     * This method delegates the retrieval of the manual invoice template to the `manualInvoiceService`.
     *
     * @param type the {@link ManualInvoiceType} of the template to be downloaded
     * @return a {@link ManualInvoiceTemplateContent} object containing the template content
     */
    public ManualInvoiceTemplateContent downloadManualInvoiceTemplate(ManualInvoiceType type) {
        return manualInvoiceService.downloadTemplate(type);
    }

    /**
     * Retrieves a list of customer contracts and orders based on the provided request.
     * <p>
     * This method delegates the retrieval of the customer contract and order list to the `manualInvoiceService`.
     *
     * @param request the {@link CustomerContractsAndOrdersRequest} containing the parameters to filter the contract and order data
     * @return a {@link Page} of {@link CustomerContractOrderResponse} representing the customer contract and order data
     */
    public Page<CustomerContractOrderResponse> contractOrderList(CustomerContractsAndOrdersRequest request) {
        return manualInvoiceService.getCustomerContractOrderList(request);
    }

    /**
     * Retrieves a paginated list of periodic billing runs based on the provided request.
     * <p>
     * This method delegates the retrieval of the periodic billing run list to the `billingRunRepository`. It applies various filters and sorting options based on the fields in the `BillingRunListingPeriodicRequest` object.
     *
     * @param request the {@link BillingRunListingPeriodicRequest} containing the parameters to filter and sort the periodic billing runs
     * @return a {@link Page} of {@link BillingRunListingPeriodicResponse} representing the periodic billing runs
     */
    public Page<BillingRunListingPeriodicResponse> listPeriodic(BillingRunListingPeriodicRequest request) {
        return billingRunRepository.filterPeriodic(
                        getSearchByEnum(request.getSearchFields()),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBListUtils.convertEnumListToDBEnumArray(request.getAutomations()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getBillingCriteria()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getApplicationLevels()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getBillingTypes()),
                        ListUtils.emptyIfNull(request.getPeriodicity()),
                        checkDirectionForProcessPeriodicity(request),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), getSorByEnum(request.getColumns()))
                                )
                        )
                )
                .map(BillingRunListingPeriodicResponse::new);
    }

    /**
     * Retrieves the search field value based on the provided {@link BillingRunSearchFields} enum.
     * If the enum value is not null, it returns the corresponding value. Otherwise, it returns the value of {@link BillingRunSearchFields#ALL}.
     *
     * @param billingRunSearchField the {@link BillingRunSearchFields} enum value to get the corresponding search field value
     * @return the search field value
     */
    private String getSearchByEnum(BillingRunSearchFields billingRunSearchField) {
        return billingRunSearchField != null ? billingRunSearchField.getValue() : BillingRunSearchFields.ALL.getValue();
    }

    /**
     * Retrieves the sort field value based on the provided {@link BillingRunListColumns} enum.
     * If the enum value is not null, it returns the corresponding value. Otherwise, it returns the value of {@link BillingRunListColumns#NUMBER}.
     *
     * @param sortByColumn the {@link BillingRunListColumns} enum value to get the corresponding sort field value
     * @return the sort field value
     */
    private String getSorByEnum(BillingRunListColumns sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : BillingRunListColumns.NUMBER.getValue();
    }

    /**
     * Checks the direction for the process periodicity based on the provided {@link BillingRunListColumns} enum.
     * If the columns enum is equal to {@link BillingRunListColumns#PROCESS_PERIODICITY}, it returns the direction from the request.
     * Otherwise, it returns null.
     *
     * @param request the {@link BillingRunListingPeriodicRequest} containing the columns and direction information
     * @return the direction for the process periodicity, or null if the columns enum is not {@link BillingRunListColumns#PROCESS_PERIODICITY}
     */
    private String checkDirectionForProcessPeriodicity(BillingRunListingPeriodicRequest request) {
        BillingRunListColumns columns = request.getColumns();
        String direction = null;
        if (columns != null) {
            if (columns.equals(BillingRunListColumns.PROCESS_PERIODICITY)) {
                if (request.getDirection() != null) {
                    direction = request.getDirection().name();
                }
            }
        }
        return direction;
    }

    /**
     * Filters the billing runs by the provided billing number.
     *
     * @param request the {@link BillingRunFilterByRequest} containing the billing number prompt and pagination information
     * @return a {@link Page} of {@link BillingRunFilterByResponse} objects matching the filter criteria
     */
    public Page<BillingRunFilterByResponse> filterByBillingNumber(BillingRunFilterByRequest request) {
        return billingRunRepository
                .filterByBillingNumber(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
    }

    /**
     * Uploads a billing invoices file and saves it to the file system.
     *
     * @param file the {@link MultipartFile} containing the billing invoices file to be uploaded
     * @return a {@link BillingFileResponse} containing the details of the uploaded file
     * @throws IllegalArgumentsProvidedException if the file name is null
     * @throws DomainEntityNotFoundException     if the template for Invoice Correction is not found
     */
    public BillingFileResponse upload(MultipartFile file) {
        log.debug("Uploading billing invoices file {}.", file.getName());

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Billing invoices file name is null.");
            throw new IllegalArgumentsProvidedException("Billing invoices file name is null.");
        }

        EPBExcelUtils.validateFileFormat(file);
        EPBExcelUtils.validateExcelIsEmpty(file);

        Template template = templateRepository
                .findById("INVOICE_CORRECTION")
                .orElseThrow(() -> new DomainEntityNotFoundException("Template for Invoice Correction not found;"));

        EPBExcelUtils.validateFileContent(file, fileService.downloadFile(template.getFileUrl()).getByteArray(), 1);

        String fileName = String.format("%s_%s", UUID.randomUUID(), originalFilename.replaceAll("\\s+", ""));
        String path = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        String url = fileService.uploadFile(file, path, fileName);

        BillingInvoicesFile billingInvoicesFile = new BillingInvoicesFile();
        billingInvoicesFile.setBillingId(null);
        billingInvoicesFile.setName(fileName);
        billingInvoicesFile.setFileUrl(url);
        billingInvoicesFile.setStatus(EntityStatus.ACTIVE);
        billingInvoicesFileRepository.save(billingInvoicesFile);

        return new BillingFileResponse(billingInvoicesFile);
    }

    /**
     * Retrieves the template file for the invoice correction process.
     *
     * @return the byte array of the template file
     * @throws DomainEntityNotFoundException if the template file is not found
     * @throws ClientException               if there is an error fetching the template file
     */
    public byte[] getTemplate() {
        try {
            var templatePath = templateRepository.findById("INVOICE_CORRECTION").orElseThrow(() -> new DomainEntityNotFoundException("Unable to find template path"));
            log.info("template path ->>>> :" + templatePath.getFileUrl());
            return fileService.downloadFile(templatePath.getFileUrl()).getByteArray();
        } catch (Exception exception) {
            log.error("Could not fetch invoice correction template", exception);
            throw new ClientException("Could not fetch invoice correction template", APPLICATION_ERROR);
        }
    }

    /**
     * Retrieves the invoice correction file for the specified billing ID.
     *
     * @param billingId the ID of the billing for which to retrieve the invoice correction file
     * @return a {@link KeyValue} containing the file name and the byte array of the invoice correction file
     * @throws DomainEntityNotFoundException if the invoice correction file is not found
     * @throws ClientException               if there is an error fetching the invoice correction file
     */
    public KeyValue<String, byte[]> getInvoiceCorrectionFile(Long billingId) {
        try {
            var templatePath = billingInvoicesFileRepository.findByBillingIdAndStatus(billingId, EntityStatus.ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("Unable to find template path"));
            log.info("template path ->>>> :" + templatePath.getFileUrl());
            String fileName = templatePath.getFileUrl().split("_")[3];
            return KeyValue.just(fileName, fileService.downloadFile(templatePath.getFileUrl()).getByteArray());
        } catch (Exception exception) {
            log.error("Could not fetch invoice correction file", exception);
            throw new ClientException("Could not fetch invoice correction file", APPLICATION_ERROR);
        }
    }

    /**
     * Checks the tax event date for the billing run parameters.
     *
     * @param parameters    the billing run parameters
     * @param errorMessages the list to add error messages to
     */
    private void checkTaxEventDate(BillingRunCommonParameters parameters, List<String> errorMessages) {
        if (parameters.getPeriodicity() == null || parameters.getPeriodicity().equals(BillingRunPeriodicity.STANDARD)) {
            Long accountingPeriodId = parameters.getAccountingPeriodId();
            if (accountingPeriodId == null) {
                errorMessages.add("accountingPeriodId-accountingPeriodId can't be null;");
            } else {
                AccountingPeriods accountingPeriods = getAccountingPeriods(accountingPeriodId);
                if (accountingPeriods == null) {
                    errorMessages.add("accountingPeriods-accountingPeriods with Id: %s can't be found;".formatted(accountingPeriodId));
                } else {
                    LocalDate taxEventDate = parameters.getTaxEventDate();
                    if (taxEventDate == null) {
                        errorMessages.add("taxEventDate-taxEventDate can't be null;");
                    } else if (taxEventDate.isBefore(LocalDate.now()) && (taxEventDate.isBefore(ChronoLocalDate.from(accountingPeriods.getStartDate())) || taxEventDate.isAfter(ChronoLocalDate.from(accountingPeriods.getEndDate())))) {
                        errorMessages.add("taxEventDate-Tax Event Date should be in selected accounting period;");
                    }
                }
            }
        }
    }

    /**
     * Checks that the invoice date is not null and is within the selected accounting period.
     *
     * @param parameters    the billing run parameters
     * @param errorMessages the list to add error messages to
     */
    private void checkInvoiceDate(BillingRunCommonParameters parameters, List<String> errorMessages) {
        if (parameters.getPeriodicity() == null || parameters.getPeriodicity().equals(BillingRunPeriodicity.STANDARD)) {
            Long accountingPeriodId = parameters.getAccountingPeriodId();
            if (accountingPeriodId == null) {
                errorMessages.add("accountingPeriodId-accountingPeriodId can't be null;");
            } else {
                AccountingPeriods accountingPeriods = getAccountingPeriods(accountingPeriodId);
                if (accountingPeriods == null) {
                    errorMessages.add("accountingPeriods-accountingPeriods with Id: %s can't be found;".formatted(accountingPeriodId));
                } else {
                    LocalDate invoiceDate = parameters.getInvoiceDate();
                    if (invoiceDate == null) {
                        errorMessages.add("invoiceDate-Invoice date can't be null;");
                    }
                }
            }
        }
    }

    private boolean isPriceVolumeChangeValid(BillingType type, InvoiceCorrectionParameters parameters) {
        if (type == BillingType.INVOICE_CORRECTION && !parameters.isVolumeChange() && !parameters.isPriceChange()) {
            return false;
        }
        return true;
    }

    /**
     * Retrieves a page of available invoices based on the provided request parameters.
     * Used for manual credit debit note billing run only.
     *
     * @param billingRunInvoiceRequest the request containing parameters to filter the invoices, such as prompt, invoice number, page, and size.
     * @return a page of {@link BillingRunInvoiceResponse} objects representing the available invoices.
     */
    public Page<BillingRunInvoiceResponse> getAvailableInvoices(BillingRunInvoiceRequest billingRunInvoiceRequest) {
        return invoiceRepository.filterInvoiceNumbers(
                EPBStringUtils.fromPromptToQueryParameter(billingRunInvoiceRequest.getPrompt()),
                billingRunInvoiceRequest.getInvoiceNumber(),
                PageRequest.of(billingRunInvoiceRequest.getPage(), billingRunInvoiceRequest.getSize())
        );

    }

    /**
     * Checks if the current user has the specified permission to perform billing run operations.
     *
     * @param permission The permission to check for.
     * @throws ClientException if the user does not have the required permission.
     */
    private void checkPermission(PermissionEnum permission) {
        if (!permissionService.getPermissionsFromContext(BILLING_RUN).contains(permission.getId()))
            throw new ClientException("Can't create billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
    }

    /**
     * Retrieves the tasks associated with the specified billing run.
     *
     * @param id The ID of the billing run.
     * @return A list of {@link TaskShortResponse} objects representing the tasks for the specified billing run.
     */
    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByBillingRunId(id);
    }

    /**
     * retrieves draft invoices generated on billing run
     *
     * @param id      billing run id.
     * @param request page, size and prompt to filter invoices.
     * @return List of paginated @return {@link BillingRunInvoiceViewResponse} objects
     */
    public Page<BillingRunInvoiceViewResponse> listDraftInvoices(Long id, BillingRunInvoiceListingRequest request) {
        return billingRunRepository.getInvoicesForBillingRun(true, id,
                EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                PageRequest.of(request.page(), request.size()));
    }

    /**
     * retrieves pdf documents generated for the draft invoices of billing run
     *
     * @param id      billing run id.
     * @param request page, size and prompt to filter invoices.
     * @return List of paginated @return {@link BillingRunInvoiceViewResponse} objects
     */
    public Page<BillingRunInvoiceViewResponse> listPdfDocuments(Long id, BillingRunInvoiceListingRequest request) {
        return billingRunRepository.getInvoicesForBillingRun(false, id,
                EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                PageRequest.of(request.page(), request.size()));
    }

    /**
     * Marks specified invoices as removed in the billing run.
     * If markAll is true, all non-removed invoices will be marked as removed, except for those specified in excludedInvoiceIds.
     * If markAll is false, only the invoices specified in invoiceIds will be marked as removed.
     * It is required for the billing run to be in DRAFT status for invoice removal to be allowed.
     *
     * @param billingRunId       the ID of the billing run
     * @param markAll            a boolean indicating whether to mark all non-removed invoices as removed
     * @param invoiceIds         a list of invoice IDs to mark as removed (ignored if markAll is true)
     * @param excludedInvoiceIds a list of invoice IDs to exclude from being marked as removed (only relevant if markAll is true)
     * @throws DomainEntityNotFoundException     if the billing run with the specified ID does not exist
     * @throws IllegalArgumentsProvidedException if the billing run is not in DRAFT status, or if invalid invoice IDs are provided
     */
    @Transactional
    public void markAsRemovedDraft(Long billingRunId,
                                   Boolean markAll,
                                   List<Long> invoiceIds,
                                   List<Long> excludedInvoiceIds) {
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));

        if (!Objects.equals(billingRun.getStatus(), BillingStatus.DRAFT)) {
            throw new IllegalArgumentsProvidedException("Invoice removal is available only for DRAFT billing run");
        }

        List<Long> markedInvoicesByBillingRun = billingRunDraftInvoicesMarkRepository.findMarkedInvoicesByBillingRun(billingRunId);
        List<Long> billingRunInvoiceIds = invoiceRepository.findByBillingRunAndInvoiceStatus(billingRunId, InvoiceStatus.DRAFT).stream().map(Invoice::getId).toList();
        List<Long> nonRemovedInvoices = CollectionUtils.subtract(billingRunInvoiceIds, markedInvoicesByBillingRun).stream().toList();

        List<BillingRunDraftInvoicesMark> markedInvoicesAsRemoved = new ArrayList<>();
        if (Boolean.TRUE.equals(markAll)) {
            markedInvoicesAsRemoved.addAll(
                    nonRemovedInvoices
                            .stream()
                            .filter(invoiceId -> !excludedInvoiceIds.contains(invoiceId))
                            .map((invoiceId) -> BillingRunDraftInvoicesMark
                                    .builder()
                                    .billingRun(billingRunId)
                                    .invoice(invoiceId)
                                    .build())
                            .toList()
            );
        } else {
            if (CollectionUtils.isNotEmpty(invoiceIds)) {
                for (Long invoiceId : invoiceIds) {
                    if (!billingRunInvoiceIds.contains(invoiceId)) {
                        throw new IllegalArgumentsProvidedException("Cannot mark invoice with id: [%s] as removed, invoice not assigned to billing run");
                    }

                    if (!nonRemovedInvoices.contains(invoiceId)) {
                        throw new IllegalArgumentsProvidedException("Cannot mark invoice with id: [%s] as removed".formatted(invoiceId));
                    }

                    markedInvoicesAsRemoved.add(
                            new BillingRunDraftInvoicesMark(
                                    null,
                                    billingRunId,
                                    invoiceId
                            )
                    );
                }
            }
        }

        if (CollectionUtils.isNotEmpty(markedInvoicesAsRemoved)) {
            billingRunDraftInvoicesMarkRepository.saveAll(markedInvoicesAsRemoved);
        }
    }

    /**
     * Marks invoices as restored drafts in a billing run.
     *
     * @param billingRunId       The ID of the billing run.
     * @param markAll            Whether to mark all invoices as restored or only specified ones.
     * @param invoiceIds         The list of invoice IDs to mark as restored.
     * @param excludedInvoiceIds The list of invoice IDs to exclude from restoration.
     * @throws DomainEntityNotFoundException     If the billing run with the given ID does not exist.
     * @throws IllegalArgumentsProvidedException If the billing run is not in DRAFT status.
     */
    @Transactional
    public void markAsRestoredDraft(Long billingRunId,
                                    Boolean markAll,
                                    List<Long> invoiceIds,
                                    List<Long> excludedInvoiceIds) {
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));

        if (!Objects.equals(billingRun.getStatus(), BillingStatus.DRAFT)) {
            throw new IllegalArgumentsProvidedException("Invoice restore is available only for DRAFT billing run");
        }

        if (Boolean.TRUE.equals(markAll)) {
            billingRunDraftInvoicesMarkRepository.deleteAllByBillingRunAndInvoiceNotIn(billingRunId, excludedInvoiceIds);
        } else {
            billingRunDraftInvoicesMarkRepository.deleteAllByBillingRunAndInvoiceIn(billingRunId, invoiceIds);
        }
    }

    /**
     * Marks the specified invoices in the given billing run as removed.
     *
     * @param billingRunId       the ID of the billing run
     * @param markAll            indicates whether to mark all non-removed invoices in the billing run
     * @param invoiceIds         the IDs of the invoices to mark as removed (can be null if markAll is true)
     * @param excludedInvoiceIds the IDs of the invoices to exclude from marking as removed (can be null)
     * @throws DomainEntityNotFoundException     if the billing run with the specified ID does not exist
     * @throws IllegalArgumentsProvidedException if the billing run is not in DRAFT status,
     *                                           invoice not assigned to billing run, or
     *                                           invalid invoice ID is provided
     */
    @Transactional
    public void markAsRemovedDraftGenerated(Long billingRunId,
                                            Boolean markAll,
                                            List<Long> invoiceIds,
                                            List<Long> excludedInvoiceIds) {
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));

        if (!Objects.equals(billingRun.getStatus(), BillingStatus.GENERATED)) {
            throw new IllegalArgumentsProvidedException("Invoice removal is available only for DRAFT billing run");
        }

        List<Long> markedInvoicesByBillingRun = billingRunDraftPdfInvoicesMarkRepository.findMarkedInvoicesByBillingRun(billingRunId);
        List<Long> billingRunInvoiceIds = invoiceRepository.findByBillingRunAndInvoiceStatus(billingRunId, InvoiceStatus.DRAFT_GENERATED).stream().map(Invoice::getId).toList();
        List<Long> nonRemovedInvoices = CollectionUtils.subtract(billingRunInvoiceIds, markedInvoicesByBillingRun).stream().toList();

        List<BillingRunDraftPdfInvoicesMark> markedInvoicesAsRemoved = new ArrayList<>();
        if (Boolean.TRUE.equals(markAll)) {
            markedInvoicesAsRemoved.addAll(
                    nonRemovedInvoices
                            .stream()
                            .filter(invoiceId -> !excludedInvoiceIds.contains(invoiceId))
                            .map((invoiceId) -> BillingRunDraftPdfInvoicesMark
                                    .builder()
                                    .billingRun(billingRunId)
                                    .invoice(invoiceId)
                                    .build())
                            .toList()
            );
        } else {
            if (CollectionUtils.isNotEmpty(invoiceIds)) {
                for (Long invoiceId : invoiceIds) {
                    if (!billingRunInvoiceIds.contains(invoiceId)) {
                        throw new IllegalArgumentsProvidedException("Cannot mark invoice with id: [%s] as removed, invoice not assigned to billing run");
                    }

                    if (!nonRemovedInvoices.contains(invoiceId)) {
                        throw new IllegalArgumentsProvidedException("Cannot mark invoice with id: [%s] as removed".formatted(invoiceId));
                    }

                    markedInvoicesAsRemoved.add(
                            new BillingRunDraftPdfInvoicesMark(
                                    null,
                                    billingRunId,
                                    invoiceId
                            )
                    );
                }
            }
        }

        if (CollectionUtils.isNotEmpty(markedInvoicesAsRemoved)) {
            billingRunDraftPdfInvoicesMarkRepository.saveAll(markedInvoicesAsRemoved);
        }
    }

    /**
     * Marks the specified invoices as restored draft generated.
     *
     * @param billingRunId       the ID of the billing run
     * @param markAll            flag indicating whether to mark all invoices as restored draft generated
     * @param invoiceIds         the list of invoice IDs to mark as restored draft generated
     * @param excludedInvoiceIds the list of excluded invoice IDs when markAll is true
     * @throws DomainEntityNotFoundException     if the billing run with the specified ID does not exist
     * @throws IllegalArgumentsProvidedException if the billing run status is not GENERATED
     */
    @Transactional
    public void markAsRestoredDraftGenerated(Long billingRunId,
                                             Boolean markAll,
                                             List<Long> invoiceIds,
                                             List<Long> excludedInvoiceIds) {
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));

        if (!Objects.equals(billingRun.getStatus(), BillingStatus.GENERATED)) {
            throw new IllegalArgumentsProvidedException("Invoice restore is available only for GENERATED billing run");
        }

        if (Boolean.TRUE.equals(markAll)) {
            billingRunDraftPdfInvoicesMarkRepository.deleteAllByBillingRunAndInvoiceNotIn(billingRunId, excludedInvoiceIds);
        } else {
            billingRunDraftPdfInvoicesMarkRepository.deleteAllByBillingRunAndInvoiceIn(billingRunId, invoiceIds);
        }
    }

    /**
     * Terminate a billing run.
     * <p>
     * This method cancels a billing run with the given ID by changing its status to {@link BillingStatus#CANCELLED}.
     * It can only be executed if the current status of the billing run is one of the following:
     * {@link BillingStatus#IN_PROGRESS_DRAFT},
     * {@link BillingStatus#DRAFT},
     * {@link BillingStatus#IN_PROGRESS_GENERATION},
     * {@link BillingStatus#IN_PROGRESS_ACCOUNTING},
     * or {@link BillingStatus#PAUSED}.
     * <p>
     * If the billing run does not exist with the given ID, a {@link DomainEntityNotFoundException} will be thrown.
     * If the current status of the billing run is not one of the allowed statuses, an {@link IllegalArgumentsProvidedException}
     * will be thrown.
     *
     * @param billingRunId the ID of the billing run to terminate
     * @throws DomainEntityNotFoundException     if the billing run does not exist with the given ID
     * @throws IllegalArgumentsProvidedException if the current status of the billing run is not one of the allowed statuses
     * @see BillingStatus
     * @see DomainEntityNotFoundException
     * @see IllegalArgumentsProvidedException
     */
    @Transactional
    @ExecutionTimeLogger
    public void terminate(Long billingRunId) {
        log.debug("Terminating billing run with id: [%s]".formatted(billingRunId));
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));
        if (!checkBillingPermission(BillingPermissions.TERMINATE, billingRun.getType())) {
            log.debug("User does not have permission to terminate billing run with id: [%s]".formatted(billingRunId));
            throw new ClientException("Can't terminate billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        List<BillingStatus> availableStatusesForTermination = List.of(
                BillingStatus.DRAFT,
                BillingStatus.IN_PROGRESS_GENERATION,
                BillingStatus.IN_PROGRESS_ACCOUNTING,
                BillingStatus.PAUSED,
                BillingStatus.GENERATED
        );

        log.debug("Billing run status: [%s]".formatted(billingRun.getStatus()));
        if (!availableStatusesForTermination.contains(billingRun.getStatus())) {
            log.debug("Billing run with id: [%s] is not in one of the following statuses: [%s]".formatted(billingRunId, availableStatusesForTermination));
            throw new IllegalArgumentsProvidedException("Termination available only for followed statuses: [%s]".formatted(availableStatusesForTermination));
        }

        List<Document> billingRunInvoiceDocuments = documentRepository
                .findBillingRunInvoiceDocuments(billingRun.getId());
        log.debug("Billing run invoice documents size: [%s]".formatted(billingRunInvoiceDocuments.size()));

        billingRunOutdatedDocumentService.deleteOutdatedDocuments(billingRunInvoiceDocuments);

        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork((work) -> {
                Long runId = billingRun.getId();
                MDC.put("billingId", String.valueOf(runId));
                log.debug("Starting data cleanup for billing run with id: [%s]".formatted(runId));
                CallableStatement statement = work.prepareCall("CALL billing_run.terminate_billing_run(?)");
                statement.setLong(1, runId);
                log.debug("Procedure call was successful;");
                statement.execute();
            });
        } catch (Exception e) {
            log.error("Exception handled while cleanup billing run data", e);
        }
        billingRun.setStatus(BillingStatus.CANCELLED);
        log.debug("Billing run terminated");

        billingRunRepository.save(billingRun);
    }

    /**
     * Pauses a billing run with the given ID.
     *
     * @param billingRunId the ID of the billing run to pause
     * @throws DomainEntityNotFoundException if the billing run does not exist with the given ID
     */
    @Transactional
    public void pause(Long billingRunId, boolean mustCheckPermission) {
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));
        if (mustCheckPermission && !checkBillingPermission(BillingPermissions.PAUSE, billingRun.getType())) {
            throw new ClientException("Can't pause billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        switch (billingRun.getStatus()) {
            case IN_PROGRESS_DRAFT,
                 IN_PROGRESS_GENERATION,
                 IN_PROGRESS_ACCOUNTING,
                 DRAFT,
                 GENERATED -> {
                billingRun.setStatus(BillingStatus.PAUSED);
                billingRunRepository.save(billingRun);
            }
            default ->
                    throw new IllegalArgumentsProvidedException("You can pause only while billing run is in progress");
        }
    }

    /**
     * Resumes a billing run with the specified ID.
     *
     * @param billingRunId the ID of the billing run to resume
     * @throws DomainEntityNotFoundException if the billing run does not exist with the specified ID
     */
    @Transactional
    public void resume(Long billingRunId, boolean mustCheckPermission) {
        BillingRun billingRun = billingRunRepository
                .findById(billingRunId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing run does not exists with id: [%s]".formatted(billingRunId)));
        if (mustCheckPermission && !checkBillingPermission(BillingPermissions.RESUME, billingRun.getType())) {
            throw new ClientException("Can't resume billing run without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
        }
        BillingRunProcessStage processStage = billingRun.getProcessStage();
        if (Objects.isNull(processStage)) {
            throw new IllegalArgumentsProvidedException("Cannot resume process, unknown process stage");
        }

        switch (processStage) {
            case DRAFT -> {
                billingRun.setStatus(BillingStatus.IN_PROGRESS_DRAFT);
                billingRunProcessHelper.updateBillingRunImmediately(billingRun);
                switch (billingRun.getType()) {
                    case STANDARD_BILLING -> {
                        billingRun.setMainInvoiceGenerationStatus(null);
                        billingRunProcessHelper.updateBillingRunImmediately(billingRun);
                    }
                    case INVOICE_REVERSAL -> invoiceReversalProcessService.startProcessing(billingRun);
                }
                billingRunRepository.save(billingRun);
            }
            case DRAFT_DOCUMENT -> {
                billingRun.setStatus(BillingStatus.IN_PROGRESS_GENERATION);
                billingRunRepository.save(billingRun);
            }
            case ACCOUNTING -> {
                billingRun.setStatus(BillingStatus.IN_PROGRESS_ACCOUNTING);
                billingRunRepository.save(billingRun);
            }
        }
    }

    /**
     * Creates one time billing runs based on periodic.
     * this method should be called from job that runs at 00:00 every day.
     *
     * @return
     */
    //Todo return void and make it as job!;
    public List<Pair<Long, Long>> createOneTimesForToday() {
        List<OneTimeCreationModel> periodicRuns = billingRunRepository.findAllPeriodicForOneTimeCreation();
        List<Pair<BillingRun, Long>> billingRunsToSave = new ArrayList<>();
        Set<Long> billingsCreated = new HashSet<>();
        for (OneTimeCreationModel periodicRun : periodicRuns) {
            if (!billingsCreated.contains(periodicRun.getId())) {
                billingRunsToSave.add(Pair.of(billingRunRepository.saveAndFlush(cloneBillingRun(periodicRun)), periodicRun.getId()));
                billingsCreated.add(periodicRun.getId());
            }
        }

        List<BillingProcessPeriodicity> newPeriodicity = new ArrayList<>();
        List<BillingRunTasks> newTasks = new ArrayList<>();
        List<BillingRunBillingGroup> newBillingGroups = new ArrayList<>();
        for (Pair<BillingRun, Long> pair : billingRunsToSave) {
            BillingRun cloned = pair.getLeft();
            Long oldRun = pair.getRight();
            //Todo add templates
//            List<BillingProcessPeriodicity> oldPeriodicity = billingProcessPeriodicityRepository.findByBillingIdAndStatus(oldRun, EntityStatus.ACTIVE);
//            newPeriodicity.addAll(oldPeriodicity.stream().map(x -> new BillingProcessPeriodicity(x, cloned.getId())).toList());

            List<BillingRunTasks> tasks = billingRunTasksRepository.findByBillingIdAndStatusIn(oldRun, List.of(EntityStatus.ACTIVE));
            newTasks.addAll(tasks.stream().map(x -> new BillingRunTasks(cloned.getId(), x)).toList());

            List<BillingRunBillingGroup> billingGroups = billingRunBillingGroupRepository.findByBillingRunId(oldRun, EntityStatus.ACTIVE);
            newBillingGroups.addAll(billingGroups.stream().map(x -> new BillingRunBillingGroup(x, cloned.getId())).toList());
        }
        billingProcessPeriodicityRepository.saveAll(newPeriodicity);
        billingRunTasksRepository.saveAll(newTasks);
        billingRunBillingGroupRepository.saveAll(newBillingGroups);

        //Todo this should be removed after testing
        return billingRunsToSave.stream().map(x -> Pair.of(x.getLeft().getId(), x.getRight())).toList();
    }

    /**
     * Clones a BillingRun object from an OneTimeCreationModel.
     *
     * @param billingRun the OneTimeCreationModel to clone from
     * @return a new BillingRun object with the cloned data
     */
    private BillingRun cloneBillingRun(OneTimeCreationModel billingRun) {
        BillingRun newRun = new BillingRun();
        newRun.setBillingNumber(generateBillingNumber());
        newRun.setRunPeriodicity(BillingRunPeriodicity.STANDARD);
        newRun.setAdditionalInfo(billingRun.getAdditionalInfo());
        newRun.setType(billingRun.getType());
        newRun.setStatus(BillingStatus.INITIAL);
        newRun.setEmployeeId(billingRun.getEmployeeId());
        newRun.setMaxEndDate(generatePeriodicEndDate(billingRun.getMaxEndDate(), billingRun.getMaxEndDateValue()));
        //Todo check with tiko
        newRun.setTaxEventDate(LocalDate.now());
        newRun.setInvoiceDate(LocalDate.now());
        Optional<Long> currentMonthsAccountingPeriodId = accountingPeriodsRepository.findCurrentMonthsAccountingPeriodId();
        //TOdo check this maybe I have to throw exception
        currentMonthsAccountingPeriodId.ifPresent(newRun::setAccountingPeriodId);
        newRun.setInvoiceDueDateType(InvoiceDueDateType.ACCORDING_TO_THE_CONTRACT);
        newRun.setInvoiceDueDate(LocalDate.now());
        newRun.setApplicationModelType(extractEnums(billingRun.getApplicationModelType(), ApplicationModelType.class));
        newRun.setSendingAnInvoice(billingRun.getSendingAnInvoice());
        ProcessPeriodicityBillingProcessStart processExecutionType = billingRun.getProcessExecutionType();
        if (DATE_AND_TIME.equals(processExecutionType)) {
            newRun.setExecutionType(ExecutionType.EXACT_DATE);
            newRun.setExecutionDate(billingRun.getProcessExecutionDate());
        } else if (billingRun.getRunPeriodicity().equals(BillingRunPeriodicity.PERIODIC)) {
            newRun.setExecutionType(ExecutionType.EXACT_DATE);
            newRun.setExecutionDate(LocalDateTime.of(LocalDate.now(), billingRun.getStartTime().toLocalTime()));
        } else {
            newRun.setExecutionType(ExecutionType.EXACT_DATE);
            newRun.setExecutionDate(LocalDateTime.now().plusMinutes(30));
        }
        log.debug("StartTimeForBillingRun {}, {}", billingRun.getId(), billingRun.getStartTime().toLocalTime().toString());
        newRun.setBillingCriteria(billingRun.getBillingCriteria());
        newRun.setApplicationLevel(billingRun.getApplicationLevel());
        newRun.setCustomerContractOrPodConditions(billingRun.getCustomerContractOrPodConditions());
        newRun.setCustomerContractOrPodList(billingRun.getCustomerContractOrPodList());
        newRun.setRunStages(extractEnums(billingRun.getRunStages(), RunStage.class));
        newRun.setVatRateId(billingRun.getVatRateId());
        newRun.setGlobalVatRate(billingRun.getGlobalVatRate());
        newRun.setInterestRateId(billingRun.getInterestRateId());
        newRun.setBankId(billingRun.getBankId());
        newRun.setIban(billingRun.getIban());
        newRun.setAmountExcludingVat(billingRun.getAmountExcludingVat());
        newRun.setIssuingForTheMonthToCurrent(billingRun.getIssuingForTheMonthToCurrent());
        newRun.setIssuedSeparateInvoices(extractEnums(billingRun.getIssuedSeparateInvoices(), IssuedSeparateInvoice.class));
        newRun.setCurrencyId(billingRun.getCurrencyId());
        newRun.setDeductionFrom(billingRun.getDeductionFrom());
        newRun.setNumberOfIncomeAccount(billingRun.getNumberOfIncomeAccount());
        newRun.setCostCenterControllingOrder(billingRun.getCostCenterControllingOrder());
        newRun.setCustomerDetailId(billingRun.getCustomerDetailId());
        newRun.setProductContractId(billingRun.getProductContractId());
        newRun.setServiceContractId(billingRun.getServiceContractId());
        newRun.setDocumentType(billingRun.getDocumentType());
        newRun.setGoodsOrderId(billingRun.getGoodsOrderId());
        newRun.setServiceOrderId(billingRun.getServiceOrderId());
        newRun.setBasisForIssuing(billingRun.getBasisForIssuing());
        newRun.setManualInvoiceType(billingRun.getManualInvoiceType());
        newRun.setDirectDebit(billingRun.getDirectDebit());
        newRun.setCustomerCommunicationId(billingRun.getCustomerCommunicationId());
        newRun.setListOfInvoices(billingRun.getListOfInvoices());
        newRun.setPriceChange(billingRun.getPriceChange());
        newRun.setPeriodicityCreatedFromId(billingRun.getPeriodicityId());
        newRun.setPeriodicBillingCreatedFrom(billingRun.getId());
        return newRun;
    }

    /**
     * Generates a periodic end date based on the provided `maxEndDate` and `maxEndDateValue`.
     *
     * @param maxEndDate      The maximum end date to use for the calculation.
     * @param maxEndDateValue The maximum end date value to use for the calculation.
     * @return The generated periodic end date, or `null` if the input parameters are invalid.
     */
    private LocalDate generatePeriodicEndDate(BillingEndDate maxEndDate, Integer maxEndDateValue) {
        if (maxEndDateValue == null || maxEndDate == null) {
            return null;
        }
        LocalDate now = maxEndDate.equals(BillingEndDate.CURRENT_MONTH) ? LocalDate.now() : LocalDate.now().minusMonths(1);
        return LocalDate.of(now.getYear(), now.getMonthValue(), maxEndDateValue > now.getMonth().maxLength() ? now.getMonth().maxLength() : maxEndDateValue);

    }

    /**
     * Generates an Excel report containing error information for a specific billing run.
     *
     * @param billingId The ID of the billing run for which to generate the error report.
     * @param protocol  The billing protocol associated with the billing run.
     * @param response  The HTTP servlet response to write the generated Excel file to.
     * @throws DomainEntityNotFoundException If the billing run with the specified ID does not exist.
     */
    public void generateErrorExcel(Long billingId, BillingProtocol protocol, HttpServletResponse response) {
        if (!billingRunRepository.existsById(billingId)) {
            throw new DomainEntityNotFoundException("billingId-Billing not found with id: %s".formatted(billingId));
        }
        multiSheetExcelService.generateExcel(MultiSheetExcelType.BILLING_RUN_ERROR_REPORT.getValue(), response, String.valueOf(billingId), protocol.name());
    }

    /**
     * Validates the provided template ID and sets it on the given BillingRun instance.
     * <p>
     * If the provided template ID matches the existing template ID on the BillingRun, this method returns without making any changes.
     * If the template ID is null, the template ID on the BillingRun is set to null.
     * If the template ID does not exist in the contractTemplateRepository with the expected template purpose and type, an error message is added to the provided errorMessages list.
     * Otherwise, the template ID on the BillingRun is set to the provided template ID.
     *
     * @param templateId    The ID of the template to validate and set.
     * @param termination   The BillingRun instance to update with the validated template ID.
     * @param errorMessages A list to add any error messages to.
     */
    private void validateAndSetTemplate(Long templateId, BillingRun termination, List<String> errorMessages) {
        if (Objects.equals(templateId, termination.getTemplateId()))
            return;
        if (templateId == null) {
            termination.setTemplateId(null);
            return;
        }
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, INVOICE, ContractTemplateType.DOCUMENT, LocalDate.now())) {
            errorMessages.add("commonParameters.templateId-[templateId] with id %s do not exist!;".formatted(templateId));
        }
        termination.setTemplateId(templateId);
    }

    /**
     * Validates the provided email template ID and sets it on the given BillingRun instance.
     * <p>
     * If the provided email template ID matches the existing email template ID on the BillingRun, this method returns without making any changes.
     * If the email template ID is null, the email template ID on the BillingRun is set to null.
     * If the email template ID does not exist in the contractTemplateRepository with the expected template purpose and type, an error message is added to the provided errorMessages list.
     * Otherwise, the email template ID on the BillingRun is set to the provided email template ID.
     *
     * @param templateId    The ID of the email template to validate and set.
     * @param termination   The BillingRun instance to update with the validated email template ID.
     * @param errorMessages A list to add any error messages to.
     */
    private void validateAndSetEmailTemplate(Long templateId, BillingRun termination, List<String> errorMessages) {
        if (Objects.equals(templateId, termination.getEmailTemplateId()))
            return;
        if (templateId == null) {
            termination.setTemplateId(null);
            return;
        }
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, INVOICE, ContractTemplateType.EMAIL, LocalDate.now())) {
            errorMessages.add("commonParameters.emailTemplateId-[emailTemplateId] with id %s do not exist!;".formatted(templateId));
        }
        termination.setEmailTemplateId(templateId);
    }

    /**
     * Sets the billing notifications for a given billing run.
     * <p>
     * This method processes a set of {@link BillingNotificationRequest} objects and creates or updates the corresponding
     * {@link BillingNotification} entities in the database. It performs validation checks to ensure the performer IDs
     * exist in the system, and adds any error messages to the provided {@code errorMessages} list.
     *
     * @param requests      The set of billing notification requests to process.
     * @param billingRunId  The ID of the billing run to associate the notifications with.
     * @param errorMessages A list to add any error messages to.
     */
    public void setBillingNotifications(Set<BillingNotificationRequest> requests, Long billingRunId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }
        int i = 0;

        for (BillingNotificationRequest request : requests) {
            PerformerType performerType = request.getPerformerType();
            switch (performerType) {
                case TAG -> {
                    if (!portalTagRepository.existsPortalTagForGroup(request.getPerformerId(), EntityStatus.ACTIVE)) {
                        errorMessages.add("commonParameters.billingNotifications[%s].performerId-Performer does not exist!;".formatted(i));
                    } else {
                        billingNotificationRepository.save(new BillingNotification(null, billingRunId, null, request.getPerformerId(), PerformerType.TAG, request.getType()));
                    }
                }
                case MANAGER -> {
                    if (!accountManagerRepository.existsByIdAndStatusIn(request.getPerformerId(), List.of(Status.ACTIVE))) {
                        errorMessages.add("commonParameters.billingNotifications[%s].performerId-Performer does not exist!;".formatted(i));
                    } else {
                        billingNotificationRepository.save(new BillingNotification(null, billingRunId, request.getPerformerId(), null, PerformerType.MANAGER, request.getType()));
                    }
                }
            }
        }
    }

    /**
     * Updates the billing notifications for a given billing run.
     * <p>
     * This method processes a set of {@link BillingNotificationRequest} objects and updates the corresponding
     * {@link BillingNotification} entities in the database. It performs validation checks to ensure the performer IDs
     * exist in the system, and adds any error messages to the provided {@code errorMessages} list.
     *
     * @param requests      The set of billing notification requests to process.
     * @param billingRunId  The ID of the billing run to associate the notifications with.
     * @param errorMessages A list to add any error messages to.
     */
    public void updateBillingNotifications(Set<BillingNotificationRequest> requests, BillingRun billingRunId, List<String> errorMessages) {
        Map<BillingNotificationRequest, BillingNotification> collect = billingNotificationRepository.findAllByBilling(billingRunId.getId()).stream()
                .collect(Collectors
                        .toMap(x -> new BillingNotificationRequest(x.getPerformerType(),
                                x.getPerformerType().equals(PerformerType.TAG) ? x.getTag() : x.getEmployee(),
                                x.getType()), j -> j));
        int i = 0;
        if (CollectionUtils.isNotEmpty(requests)) {
            for (BillingNotificationRequest request : requests) {
                BillingNotification remove = collect.remove(request);
                PerformerType performerType = request.getPerformerType();
                if (remove == null) {
                    if (!billingRunId.getStatus().equals(BillingStatus.INITIAL)) {
                        errorMessages.add("commonParameters.billingNotifications[%s].performerId-Can not add new performer because billing is not initial;".formatted(i));
                        return;
                    }
                    switch (performerType) {
                        case TAG -> {
                            if (!portalTagRepository.existsPortalTagForGroup(request.getPerformerId(), EntityStatus.ACTIVE)) {
                                errorMessages.add("commonParameters.billingNotifications[%s].performerId-Performer does not exist!;".formatted(i));
                            } else {
                                billingNotificationRepository.save(new BillingNotification(null, billingRunId.getId(), null, request.getPerformerId(), PerformerType.TAG, request.getType()));
                            }
                        }
                        case MANAGER -> {
                            if (!accountManagerRepository.existsByIdAndStatusIn(request.getPerformerId(), List.of(Status.ACTIVE))) {
                                errorMessages.add("commonParameters.billingNotifications[%s].performerId-Performer does not exist!;".formatted(i));
                            } else {
                                billingNotificationRepository.save(new BillingNotification(null, billingRunId.getId(), request.getPerformerId(), null, PerformerType.MANAGER, request.getType()));
                            }
                        }
                    }
                }
            }
        }
        Collection<BillingNotification> values = collect.values();
        if (!values.isEmpty() && !billingRunId.getStatus().equals(BillingStatus.INITIAL)) {
            errorMessages.add("commonParameters.billingNotifications[%s].performerId-Can not add new performer because billing is not initial;".formatted(i));
            return;
        }
        billingNotificationRepository.deleteAll(values);
    }

    /**
     * Retrieves the billing notification responses for the specified billing run ID.
     *
     * @param billingId The ID of the billing run to retrieve the notification responses for.
     * @return A list of {@link BillingNotificationResponse} entities representing the billing notification responses.
     */
    private List<BillingNotificationResponse> getNotificationResponse(Long billingId) {
        return billingNotificationRepository.findBillingResponse(billingId);
    }

    public Page<ProductShortResponse> listProducts(BillingItemListingRequest request) {
        return productRepository.findAllForListings(
                request.getPrompt(),
                LocalDateTime.now(),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "id")));
    }


    public HttpEntity<ByteArrayResource> downloadDocument(Long documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(() -> new DomainEntityNotFoundException("documentId-Document not found!;"));
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "force-download"));
        header.set("Content-Disposition", "attachment; filename=%s".formatted(UrlEncodingUtil.encodeFileName(document.getName())));
        return new HttpEntity(this.fileService.downloadFile(document.getSignedFileUrl()), header);
    }

    private record InvoiceCorrectionFileContent(
            String invoiceNumber,
            LocalDate invoiceDate
    ) {
    }
}

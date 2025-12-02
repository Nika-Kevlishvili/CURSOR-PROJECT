package bg.energo.phoenix.service.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDocument;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceVatRateValue;
import bg.energo.phoenix.model.entity.billing.invoice.ManualDebitOrCreditNoteInvoiceSummaryData;
import bg.energo.phoenix.model.entity.contract.billing.ContractBillingGroup;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.billing.IncomeAccountName;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.receivable.ReasonForDisconnection;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.*;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminder;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceDocumentType;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceType;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.DefaultAssignmentType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import bg.energo.phoenix.model.enums.receivable.collectionChannel.CustomerConditionType;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupplyRequests.DisconnectionRequestsStatus;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.*;
import bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.listing.DPSRequestsListColumns;
import bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.listing.DPSRequestsListingRequest;
import bg.energo.phoenix.model.request.receivable.disconnectionPowerSupplyRequests.listing.DPSRequestsSearchByEnums;
import bg.energo.phoenix.model.response.receivable.ValidateConditionResponse;
import bg.energo.phoenix.model.response.receivable.disconnectionPowerSupplyRequests.*;
import bg.energo.phoenix.model.response.receivable.powerSupplyDisconnectionReminder.RemindersForDPSRequestResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.invoice.InvoiceDocumentRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceVatRateValueRepository;
import bg.energo.phoenix.repository.billing.invoice.ManualDebitOrCreditNoteInvoiceSummaryDataRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.billing.IncomeAccountNameRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.ReasonForDisconnectionRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.*;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.billing.invoice.InvoiceEventPublisher;
import bg.energo.phoenix.service.billing.invoice.document.TaxInvoiceDocumentGenerationService;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.document.DisconnectionPowerSupplyRequestsDocumentCreationService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBListingUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.DISCONNECTION_POWER_SUPPLY;
import static bg.energo.phoenix.util.epb.EPBDecimalUtils.convertToCurrencyScale;
import static bg.energo.phoenix.util.epb.EPBFinalFields.DISCONNECTION_POWER_SUPPLY_REQUESTS_NUMBER_PREFIX;

@Slf4j
@Service
public class DisconnectionPowerSupplyRequestsService {
    private static final String CUSTOMER_LIABILITY_PREFIX = "Liability-";
    private final DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final ReasonForDisconnectionRepository reasonForDisconnectionRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final DisconnectionPowerSupplyRequestsConditionService conditionService;
    private final PermissionService permissionService;
    private final FileService fileService;
    private final TaskService taskService;
    private final PowerSupplyDisconnectionReminderRepository powerSupplyDisconnectionReminderRepository;
    private final DisconnectionPowerSupplyRequestsFileRepository disconnectionPowerSupplyRequestsFileRepository;
    private final DisconnectionPowerSupplyRequestsPodsRepository disconnectionPowerSupplyRequestsPodsRepository;
    private final DisconnectionPowerSupplyRequestsPodLiabilitiesRepository disconnectionPowerSupplyRequestsPodLiabilitiesRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final IncomeAccountNameRepository incomeAccountNameRepository;
    private final DisconnectionPowerSupplyRequestsCheckedPodsRepository disconnectionPowerSupplyRequestsCheckedPodsRepository;
    private final DisconnectionPowerSupplyRequestsResultsRepository disconnectionPowerSupplyRequestsResultsRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ManualDebitOrCreditNoteInvoiceSummaryDataRepository manualDebitOrCreditNoteInvoiceSummaryDataRepository;
    private final DisconnectionOfPowerRequestTemplatesRepository requestTemplatesRepository;
    private final ProductContractDetailsRepository productContractRepository;
    private final VatRateRepository vatRateRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ContractBillingGroupRepository contractBillingGroupRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceVatRateValueRepository invoiceVatRateValueRepository;
    private final InvoiceEventPublisher invoiceEventPublisher;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final TaxInvoiceDocumentGenerationService taxInvoiceDocumentGenerationService;
    private final PlatformTransactionManager platformTransactionManager;
    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final DisconnectionPowerSupplyRequestsDocumentCreationService disconnectionPowerSupplyRequestsDocumentCreationService;
    private final DocumentsRepository documentsRepository;
    private final InvoiceNumberService invoiceNumberService;
    @PersistenceContext
    private EntityManager entityManager;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;
    @Value("${invoice.document.ftp_directory_path}")
    private String invoiceDocumentFtpDirectoryPath;
    private TransactionTemplate transactionTemplate;

    public DisconnectionPowerSupplyRequestsService(DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository,
                                                   GridOperatorRepository gridOperatorRepository,
                                                   ReasonForDisconnectionRepository reasonForDisconnectionRepository,
                                                   CurrencyRepository currencyRepository,
                                                   CustomerRepository customerRepository,
                                                   DisconnectionPowerSupplyRequestsConditionService conditionService,
                                                   PermissionService permissionService, FileService fileService,
                                                   TaskService taskService,
                                                   PowerSupplyDisconnectionReminderRepository powerSupplyDisconnectionReminderRepository,
                                                   DisconnectionPowerSupplyRequestsFileRepository disconnectionPowerSupplyRequestsFileRepository,
                                                   DisconnectionPowerSupplyRequestsPodsRepository disconnectionPowerSupplyRequestsPodsRepository,
                                                   DisconnectionPowerSupplyRequestsPodLiabilitiesRepository disconnectionPowerSupplyRequestsPodLiabilitiesRepository,
                                                   CustomerLiabilityRepository customerLiabilityRepository,
                                                   DisconnectionPowerSupplyRequestsCheckedPodsRepository disconnectionPowerSupplyRequestsCheckedPodsRepository,
                                                   DisconnectionPowerSupplyRequestsResultsRepository disconnectionPowerSupplyRequestsResultsRepository,
                                                   ContractTemplateRepository contractTemplateRepository,
                                                   ManualDebitOrCreditNoteInvoiceSummaryDataRepository manualDebitOrCreditNoteInvoiceSummaryDataRepository,
                                                   DisconnectionOfPowerRequestTemplatesRepository requestTemplatesRepository,
                                                   ProductContractDetailsRepository productContractRepository, VatRateRepository vatRateRepository,
                                                   CustomerDetailsRepository customerDetailsRepository,
                                                   ContractBillingGroupRepository contractBillingGroupRepository,
                                                   InvoiceRepository invoiceRepository, InvoiceVatRateValueRepository invoiceVatRateValueRepository,
                                                   InvoiceEventPublisher invoiceEventPublisher, ContractTemplateDetailsRepository contractTemplateDetailsRepository,
                                                   AccountManagerRepository accountManagerRepository,
                                                   TaxInvoiceDocumentGenerationService taxInvoiceDocumentGenerationService,
                                                   PlatformTransactionManager platformTransactionManager,
                                                   InvoiceDocumentRepository invoiceDocumentRepository,
                                                   DisconnectionPowerSupplyRequestsDocumentCreationService disconnectionPowerSupplyRequestsDocumentCreationService,
                                                   DocumentsRepository documentsRepository,
                                                   IncomeAccountNameRepository incomeAccountNameRepository,
                                                   InvoiceNumberService invoiceNumberService) {
        this.disconnectionPowerSupplyRequestRepository = disconnectionPowerSupplyRequestRepository;
        this.gridOperatorRepository = gridOperatorRepository;
        this.reasonForDisconnectionRepository = reasonForDisconnectionRepository;
        this.currencyRepository = currencyRepository;
        this.customerRepository = customerRepository;
        this.conditionService = conditionService;
        this.permissionService = permissionService;
        this.fileService = fileService;
        this.taskService = taskService;
        this.powerSupplyDisconnectionReminderRepository = powerSupplyDisconnectionReminderRepository;
        this.disconnectionPowerSupplyRequestsFileRepository = disconnectionPowerSupplyRequestsFileRepository;
        this.disconnectionPowerSupplyRequestsPodsRepository = disconnectionPowerSupplyRequestsPodsRepository;
        this.disconnectionPowerSupplyRequestsPodLiabilitiesRepository = disconnectionPowerSupplyRequestsPodLiabilitiesRepository;
        this.customerLiabilityRepository = customerLiabilityRepository;
        this.disconnectionPowerSupplyRequestsCheckedPodsRepository = disconnectionPowerSupplyRequestsCheckedPodsRepository;
        this.disconnectionPowerSupplyRequestsResultsRepository = disconnectionPowerSupplyRequestsResultsRepository;
        this.contractTemplateRepository = contractTemplateRepository;
        this.manualDebitOrCreditNoteInvoiceSummaryDataRepository = manualDebitOrCreditNoteInvoiceSummaryDataRepository;
        this.requestTemplatesRepository = requestTemplatesRepository;
        this.productContractRepository = productContractRepository;
        this.vatRateRepository = vatRateRepository;
        this.customerDetailsRepository = customerDetailsRepository;
        this.contractBillingGroupRepository = contractBillingGroupRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceVatRateValueRepository = invoiceVatRateValueRepository;
        this.invoiceEventPublisher = invoiceEventPublisher;
        this.contractTemplateDetailsRepository = contractTemplateDetailsRepository;
        this.accountManagerRepository = accountManagerRepository;
        this.taxInvoiceDocumentGenerationService = taxInvoiceDocumentGenerationService;
        this.platformTransactionManager = platformTransactionManager;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.invoiceDocumentRepository = invoiceDocumentRepository;
        this.disconnectionPowerSupplyRequestsDocumentCreationService = disconnectionPowerSupplyRequestsDocumentCreationService;
        this.documentsRepository = documentsRepository;
        this.incomeAccountNameRepository = incomeAccountNameRepository;
        this.invoiceNumberService = invoiceNumberService;
    }

    /**
     * Creates a new Disconnection Power Supply Request and saves it to the database.
     *
     * @param request the request object containing the details of the new Disconnection Power Supply Request
     * @return the ID of the newly created Disconnection Power Supply Request
     * @throws ClientException if the user does not have the necessary permissions to create the request
     */
    @Transactional
    public Long create(DPSRequestsBaseRequest request) {
        List<String> errorMessages = new ArrayList<>();

        if (request.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.DRAFT)) {
            if (checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_CREATE_REQUESTS_AS_DRAFT)) {
                throw new ClientException("You dont have permission to save Disconnection Power supply request as draft", ErrorCode.ACCESS_DENIED);
            }
        }

        if (request.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.EXECUTED)) {
            if (checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_CREATE_REQUESTS_AS_EXECUTE)) {
                throw new ClientException("You dont have permission to save Disconnection Power supply request as execute", ErrorCode.ACCESS_DENIED);
            }
        }

        if (CollectionUtils.isNotEmpty(request.getPods())) {
            checkIfPodIdsAreUnique(request.getPods());
        }
        DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests = mapIntoEntity(request, new DisconnectionPowerSupplyRequests(), errorMessages);
        generateAndSetNumberAndId(disconnectionPowerSupplyRequests);
        disconnectionPowerSupplyRequests.setStatus(EntityStatus.ACTIVE);
        disconnectionPowerSupplyRequests.setDisconnectionRequestsStatus(request.getDisconnectionRequestsStatus());
        disconnectionPowerSupplyRequests.setIsAllSelected(Objects.nonNull(request.getIsAllSelected()) ? request.getIsAllSelected() : false);
        disconnectionPowerSupplyRequests.setPodWithHighestConsumption(Objects.nonNull(request.getPodWithHighestConsumption()) ? request.getPodWithHighestConsumption() : false);
        disconnectionPowerSupplyRequests.setExcludePodIds(saveExcludePodIds(request.getExcludePodIds()));
        disconnectionPowerSupplyRequestRepository.saveAndFlush(disconnectionPowerSupplyRequests);
        Long disconnectionPSRId = disconnectionPowerSupplyRequests.getId();
        saveTemplates(request.getTemplateIds(), disconnectionPSRId, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        Boolean isAllSelected = Objects.requireNonNullElse(request.getIsAllSelected(), false);
        request.setIsAllSelected(isAllSelected);
        Boolean podWithHighestConsumption = Objects.requireNonNullElse(request.getPodWithHighestConsumption(), false);
        request.setPodWithHighestConsumption(podWithHighestConsumption);
        if (disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.DRAFT) && (CollectionUtils.isNotEmpty(request.getPods()) || request.getIsAllSelected() || request.getPodWithHighestConsumption())) {
            mapIntoCheckedPods(disconnectionPSRId, request);
        } else if (disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.EXECUTED) && (CollectionUtils.isNotEmpty(request.getPods()) || request.getIsAllSelected() || request.getPodWithHighestConsumption())) {
            mapIntoPodsResult(request, disconnectionPSRId);
        }

        if (CollectionUtils.isNotEmpty(request.getFiles())) {
            validateAndPrepareFiles(request.getFiles(), disconnectionPSRId, errorMessages);
            if (errorMessages.isEmpty()) {
                addAndDeleteFiles(request, disconnectionPSRId);
            }
        }

        if (request.getDisconnectionRequestsStatus() == DisconnectionRequestsStatus.EXECUTED) {
            disconnectionPowerSupplyRequestsDocumentCreationService.generateDocuments(disconnectionPowerSupplyRequests.getId());
        }

        return disconnectionPSRId;
    }

    /**
     * Retrieves the details of a DisconnectionPowerSupplyRequests entity by its ID.
     *
     * @param id The ID of the DisconnectionPowerSupplyRequests entity to retrieve.
     * @return A DPSRequestsResponse object containing the details of the requested DisconnectionPowerSupplyRequests entity.
     * @throws DomainEntityNotFoundException if the DisconnectionPowerSupplyRequests entity with the specified ID is not found.
     */
    public DPSRequestsResponse view(Long id) {
        Optional<DisconnectionPowerSupplyRequests> disconnectionPowerSupplyRequestOptional = disconnectionPowerSupplyRequestRepository.findById(id);
        if (disconnectionPowerSupplyRequestOptional.isEmpty()) {
            throw new DomainEntityNotFoundException("id-disconnectionPowerSupplyRequestOptional with id %s not found;".formatted(id));
        }

        DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests = disconnectionPowerSupplyRequestOptional.get();
        DisconnectionRequestsStatus disconnectionRequestsStatus = disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus();
        EntityStatus status = disconnectionPowerSupplyRequests.getStatus();
        checkIfUserHasPermission(disconnectionRequestsStatus, status);

        DPSRequestsResponse response = new DPSRequestsResponse(disconnectionPowerSupplyRequests);

        if (Objects.nonNull(disconnectionPowerSupplyRequests.getExcludePodIds()) && !disconnectionPowerSupplyRequests.getExcludePodIds().isEmpty()) {
            List<Long> excludePods = Arrays.stream(disconnectionPowerSupplyRequests.getExcludePodIds().split(","))
                    .map(Long::parseLong).toList();
            response.setExcludePodIds(excludePods);
        }

        fetchAndMapConditionsAndConditionsInfo(disconnectionPowerSupplyRequests.getCondition(), response);
        response.setTaskShortResponse(getTasks(id));

        Long disconnectionReminderId = disconnectionPowerSupplyRequests.getDisconnectionReminderId();
        if (Objects.nonNull(disconnectionReminderId)) {
            Optional<PowerSupplyDisconnectionReminder> reminderForDisconnection = powerSupplyDisconnectionReminderRepository.findById(disconnectionReminderId);
            reminderForDisconnection.ifPresent(powerSupplyDisconnectionReminder -> response.setReminderForDisconnection(new ShortResponse(powerSupplyDisconnectionReminder.getId(), powerSupplyDisconnectionReminder.getReminderNumber())));
        }

        Optional<GridOperator> gridOperator = gridOperatorRepository.findById(disconnectionPowerSupplyRequests.getGridOperatorId());
        if (gridOperator.isPresent()) {
            ShortResponse shortResponse = new ShortResponse(gridOperator.get().getId(), gridOperator.get().getName());
            response.setGridOperator(shortResponse);
        }

        Optional<ReasonForDisconnection> disconnectionReason = reasonForDisconnectionRepository.findById(disconnectionPowerSupplyRequests.getDisconnectionReasonId());
        if (disconnectionReason.isPresent()) {
            ShortResponse shortResponse = new ShortResponse(disconnectionReason.get().getId(), disconnectionReason.get().getName());
            response.setDisconnectionReason(shortResponse);
        }
        response.setTemplateResponses(findTemplatesForContract(disconnectionPowerSupplyRequests.getId()));
        Long currencyId = disconnectionPowerSupplyRequests.getCurrencyId();
        if (Objects.nonNull(currencyId)) {
            Optional<Currency> currency = currencyRepository.findById(currencyId);
            if (currency.isPresent()) {
                ShortResponse shortResponse = new ShortResponse(currency.get().getId(), currency.get().getName());
                response.setCurrency(shortResponse);
            }
        }

        setFiles(disconnectionPowerSupplyRequests, response);

        return response;
    }

    /**
     * Retrieves a page of customers for a power supply disconnection request.
     *
     * @param powerSupplyDisconnectionRequestId the ID of the power supply disconnection request
     * @param request                           the request object containing search and pagination parameters
     * @return a page of {@link CustomersForDPSResponse} objects representing the customers for the disconnection request
     * @throws DomainEntityNotFoundException if the power supply disconnection request with the given ID is not found
     */
    public Page<CustomersForDPSResponse> viewPodTab(Long powerSupplyDisconnectionRequestId, CustomersForDPSRequest request) {
        Optional<DisconnectionPowerSupplyRequests> disconnectionPowerSupplyRequest = disconnectionPowerSupplyRequestRepository.findById(powerSupplyDisconnectionRequestId);
        if (disconnectionPowerSupplyRequest.isEmpty()) {
            throw new DomainEntityNotFoundException("id-disconnectionPowerSupplyRequest with id %s not found;".formatted(powerSupplyDisconnectionRequestId));
        }

        Page<CustomersForDPSResponse> response = null;
        if (disconnectionPowerSupplyRequest.get().getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.DRAFT)
                && Objects.nonNull(request)) {
            List<Long> checkedPodIds = disconnectionPowerSupplyRequestsCheckedPodsRepository.findByPowerSupplyDisconnectionRequestId(powerSupplyDisconnectionRequestId);
            response = loadCustomerForDisconnectionPowerSupply(request);
            for (CustomersForDPSResponse customersForDPSResponse : response.getContent()) {
                if (checkedPodIds.contains(customersForDPSResponse.getPodId())) {
                    customersForDPSResponse.setIsChecked(true);
                }
            }
        }

        if (disconnectionPowerSupplyRequest.get().getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.EXECUTED)
                || disconnectionPowerSupplyRequest.get().getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.FEE_CHARGED)) {
            Sort order = EPBListingUtils.extractSortBy(
                    request.getDirection(),
                    request.getSortBy(),
                    CustomersForDPSListColumns.CUSTOMER,
                    CustomersForDPSListColumns::getValue);
            response = disconnectionPowerSupplyRequestsResultsRepository.filter(
                            EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                            EPBListingUtils.extractSearchBy(request.getSearchBy(), CustomersForDPSSearchByEnums.ALL),
                            request.getIsHighestConsumption(),
                            powerSupplyDisconnectionRequestId,
                            request.getLiabilityAmountFrom(),
                            request.getLiabilityAmountTo(),
                            PageRequest.of(request.getPage(), request.getSize(), order))
                    .map(CustomersForDPSResponse::new);
        }

        return response;
    }

    /**
     * Updates an existing DisconnectionPowerSupplyRequests entity.
     *
     * @param id      The ID of the DisconnectionPowerSupplyRequests entity to update.
     * @param request The request object containing the updated data for the DisconnectionPowerSupplyRequests entity.
     * @return The ID of the updated DisconnectionPowerSupplyRequests entity.
     * @throws DomainEntityNotFoundException If the DisconnectionPowerSupplyRequests entity with the given ID is not found.
     * @throws OperationNotAllowedException  If the DisconnectionPowerSupplyRequests entity is in a status that does not allow editing.
     */
    @Transactional
    public Long update(Long id, DPSRequestsBaseRequest request) {
        List<String> errorMessages = new ArrayList<>();
        Optional<DisconnectionPowerSupplyRequests> disconnectionPowerSupplyRequestOptional = disconnectionPowerSupplyRequestRepository.findById(id);
        if (disconnectionPowerSupplyRequestOptional.isEmpty()) {
            throw new DomainEntityNotFoundException("id-disconnectionPowerSupplyRequest with id %s not found;".formatted(id));
        }
        DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests = disconnectionPowerSupplyRequestOptional.get();

        if (disconnectionPowerSupplyRequests.getStatus().equals(EntityStatus.DELETED)) {
            throw new OperationNotAllowedException("It is not possible to edit DELETED disconnectionPowerSupplyRequest;");
        }

        if (disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.FEE_CHARGED)) {
            throw new OperationNotAllowedException("It is not possible to edit disconnectionPowerSupplyRequest with FEE_CHARGED status;");
        }

        Boolean isAllSelected = Objects.requireNonNullElse(request.getIsAllSelected(), false);
        request.setIsAllSelected(isAllSelected);
        Boolean podWithHighestConsumption = Objects.requireNonNullElse(request.getPodWithHighestConsumption(), false);
        request.setPodWithHighestConsumption(podWithHighestConsumption);
        if (disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.DRAFT) && !request.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.EXECUTED)) {
            if (request.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.FEE_CHARGED)) {
                throw new OperationNotAllowedException("It is not possible to change status from DRAFT to FEE_CHARGED;");
            }
            mapIntoEntity(request, disconnectionPowerSupplyRequests, errorMessages);
            deleteAndAddNewPods(id, request);
        }

        if (disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.EXECUTED)) {
            if (request.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.DRAFT)) {
                throw new OperationNotAllowedException("It is not possible to change status from EXECUTED to DRAFT;");
            }
        }
        if (disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.EXECUTED) && request.getCondition() != null
                && !disconnectionPowerSupplyRequests.getCondition().equals(request.getCondition())) {
            throw new OperationNotAllowedException("It is not possible to change condition for executed disconnectionPowerSupplyRequest;");
        }

        if (CollectionUtils.isNotEmpty(request.getPods())) {
            checkIfPodIdsAreUnique(request.getPods());
        }

        if (CollectionUtils.isNotEmpty(request.getFiles())) {
            validateAndPrepareFiles(request.getFiles(), id, errorMessages);
            if (errorMessages.isEmpty()) {
                addAndDeleteFiles(request, id);
            }
        }

        if (disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.DRAFT)
                && request.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.EXECUTED)) {
            mapIntoPodsResult(request, disconnectionPowerSupplyRequests.getId());
            mapIntoEntity(request, disconnectionPowerSupplyRequests, errorMessages);
        }
        updateTemplates(request.getTemplateIds(), disconnectionPowerSupplyRequests.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        disconnectionPowerSupplyRequests.setDisconnectionRequestsStatus(request.getDisconnectionRequestsStatus());
        disconnectionPowerSupplyRequests.setIsAllSelected(Objects.nonNull(request.getIsAllSelected()) ? request.getIsAllSelected() : false);
        disconnectionPowerSupplyRequests.setPodWithHighestConsumption(Objects.nonNull(request.getPodWithHighestConsumption()) ? request.getPodWithHighestConsumption() : false);
        disconnectionPowerSupplyRequests.setExcludePodIds(saveExcludePodIds(request.getExcludePodIds()));
        disconnectionPowerSupplyRequestRepository.saveAndFlush(disconnectionPowerSupplyRequests);

        if (disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus() != DisconnectionRequestsStatus.EXECUTED && request.getDisconnectionRequestsStatus() == DisconnectionRequestsStatus.EXECUTED) {
            disconnectionPowerSupplyRequestsDocumentCreationService.generateDocuments(disconnectionPowerSupplyRequests.getId());
        }

        return disconnectionPowerSupplyRequests.getId();
    }


    /**
     * Deletes existing pods and adds new pods to the DisconnectionPowerSupplyRequest.
     *
     * @param id      The ID of the DisconnectionPowerSupplyRequest.
     * @param request The updated DisconnectionPowerSupplyRequest containing the new pods.
     */
    public void deleteAndAddNewPods(Long id, DPSRequestsBaseRequest request) {
        List<CustomersForDPSResponse> combined = new ArrayList<>(request.getPods());
        List<Long> podIds = disconnectionPowerSupplyRequestsCheckedPodsRepository.findByPowerSupplyDisconnectionRequestId(id);
        if (request.getIsAllSelected()) {
            combined.addAll(getCustomersIfSelectAllAndPodWithHighestConIsSelected(request, null));
            request.setPods(combined);
        }
        if (request.getPodWithHighestConsumption()) {
            List<CustomersForDPSResponse> withHighestConIsSelected = getCustomersIfSelectAllAndPodWithHighestConIsSelected(request, true);
            combined.addAll(withHighestConIsSelected);
            request.setPods(combined);
        }
        if (CollectionUtils.isNotEmpty(combined)) {
            List<Long> newPodIdsList = EPBListUtils.transform(combined, CustomersForDPSResponse::getPodId);
            if (CollectionUtils.isNotEmpty(podIds)) {
                List<Long> deletedElementsFromList = EPBListUtils.getDeletedElementsFromList(podIds, newPodIdsList);
                disconnectionPowerSupplyRequestsCheckedPodsRepository.deleteAll(disconnectionPowerSupplyRequestsCheckedPodsRepository.findAllByPodIdIn(deletedElementsFromList));
            }
            List<Long> addedElementsFromList = EPBListUtils.getAddedElementsFromList(podIds, newPodIdsList);
            Map<Long, CustomersForDPSResponse> podsWitPodId = EPBListUtils.transformToMap(combined, CustomersForDPSResponse::getPodId);
            List<CustomersForDPSResponse> updatedPodTabRecords = addedElementsFromList.stream().map(podsWitPodId::get).toList();
            request.setPods(updatedPodTabRecords);
            saveCheckedPods(combined, id);
        } else {
            List<Long> deletedElementsFromList = EPBListUtils.getDeletedElementsFromList(podIds, new ArrayList<>());
            disconnectionPowerSupplyRequestsCheckedPodsRepository.deleteAll(disconnectionPowerSupplyRequestsCheckedPodsRepository.findAllByPodIdIn(deletedElementsFromList));
        }
    }


    /**
     * Maps the customers for a disconnection power supply request into a list of checked pods.
     *
     * @param disconnectionPowerSupplyRequestId the ID of the disconnection power supply request
     * @param request                           the request object containing the details for the disconnection power supply request
     */
    private void mapIntoCheckedPods(Long disconnectionPowerSupplyRequestId, DPSRequestsBaseRequest request) {
        List<CustomersForDPSResponse> customersForDPSResponses;
        if (request.getIsAllSelected()) {
            customersForDPSResponses = getCustomersIfSelectAllAndPodWithHighestConIsSelected(request, null);
        } else if (request.getPodWithHighestConsumption()) {
            customersForDPSResponses = getCustomersIfSelectAllAndPodWithHighestConIsSelected(request, true);
            if (CollectionUtils.isNotEmpty(request.getPods())) {
                List<CustomersForDPSResponse> combined = new ArrayList<>(customersForDPSResponses);
                combined.addAll(request.getPods());
                customersForDPSResponses = combined;
            }
        } else {
            customersForDPSResponses = request.getPods();
        }
        saveCheckedPods(customersForDPSResponses, disconnectionPowerSupplyRequestId);
    }


    private void mapIntoPodsResult(DPSRequestsBaseRequest request, Long disconnectionPowerSupplyRequestId) {
        List<CustomersForDPSResponse> customersForDPSResponses;
        if (request.getIsAllSelected()) {
            customersForDPSResponses = getCustomersIfSelectAllAndPodWithHighestConIsSelected(request, null);
            customersForDPSResponses.forEach(response -> response.setIsChecked(true));
        } else if (request.getPodWithHighestConsumption()) {
            customersForDPSResponses = getCustomersIfSelectAllAndPodWithHighestConIsSelected(request, true);
            if (CollectionUtils.isNotEmpty(request.getPods())) {
                List<CustomersForDPSResponse> combined = new ArrayList<>(customersForDPSResponses);
                combined.addAll(request.getPods());
                customersForDPSResponses = combined;
            }
            customersForDPSResponses.forEach(response -> response.setIsChecked(true));
        } else {
            customersForDPSResponses = request.getPods();
        }

        List<DisconnectionPowerSupplyRequestsResults> podsResult = new ArrayList<>();
        for (CustomersForDPSResponse record : customersForDPSResponses) {
            Boolean isChecked = record.getIsChecked();
            if (Objects.isNull(isChecked) || isChecked) {
                DisconnectionPowerSupplyRequestsResults results = DisconnectionPowerSupplyRequestsResults.builder()
                        .customers(record.getCustomers())
                        .contracts(record.getContracts())
                        .altRecipientInvCustomers(record.getAltRecipientInvCustomers())
                        .billingGroups(record.getBillingGroups())
                        .isHighestConsumption(record.getIsHighestConsumption())
                        .liabilitiesInBillingGroup(record.getLiabilitiesInBillingGroup())
                        .liabilitiesInPod(record.getLiabilitiesInPod())
                        .id(record.getPodId())
                        .customerId(record.getCustomerId())
                        .powerSupplyDisconnectionRequestId(disconnectionPowerSupplyRequestId)
                        .existingCustomerReceivables(record.getExistingCustomerReceivables())
                        .podIdentifier(record.getPodIdentifier())
                        .invoiceNumber(record.getInvoiceNumber())
                        .isChecked(true)
                        .podId(record.getPodId())
                        .liabilityAmountCustomer(record.getLiabilityAmountCustomer())
                        .customerNumber(record.getCustomerNumber())
                        .podDetailId(record.getPodDetailId())
                        .build();
                podsResult.add(results);
            }

            DisconnectionPowerSupplyRequestsPods pod = DisconnectionPowerSupplyRequestsPods.builder()
                    .customerId(record.getCustomerId())
                    .podId(record.getPodId())
                    .powerSupplyDisconnectionRequestId(disconnectionPowerSupplyRequestId)
                    .isChecked(Objects.requireNonNullElse(isChecked, false))
                    .build();
            disconnectionPowerSupplyRequestsPodsRepository.saveAndFlush(pod);
            setLiabilityInfo(record, pod.getId());
        }
        disconnectionPowerSupplyRequestsResultsRepository.saveAll(podsResult);

    }

    /**
     * Retrieves a list of tasks associated with the specified disconnection power supply request ID.
     *
     * @param id The ID of the disconnection power supply request.
     * @return A list of task short responses for the tasks associated with the specified disconnection power supply request.
     */
    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByDisconnectionPowerSupplyRequestId(id);
    }

    /**
     * Retrieves liability information for a given power supply disconnection request pod.
     *
     * @param record The PodTabRecord containing the liabilities for the pod.
     * @param podId  The ID of the power supply disconnection request pod.
     */
    public void setLiabilityInfo(CustomersForDPSResponse record, Long podId) {
        List<DisconnectionPowerSupplyRequestsPodLiabilities> podLiabilities = new ArrayList<>();
        String liabilities = String.join(",", record.getLiabilitiesInPod(), record.getLiabilitiesInBillingGroup());
        Set<String> liabilitySet = new HashSet<>(Arrays.asList(liabilities.split(",")));
        for (String liability : liabilitySet) {
            if (!Objects.isNull(liability) && !liability.equals("null") && !liability.isEmpty()) {
                String[] splitLiability = liability.split("-");
                Long liabilityId = Long.parseLong(splitLiability[splitLiability.length - 1]);
                Optional<CustomerLiability> liabilityOptional = customerLiabilityRepository.findById(liabilityId);
                if (liabilityOptional.isPresent()) {
                    DisconnectionPowerSupplyRequestsPodLiabilities podLiability = new DisconnectionPowerSupplyRequestsPodLiabilities();
                    podLiability.setLiabilityAmount(Objects.requireNonNullElse(liabilityOptional.get().getCurrentAmount(), new BigDecimal(0)));
                    podLiability.setCustomerLiabilityId(liabilityOptional.get().getId());
                    podLiability.setPowerSupplyDisconnectionRequestPodId(podId);
                    podLiabilities.add(podLiability);
                }
            }
        }
        disconnectionPowerSupplyRequestsPodLiabilitiesRepository.saveAll(podLiabilities);
    }

    /**
     * Uploads a file containing disconnection power supply requests and saves it to the file system.
     *
     * @param file The file to be uploaded.
     * @return A response containing the saved file details.
     * @throws ClientException if the file name is null.
     */
    @Transactional
    public FileWithStatusesResponse upload(MultipartFile file, List<DocumentFileStatus> statuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null;", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "disconnection_request_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        DisconnectionPowerSupplyRequestsFile disconnectionPowerSupplyRequestsFile = new DisconnectionPowerSupplyRequestsFile(null, formattedFileName, url, null, EntityStatus.ACTIVE, statuses);

        DisconnectionPowerSupplyRequestsFile save = disconnectionPowerSupplyRequestsFileRepository.saveAndFlush(disconnectionPowerSupplyRequestsFile);
        return new FileWithStatusesResponse(save, accountManagerRepository.findByUserName(save.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    /**
     * Downloads a disconnection power supply request file by the given ID.
     *
     * @param id the ID of the disconnection power supply request file to download
     * @return a {@link DisconnectionRequestFileContent} containing the file name and byte array of the downloaded file
     * @throws DomainEntityNotFoundException if the file with the given ID is not found
     */
    public DisconnectionRequestFileContent download(Long id, ContractFileType fileType) {
        if (fileType == ContractFileType.UPLOADED_FILE) {
            DisconnectionPowerSupplyRequestsFile disconnectionPowerSupplyRequestsFile = disconnectionPowerSupplyRequestsFileRepository
                    .findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found;"));

            ByteArrayResource resource = fileService.downloadFile(disconnectionPowerSupplyRequestsFile.getFileUrl());

            return new DisconnectionRequestFileContent(disconnectionPowerSupplyRequestsFile.getName(), resource.getByteArray());
        } else {
            Document document = documentsRepository
                    .findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found;"));

            ByteArrayResource resource = fileService.downloadFile(document.getSignedFileUrl());

            return new DisconnectionRequestFileContent(document.getName(), resource.getByteArray());
        }

    }

    public Page<DisconnectionRequestListingResponse> filter(DPSRequestsListingRequest listingRequest) {
        Sort.Order order = new Sort.Order(Objects.requireNonNullElse(listingRequest.getDirection(), Sort.Direction.DESC), checkSortField(listingRequest));
        return disconnectionPowerSupplyRequestRepository.filter(
                EPBListUtils.convertEnumListIntoStringListIfNotNull(listingRequest.getSupplierTypes()),
                ListUtils.emptyIfNull(listingRequest.getGridOperatorIds()),
                getDisconnectionRequestsStatuses(listingRequest.getDisconnectionRequestsStatuses()),
                listingRequest.getGridOperatorRequestRegistrationDateFrom(),
                listingRequest.getGridOperatorRequestRegistrationDateTo(),
                listingRequest.getCustomerReminderLetterSentDateFrom(),
                listingRequest.getCustomerReminderLetterSentDateTo(),
                listingRequest.getGridOperatorDisconnectionFeePayDateFrom(),
                listingRequest.getGridOperatorDisconnectionFeePayDateTo(),
                listingRequest.getPowerSupplyDisconnectionDateFrom(),
                listingRequest.getPowerSupplyDisconnectionDateTo(),
                EPBStringUtils.fromPromptToQueryParameter(listingRequest.getPrompt()),
                getSearchByEnum(listingRequest),
                listingRequest.getNumberOfPodsFrom(),
                listingRequest.getNumberOfPodsTo(),
                getDisconnectionRequestsStatuses(),
                PageRequest.of(listingRequest.getPage(), listingRequest.getSize(), Sort.by(order))).map(DisconnectionRequestListingResponse::new);
    }

    /**
     * Loads a page of customers for a disconnection power supply request.
     *
     * @param request the request object containing the search parameters
     * @return a page of {@link CustomersForDPSResponse} objects matching the search criteria
     */
    public Page<CustomersForDPSResponse> loadCustomerForDisconnectionPowerSupply(CustomersForDPSRequest request) {
        Sort order = EPBListingUtils.extractSortBy(
                request.getDirection(),
                request.getSortBy(),
                CustomersForDPSListColumns.CUSTOMER,
                CustomersForDPSListColumns::getValue);
        List<String> errorMessages = new ArrayList<>();
        if (Objects.isNull(request.getGridOperatorId())) {
            errorMessages.add("gridOperatorId-gridOperatorId must not be null;");
        }
        if (Objects.isNull(request.getPowerSupplyDisconnectionReminderId())) {
            errorMessages.add("powerSupplyDisconnectionReminderId-powerSupplyDisconnectionReminderId must not be null;");
        }
        if (request.getConditionType().equals(CustomerConditionType.CUSTOMERS_UNDER_CONDITIONS) && Objects.isNull(request.getCondition())) {
            errorMessages.add("[condition] customer_under_conditions_is_empty");
        }
        if (request.getConditionType().equals(CustomerConditionType.LIST_OF_CUSTOMERS) && Objects.isNull(request.getListOfCustomer())) {
            errorMessages.add("[listOfCustomer] list_of_customers_is_empty");
        }
        if (StringUtils.isNotBlank(request.getCondition())) {
            conditionService.validateCondition(request.getCondition(), errorMessages);
            conditionService.validateConditionKeys(request.getCondition(), errorMessages);
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return disconnectionPowerSupplyRequestRepository.customersForDPS(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBListingUtils.extractSearchBy(request.getSearchBy(), CustomersForDPSSearchByEnums.ALL),
                        request.getPowerSupplyDisconnectionReminderId(),
                        new ArrayList<>(),
                        request.getGridOperatorId(),
                        request.getLiabilityAmountFrom(),
                        request.getLiabilityAmountTo(),
                        request.getIsHighestConsumption(),
                        Objects.requireNonNullElse(request.getSortBy(), CustomersForDPSListColumns.CUSTOMER).getValue(),
                        request.getCondition(),
                        Objects.requireNonNullElse(request.getConditionType(), CustomerConditionType.ALL_CUSTOMERS).name(),
                        request.getListOfCustomer(),
                        PageRequest.of(request.getPage(), request.getSize(), order))
                .map(CustomersForDPSResponse::new);
    }

    /**
     * Deletes a disconnection power supply request by the given ID.
     *
     * @param id the ID of the disconnection power supply request to delete
     * @return the ID of the deleted disconnection power supply request
     * @throws DomainEntityNotFoundException if the disconnection power supply request with the given ID is not found
     * @throws OperationNotAllowedException  if the disconnection power supply request is already deleted or has a status other than "DRAFT"
     */
    @Transactional
    public Long delete(Long id) {
        Optional<DisconnectionPowerSupplyRequests> disconnectionPowerSupplyRequest = disconnectionPowerSupplyRequestRepository.findById(id);
        if (disconnectionPowerSupplyRequest.isEmpty()) {
            throw new DomainEntityNotFoundException("id-disconnectionPowerSupplyRequest with id %s not found;".formatted(id));
        }

        if (disconnectionPowerSupplyRequest.get().getStatus().equals(EntityStatus.DELETED)) {
            throw new OperationNotAllowedException("id-disconnectionPowerSupplyRequest with id %s is already deleted;".formatted(id));
        }

        if (!disconnectionPowerSupplyRequest.get().getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.DRAFT)) {
            throw new OperationNotAllowedException("disconnectionRequestsStatus-disconnectionPowerSupplyRequest with id %s has not Draft status and can not be deleted;".formatted(id));
        }

        disconnectionPowerSupplyRequest.get().setStatus(EntityStatus.DELETED);
        disconnectionPowerSupplyRequestRepository.save(disconnectionPowerSupplyRequest.get());
        return id;
    }

    private void generateAndSetNumberAndId(DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests) {
        Long nextSequenceValue = disconnectionPowerSupplyRequestRepository.getNextSequenceValue();
        String number = "%s%s".formatted(DISCONNECTION_POWER_SUPPLY_REQUESTS_NUMBER_PREFIX, nextSequenceValue);
        disconnectionPowerSupplyRequests.setRequestNumber(number);
        disconnectionPowerSupplyRequests.setId(nextSequenceValue);
    }

    private void validateListOfCustomers(String listOfCustomers, DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests, List<String> errorMessages) {
        List<String> notFoundedCustomers = new ArrayList<>();
        Arrays.stream(listOfCustomers.trim().split(",")).forEach(identifier -> {
            Optional<Customer> customer = customerRepository.findByIdentifierAndStatus(identifier.trim(), CustomerStatus.ACTIVE);
            if (customer.isEmpty()) {
                notFoundedCustomers.add(identifier);
            }
        });
        if (!notFoundedCustomers.isEmpty()) {
            errorMessages.add("listOfCustomers-Active Customer with identifier %s not found;".formatted(notFoundedCustomers));
        } else {
            disconnectionPowerSupplyRequests.setListOfCustomers(listOfCustomers);
        }
    }

    public ValidateConditionResponse validateCondition(DPSRequestsConditionValidationRequest request) {
        List<String> errorMessages = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getCondition())) {
            conditionService.validateCondition(request.getCondition(), errorMessages);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return new ValidateConditionResponse(true);
    }

    private void fetchAndMapConditionsAndConditionsInfo(String condition, DPSRequestsResponse response) {
        response.setConditionsInfo(conditionService.getConditionsInfo(condition));
        response.setCondition(condition);
    }

    private void checkIfUserHasPermission(DisconnectionRequestsStatus status, EntityStatus entityStatus) {
        if (status.equals(DisconnectionRequestsStatus.DRAFT)) {
            if (checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DRAFT)) {
                throw new ClientException("You dont have permission to view draft Disconnection Power supply request", ErrorCode.ACCESS_DENIED);
            }
        } else if (status.equals(DisconnectionRequestsStatus.EXECUTED)) {
            if (checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_EXECUTED)) {
                throw new ClientException("You dont have permission to view executed Disconnection Power supply request", ErrorCode.ACCESS_DENIED);
            }
        } else if (status.equals(DisconnectionRequestsStatus.FEE_CHARGED)) {
            if (checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_FEE_CHARGED)) {
                throw new ClientException("You dont have permission to view sent reminders Disconnection Power supply request", ErrorCode.ACCESS_DENIED);
            }
        }

        if (entityStatus.equals(EntityStatus.DELETED)) {
            if (checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DELETED)) {
                throw new ClientException("You dont have permission to view deleted Disconnection Power supply request", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private boolean checkPermission(PermissionEnum permission) {
        return !permissionService.getPermissionsFromContext(DISCONNECTION_POWER_SUPPLY).contains(permission.getId());
    }

    private String checkSortField(DPSRequestsListingRequest request) {
        if (request.getSortBy() == null) {
            return DPSRequestsListColumns.NUMBER.getValue();
        } else
            return request.getSortBy().getValue();
    }

    private String getSearchByEnum(DPSRequestsListingRequest request) {
        String searchByField;
        if (request.getSearchBy() != null) {
            searchByField = request.getSearchBy().getValue();
        } else
            searchByField = DPSRequestsSearchByEnums.ALL.getValue();
        return searchByField;
    }

    private List<String> getDisconnectionRequestsStatuses(List<DisconnectionRequestsStatus> disconnectionRequestsStatuses) {
        List<String> modifiedStatuses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(disconnectionRequestsStatuses)) {
            modifiedStatuses.add("NONE");
            for (DisconnectionRequestsStatus status : disconnectionRequestsStatuses) {
                if (status.equals(DisconnectionRequestsStatus.DRAFT)
                        && !checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DRAFT)) {
                    modifiedStatuses.add(DisconnectionRequestsStatus.DRAFT.name());
                } else if (status.equals(DisconnectionRequestsStatus.EXECUTED)
                        && !checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_EXECUTED)) {
                    modifiedStatuses.add(DisconnectionRequestsStatus.EXECUTED.name());
                } else if (status.equals(DisconnectionRequestsStatus.FEE_CHARGED)
                        && !checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_FEE_CHARGED)) {
                    modifiedStatuses.add(DisconnectionRequestsStatus.FEE_CHARGED.name());
                }
            }
        } else {
            if (!checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DRAFT)) {
                modifiedStatuses.add(DisconnectionRequestsStatus.DRAFT.name());
            }
            if (!checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_EXECUTED)) {
                modifiedStatuses.add(DisconnectionRequestsStatus.EXECUTED.name());
            }
            if (!checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_FEE_CHARGED)) {
                modifiedStatuses.add(DisconnectionRequestsStatus.FEE_CHARGED.name());
            }
        }
        return modifiedStatuses;
    }

    private List<String> getDisconnectionRequestsStatuses() {
        List<String> entityStatuses = new ArrayList<>();
        entityStatuses.add(EntityStatus.ACTIVE.name());
        if (!checkPermission(PermissionEnum.DISCONNECTION_POWER_SUPPLY_VIEW_REQUESTS_DELETED)) {
            entityStatuses.add(EntityStatus.DELETED.name());
        }
        return entityStatuses;
    }

    /**
     * Maps the properties of the given `DPSRequestsBaseRequest` object into the `DisconnectionPowerSupplyRequests` entity.
     * This method performs various validations and updates the entity accordingly.
     *
     * @param request                          The `DPSRequestsBaseRequest` object containing the request data.
     * @param disconnectionPowerSupplyRequests The `DisconnectionPowerSupplyRequests` entity to be updated.
     * @param errorMessages                    A list to store any error messages encountered during the mapping process.
     */
    private DisconnectionPowerSupplyRequests mapIntoEntity(DPSRequestsBaseRequest request, DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests, List<String> errorMessages) {
        disconnectionPowerSupplyRequests.setSupplierType(request.getSupplierType());
        disconnectionPowerSupplyRequests.setGridOpRequestRegDate(request.getGridOpRequestRegDate());
        disconnectionPowerSupplyRequests.setGridOpDisconnectionFeePayDate(request.getGridOpDisconnectionFeePayDate());
        disconnectionPowerSupplyRequests.setPowerSupplyDisconnectionDate(request.getPowerSupplyDisconnectionDate());
        disconnectionPowerSupplyRequests.setLiabilityAmountFrom(request.getLiabilityAmountFrom());
        disconnectionPowerSupplyRequests.setLiabilityAmountTo(request.getLiabilityAmountTo());
        disconnectionPowerSupplyRequests.setCustomerConditionType(request.getConditionType());
        disconnectionPowerSupplyRequests.setListOfCustomers(request.getListOfCustomer());
        disconnectionPowerSupplyRequests.setCondition(request.getCondition());

        Long reminderForDisconnectionId = request.getReminderForDisconnectionId();
        if (!reminderForDisconnectionId.equals(disconnectionPowerSupplyRequests.getDisconnectionReminderId())) {
            Page<RemindersForDPSRequestResponse> reminders = powerSupplyDisconnectionReminderRepository.getRemindersForDPSRequest(EPBStringUtils.fromPromptToQueryParameter(""), null).map(RemindersForDPSRequestResponse::new);
            if (CollectionUtils.isNotEmpty(reminders.getContent())) {
                List<Long> reminderIds = EPBListUtils.transform(reminders.getContent(), RemindersForDPSRequestResponse::getId);
                if (CollectionUtils.isEmpty(reminderIds) || !reminderIds.contains(reminderForDisconnectionId)) {
                    errorMessages.add("reminderForDisconnectionId-Reminder For Disconnection with id %s not found;".formatted(reminderForDisconnectionId));
                } else {
                    disconnectionPowerSupplyRequests.setDisconnectionReminderId(reminderForDisconnectionId);
                    Optional<PowerSupplyDisconnectionReminder> powerSupplyDisconnectionReminder = powerSupplyDisconnectionReminderRepository.findById(reminderForDisconnectionId);
                    powerSupplyDisconnectionReminder.ifPresent(supplyDisconnectionReminder -> disconnectionPowerSupplyRequests.setCustomerReminderLetterSentDate(supplyDisconnectionReminder.getCustomerSendDate()));
                }
            } else {
                errorMessages.add("reminderForDisconnectionId-No Reminders found;");
            }
        }

        if (StringUtils.isNotBlank(request.getCondition()) && !request.getCondition().equals(disconnectionPowerSupplyRequests.getCondition())) {
            conditionService.validateCondition(request.getCondition(), errorMessages);
            conditionService.validateConditionKeys(request.getCondition(), errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            disconnectionPowerSupplyRequests.setCondition(request.getCondition());
        }

        if (!request.getGridOperatorId().equals(disconnectionPowerSupplyRequests.getGridOperatorId())) {
            Optional<GridOperator> gridOperator = gridOperatorRepository.findByIdAndStatusIn(request.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE));
            if (gridOperator.isEmpty()) {
                errorMessages.add("gridOperatorId-gridOperator with id %s not found".formatted(request.getGridOperatorId()));
            } else {
                disconnectionPowerSupplyRequests.setGridOperatorId(request.getGridOperatorId());
            }
        }

        if (!request.getReasonOfDisconnectionId().equals(disconnectionPowerSupplyRequests.getDisconnectionReasonId())) {
            Optional<ReasonForDisconnection> reasonForDisconnection = reasonForDisconnectionRepository.findByIdAndStatuses(request.getReasonOfDisconnectionId(), List.of(NomenclatureItemStatus.ACTIVE));
            if (reasonForDisconnection.isEmpty()) {
                errorMessages.add("reasonOfDisconnectionId-reasonForDisconnection with id %s not found".formatted(request.getReasonOfDisconnectionId()));
            } else {
                disconnectionPowerSupplyRequests.setDisconnectionReasonId(request.getReasonOfDisconnectionId());
            }
        }

        if (Objects.nonNull(request.getCurrencyId()) && !request.getCurrencyId().equals(disconnectionPowerSupplyRequests.getCurrencyId())) {
            Optional<Currency> currency = currencyRepository.findByIdAndStatus(request.getCurrencyId(), List.of(NomenclatureItemStatus.ACTIVE));
            if (currency.isEmpty()) {
                errorMessages.add("currencyId-currency with id %s not found".formatted(request.getCurrencyId()));
            }
        }
        disconnectionPowerSupplyRequests.setCurrencyId(request.getCurrencyId());

        if (request.getConditionType().equals(CustomerConditionType.LIST_OF_CUSTOMERS)) {
            if (!request.getListOfCustomer().equals(disconnectionPowerSupplyRequests.getListOfCustomers())) {
                validateListOfCustomers(request.getListOfCustomer(), disconnectionPowerSupplyRequests, errorMessages);
            }
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return disconnectionPowerSupplyRequests;
    }

    /**
     * Validates and prepares the files associated with a DisconnectionPowerSupplyRequest.
     * <p>
     * This method retrieves the files with the provided IDs, sets the appropriate
     * DisconnectionPowerSupplyRequest ID and status, and saves the files to the repository.
     * If any file is not found, an error message is added to the provided error messages list.
     *
     * @param fileIds                    the list of file IDs to validate and prepare
     * @param disconnectionPowerSupplyId the ID of the DisconnectionPowerSupplyRequest
     * @param errorMessages              the list to add error messages to
     */
    private void validateAndPrepareFiles(List<Long> fileIds, Long disconnectionPowerSupplyId, List<String> errorMessages) {
        Map<Long, DisconnectionPowerSupplyRequestsFile> fileMap =
                disconnectionPowerSupplyRequestsFileRepository.findAllById(fileIds)
                        .stream()
                        .collect(Collectors.toMap(DisconnectionPowerSupplyRequestsFile::getId, f -> f));

        for (Long fileId : fileIds) {
            DisconnectionPowerSupplyRequestsFile file = fileMap.get(fileId);
            if (file == null) {
                errorMessages.add("files-file with id %s not found;".formatted(fileId));
            } else {
                file.setPowerSupplyDisconnectionRequestId(disconnectionPowerSupplyId);
                file.setStatus(EntityStatus.ACTIVE);
                disconnectionPowerSupplyRequestsFileRepository.save(file);
            }
        }
    }

    /**
     * Adds and deletes files associated with a DisconnectionPowerSupplyRequest.
     *
     * @param request the DisconnectionPowerSupplyRequestsEditRequest containing the file IDs to add and delete
     * @param id      the ID of the DisconnectionPowerSupplyRequest
     */
    private void addAndDeleteFiles(DPSRequestsBaseRequest request, Long id) {
        List<Long> FileIdListFromRequest = Objects.requireNonNullElse(request.getFiles(), new ArrayList<>());
        List<DisconnectionPowerSupplyRequestsFile> fileIdsFromDb = getRelatedFiles(id);
        List<Long> oldFileIdList = fileIdsFromDb.stream().map(DisconnectionPowerSupplyRequestsFile::getId).toList();

        List<Long> deletedFromList = EPBListUtils.getDeletedElementsFromList(oldFileIdList, FileIdListFromRequest);
        deletedFromList
                .stream()
                .filter(Objects::nonNull)
                .map(aLong -> fileIdsFromDb
                        .stream()
                        .filter(file -> Objects.equals(file.getId(), aLong))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .forEach(file -> {
                            file.setStatus(EntityStatus.DELETED);
                            disconnectionPowerSupplyRequestsFileRepository.save(file);
                        }
                );
        request.setFiles(EPBListUtils.getAddedElementsFromList(oldFileIdList, FileIdListFromRequest));
    }

    /**
     * Retrieves the list of files related to the specified disconnection power supply request.
     *
     * @param disconnectionPowerSupplyRequestId The ID of the disconnection power supply request.
     * @return The list of related files, or an empty list if no files are found.
     */
    private List<DisconnectionPowerSupplyRequestsFile> getRelatedFiles(Long disconnectionPowerSupplyRequestId) {
        Optional<List<DisconnectionPowerSupplyRequestsFile>> files = disconnectionPowerSupplyRequestsFileRepository.findByPowerSupplyDisconnectionRequestIdAndStatus(disconnectionPowerSupplyRequestId, EntityStatus.ACTIVE);
        return files.orElseGet(ArrayList::new);
    }


    /**
     * Checks if the provided list of {@link CustomersForDPSResponse} objects has unique {@code podId} values.
     * If any duplicate {@code podId} values are found, an {@link IllegalArgumentsProvidedException} is thrown.
     *
     * @param podTabRecordList the list of {@link CustomersForDPSResponse} objects to check
     * @throws IllegalArgumentsProvidedException if any duplicate {@code podId} values are found in the list
     */
    private void checkIfPodIdsAreUnique(List<CustomersForDPSResponse> podTabRecordList) {
        List<Long> podIds = EPBListUtils.transform(podTabRecordList, CustomersForDPSResponse::getPodId);
        Set<Long> uniquePodIds = new HashSet<>(podIds);
        if (!Objects.equals(podIds.size(), uniquePodIds.size())) {
            throw new IllegalArgumentsProvidedException("pods-podId must be unique;");
        }
    }

    public List<Long> conditionResult(Long disconnectionRequestId) {
        return conditionService.conditionResult(disconnectionRequestId);
    }

    public void saveTemplates(Set<Long> templateIds, Long productDetailId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templateIds, ContractTemplatePurposes.REQUEST_DISCONNECT_POWER, ContractTemplateStatus.ACTIVE);

        List<DisconnectionOfPowerSupplyRequestTemplate> templateSubObjects = new ArrayList<>();
        int i = 0;
        for (Long templateId : templateIds) {
            if (!allIdByIdAndStatus.contains(templateId)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i, templateId));
            }
            templateSubObjects.add(new DisconnectionOfPowerSupplyRequestTemplate(templateId, productDetailId));

            i++;
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        requestTemplatesRepository.saveAll(templateSubObjects);
    }

    public void updateTemplates(Set<Long> templateIds, Long productDetailId, List<String> errorMessages) {
        Map<Long, DisconnectionOfPowerSupplyRequestTemplate> templateMap = requestTemplatesRepository.findByProductDetailId(productDetailId).stream().collect(Collectors.toMap(DisconnectionOfPowerSupplyRequestTemplate::getTemplateId, j -> j));
        List<DisconnectionOfPowerSupplyRequestTemplate> templatesToSave = new ArrayList<>();
        Map<Long, Integer> templatesToCheck = new HashMap<>();
        int i = 0;
        for (Long templateId : templateIds) {
            DisconnectionOfPowerSupplyRequestTemplate remove = templateMap.remove(templateId);
            if (remove == null) {
                templatesToSave.add(new DisconnectionOfPowerSupplyRequestTemplate(templateId, productDetailId));
                templatesToCheck.put(templateId, i);
            }
            i++;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templatesToCheck.keySet(), ContractTemplatePurposes.REQUEST_DISCONNECT_POWER, ContractTemplateStatus.ACTIVE);
        templatesToCheck.forEach((key, value) -> {
            if (!allIdByIdAndStatus.contains(key)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(value, key));
            }
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        Collection<DisconnectionOfPowerSupplyRequestTemplate> values = templateMap.values();
        for (DisconnectionOfPowerSupplyRequestTemplate value : values) {
            value.setStatus(EntityStatus.DELETED);
            templatesToSave.add(value);
        }
        requestTemplatesRepository.saveAll(templatesToSave);

    }

    public List<ContractTemplateShortResponse> findTemplatesForContract(Long productDetailId) {
        return requestTemplatesRepository.findForContract(productDetailId, LocalDate.now());
    }

    /**
     * Retrieves a list of customers for a Disconnection Power Supply (DPS) request, considering various search criteria.
     *
     * @param request the base request object containing the search parameters
     * @return a list of {@link CustomersForDPSResponse} objects representing the customers that match the search criteria
     */
    private List<CustomersForDPSResponse> getCustomersIfSelectAllAndPodWithHighestConIsSelected(DPSRequestsBaseRequest request, Boolean podWithHighestCon) {
        return disconnectionPowerSupplyRequestRepository.customersForDPS(
                        EPBStringUtils.fromPromptToQueryParameter(""),
                        CustomersForDPSSearchByEnums.ALL.getValue(),
                        request.getReminderForDisconnectionId(),
                        Objects.requireNonNullElse(request.getExcludePodIds(), new ArrayList<>()),
                        request.getGridOperatorId(),
                        request.getLiabilityAmountFrom(),
                        request.getLiabilityAmountTo(),
                        podWithHighestCon,
                        CustomersForDPSListColumns.CUSTOMER.getValue(),
                        request.getCondition(),
                        Objects.requireNonNullElse(request.getConditionType(), CustomerConditionType.ALL_CUSTOMERS).name(),
                        request.getListOfCustomer(),
                        null)
                .map(CustomersForDPSResponse::new).getContent();
    }

    @Transactional
    public void calculateTaxAndCreateInvoice(Long requestId) {
        List<Pair<Invoice, InvoiceDocument>> execute = transactionTemplate.execute((x) -> {
            DisconnectionPowerSupplyRequests disconnectionPowerSupplyRequests = disconnectionPowerSupplyRequestRepository.findByIdAndStatus(requestId, EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Power supply disconnection request not found!"));
            if (!disconnectionPowerSupplyRequests.getDisconnectionRequestsStatus().equals(DisconnectionRequestsStatus.EXECUTED)) {
                throw new OperationNotAllowedException("Operation not allowed, status is not executed!");
            }
            if (Boolean.TRUE.equals(disconnectionPowerSupplyRequests.getTaxCalculated())) {
                throw new OperationNotAllowedException("Tax is already calculated for this request!");
            }
            List<TaxCalculationResponse> taxCalculationResponses = disconnectionPowerSupplyRequestRepository.fetchLiabilities(requestId);
            List<Pair<Invoice, InvoiceDocument>> results = new ArrayList<>();
            for (TaxCalculationResponse taxCalculationRespons : taxCalculationResponses) {
                results.add(createInvoiceAndLiability(taxCalculationRespons));
            }
            if (!taxCalculationResponses.isEmpty()) {
                disconnectionPowerSupplyRequests.setTaxCalculated(true);
                disconnectionPowerSupplyRequests.setDisconnectionRequestsStatus(DisconnectionRequestsStatus.FEE_CHARGED);
                disconnectionPowerSupplyRequestRepository.saveAndFlush(disconnectionPowerSupplyRequests);
            }
            return results;
        });
        // todo fix !
//        if(execute!=null){
//            transactionTemplate.executeWithoutResult(x->{
//                for (Pair<Invoice, InvoiceDocument> invoiceInvoiceDocumentPair : execute) {
//                    updateDocumentName(invoiceInvoiceDocumentPair.getKey(),invoiceInvoiceDocumentPair.getRight());
//                }
//            });
//        }
    }


    private Pair<Invoice, InvoiceDocument> createInvoiceAndLiability(TaxCalculationResponse response) {

        ContractBillingGroup contractBillingGroup = contractBillingGroupRepository.findById(response.getBillingGroupId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Billing group not found!"));

        Invoice liabilityInvoice = invoiceRepository.findById(response.getInvoiceId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Invoice not found!"));

        CustomerDetails customerDetails = customerDetailsRepository.findById(liabilityInvoice.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Customer details not found!"));


        ProductContractDetails productContract = productContractRepository.findById(liabilityInvoice.getProductContractDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Product contract not found!"));

        VatRate vatRate = vatRateRepository.findGlobalVatRate(LocalDate.now(), PageRequest.of(0, 1))
                .orElseThrow(() -> new DomainEntityNotFoundException("Global vat rate not found!"));

        Currency currency = currencyRepository.findById(response.getCurrencyId())
                .orElseThrow(() -> new DomainEntityNotFoundException("currency not found"));

        CustomerDetails alternative = customerDetailsRepository.findById(liabilityInvoice.getAlternativeRecipientCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Alternative recipient not found!"));

        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository.findRespectiveTemplateDetailsByTemplateIdAndDate(response.getDocumentTemplateId(), LocalDate.now())
                .orElseThrow(() -> new DomainEntityNotFoundException("template detail not found for given template with id %s".formatted(response.getDocumentTemplateId())));

        BigDecimal valueOfVat = response.getTaxForReconnection().multiply(vatRate.getValueInPercent()).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmountIncludingVat = response.getTaxForReconnection().add(valueOfVat).setScale(2, RoundingMode.HALF_UP);


        Pair<Invoice, ManualDebitOrCreditNoteInvoiceSummaryData> invoiceSummaryDataPair =
                transactionTemplate.execute(i ->
                        generateInvoice(response, liabilityInvoice, customerDetails, currency, productContract,
                                contractBillingGroup, templateDetail, totalAmountIncludingVat, valueOfVat, vatRate,
                                alternative));
        InvoiceDocument invoiceDocument = taxInvoiceDocumentGenerationService.generateDocumentForInvoice(invoiceSummaryDataPair.getLeft(), invoiceSummaryDataPair.getRight(), templateDetail);
        return Pair.of(liabilityInvoice, invoiceDocument);

    }

    private void updateDocumentName(Invoice inv, InvoiceDocument invoiceDocument) {
        transactionTemplate.executeWithoutResult((x) -> {
            if (invoiceDocument.getName().contains("%s")) {
                entityManager.refresh(inv);
                log.debug("Invoice number is {}", inv.getInvoiceNumber());
                invoiceDocument.setName(invoiceDocument.getName().replace("%s", inv.getInvoiceStatus() == InvoiceStatus.CANCELLED ? inv.getInvoiceCancellationNumber() : inv.getInvoiceNumber()));
            }
            invoiceDocumentRepository.save(invoiceDocument);
        });
    }

    /**
     * Generates an invoice based on the provided tax calculation response
     * and various billing details, and saves it to the repository.
     *
     * @param response                The tax calculation response containing tax details for the reconnection.
     * @param liabilityInvoice        The existing liability invoice details.
     * @param customerDetails         The details of the customer to whom the invoice is issued.
     * @param currency                The currency information for the invoice.
     * @param productContract         Details of the product contract associated with the invoice.
     * @param contractBillingGroup    The billing group related to the contract.
     * @param templateDetail          The template detail for the invoice.
     * @param totalAmountIncludingVat The total amount including VAT.
     * @param valueOfVat              The VAT value for the transaction.
     * @param vatRate                 The VAT rate applicable to the invoice.
     * @param alternative             Alternative customer details for the recipient.
     * @return A Pair containing the generated Invoice and its associated ManualDebitOrCreditNoteInvoiceSummaryData.
     */
    private Pair<Invoice, ManualDebitOrCreditNoteInvoiceSummaryData> generateInvoice(TaxCalculationResponse response, Invoice liabilityInvoice, CustomerDetails customerDetails, Currency currency, ProductContractDetails productContract, ContractBillingGroup contractBillingGroup, ContractTemplateDetail templateDetail, BigDecimal totalAmountIncludingVat, BigDecimal valueOfVat, VatRate vatRate, CustomerDetails alternative) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(response.getCustomerId());
        invoice.setAccountPeriodId(liabilityInvoice.getAccountPeriodId());
        invoice.setContractBillingGroupId(response.getBillingGroupId());
        invoice.setIncomeAccountNumber(response.getNumberOfIncomeAccount());
        invoice.setBasisForIssuing(response.getBasisForIssuing());
        invoice.setCostCenterControllingOrder(response.getCostCenterControllingOrder());
        invoice.setInvoiceDocumentType(InvoiceDocumentType.INVOICE);
        invoice.setInvoiceStatus(InvoiceStatus.REAL);
        invoice.setCustomerDetailId(customerDetails.getId());
        invoice.setCurrencyId(currency.getId());
        invoice.setCurrencyExchangeRateOnInvoiceCreation(currency.getAltCurrencyExchangeRate());
        invoice.setCurrencyIdInOtherCurrency(currency.getAltCurrencyId());
        invoice.setProductContractId(productContract.getContractId());
        invoice.setProductContractDetailId(productContract.getId());
        invoice.setTaxEventDate(LocalDate.now());
        if (contractBillingGroup.getBillingCustomerCommunicationId() != null) {
            invoice.setCustomerCommunicationId(contractBillingGroup.getBillingCustomerCommunicationId());
        } else {
            invoice.setContractCommunicationId(productContract.getCustomerCommunicationIdForBilling());
        }
        if (contractBillingGroup.getAlternativeRecipientCustomerDetailId() == null) {
            invoice.setAlternativeRecipientCustomerDetailId(customerDetails.getId());
        } else {
            invoice.setAlternativeRecipientCustomerDetailId(liabilityInvoice.getAlternativeRecipientCustomerDetailId());
        }

        invoice.setTemplateDetailId(templateDetail.getId());
        invoice.setInvoiceType(InvoiceType.RECONNECTION);
        invoice.setTotalAmountExcludingVat(response.getTaxForReconnection());
        invoice.setTotalAmountIncludingVat(totalAmountIncludingVat);
        invoice.setTotalAmountOfVatInOtherCurrency(convertToCurrencyScale(valueOfVat.multiply(currency.getAltCurrencyExchangeRate())));
        invoice.setTotalAmountIncludingVatInOtherCurrency(convertToCurrencyScale(totalAmountIncludingVat.multiply(currency.getAltCurrencyExchangeRate())));
        invoice.setTotalAmountExcludingVatInOtherCurrency(convertToCurrencyScale(response.getTaxForReconnection().multiply(currency.getAltCurrencyExchangeRate())));
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDirectDebit((contractBillingGroup.getDirectDebit() != null && contractBillingGroup.getDirectDebit()) || (productContract.getDirectDebit() != null && productContract.getDirectDebit()) || customerDetails.getDirectDebit() != null && customerDetails.getDirectDebit());
        Long bankId = null;
        String iban = null;
        if (contractBillingGroup.getDirectDebit()) {
            bankId = contractBillingGroup.getBankId();
            iban = contractBillingGroup.getIban();
        } else if (productContract.getDirectDebit()) {
            bankId = productContract.getBankId();
            iban = productContract.getIban();
        } else if (customerDetails.getDirectDebit()) {
            bankId = customerDetails.getBank().getId();
            iban = customerDetails.getIban();
        }
        invoice.setBankId(bankId);
        invoice.setIban(iban);
        invoice.setTotalAmountOfVat(valueOfVat);
        invoice.setPaymentDeadline(invoice.getTaxEventDate());
        invoice.setProductDetailId(productContract.getProductDetailId());
        invoice = invoiceRepository.saveAndFlush(invoice);
        invoiceNumberService.fillInvoiceNumber(invoice);
        InvoiceVatRateValue invoiceVatRateValue = new InvoiceVatRateValue(null, vatRate.getValueInPercent(), response.getTaxForReconnection(),
                valueOfVat,
                invoice.getId());
        invoiceVatRateValueRepository.save(invoiceVatRateValue);
        ManualDebitOrCreditNoteInvoiceSummaryData manualDebitOrCreditNoteInvoiceSummaryData = getManualDebitOrCreditNoteInvoiceSummaryData(response, invoice, currency, vatRate.getName());
        manualDebitOrCreditNoteInvoiceSummaryDataRepository.save(manualDebitOrCreditNoteInvoiceSummaryData);

        createLiability(invoice.getAccountPeriodId(), invoice.getPaymentDeadline(), totalAmountIncludingVat, invoice.getTotalAmountIncludingVatInOtherCurrency(),
                invoice.getCurrencyId(), invoice.getInvoiceNumber(), response.getNumberOfIncomeAccount(), response.getCostCenterControllingOrder(), response.getBasisForIssuing(),
                invoice.getDirectDebit(), invoice.getCustomerId(), invoice.getContractBillingGroupId(),
                contractBillingGroup.getAlternativeRecipientCustomerDetailId() == null ? customerDetails.getCustomerId() : alternative.getCustomerId(), invoice.getId());
        DisconnectionPowerSupplyRequestsResults disconnectionPowerSupplyRequestsResult = disconnectionPowerSupplyRequestsResultsRepository.findById(response.getResultId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Disconnection power supply request result not found with ID: " + response.getResultId()));
        disconnectionPowerSupplyRequestsResult.setSavedInvoiceId(invoice.getId());
        disconnectionPowerSupplyRequestsResultsRepository.save(disconnectionPowerSupplyRequestsResult);

        return Pair.of(invoice, manualDebitOrCreditNoteInvoiceSummaryData);
    }


    /**
     * Creates and persists a new customer liability entry with the provided details.
     *
     * @param accountingPeriodId         The ID of the accounting period associated with the liability.
     * @param dueDate                    The due date for the liability payment.
     * @param initialAmount              The initial amount of the liability.
     * @param initialAmountInOtherCcy    The initial amount of the liability in another currency.
     * @param currencyId                 The ID of the currency associated with the liability.
     * @param invoiceNumber              The invoice number related to this liability.
     * @param numberOfIncomeAccount      The income account number associated with the liability.
     * @param costCenterControllingOrder The cost center controlling order for the liability.
     * @param basisForIssuing            The basis for issuing the liability.
     * @param directDebit                Flag indicating if the direct debit is applicable for this liability.
     * @param customerId                 The ID of the customer to whom the liability is registered.
     * @param billingGroupId             The ID of the billing group associated with the liability.
     * @param alternativeRecipient       The ID of an alternative recipient customer, if any.
     * @param invoiceId                  The ID of the invoice associated with this liability.
     */
    private void createLiability(Long accountingPeriodId, LocalDate dueDate, BigDecimal initialAmount, BigDecimal
                                         initialAmountInOtherCcy, Long currencyId, String invoiceNumber,
                                 String numberOfIncomeAccount, String costCenterControllingOrder, String basisForIssuing,
                                 boolean directDebit, Long customerId,
                                 Long billingGroupId, Long alternativeRecipient, Long invoiceId) {
        CustomerLiability customerLiability = new CustomerLiability();
        customerLiability.setAccountPeriodId(accountingPeriodId);
        customerLiability.setDueDate(dueDate);
        customerLiability.setInvoiceId(invoiceId);
        customerLiability.setInitialAmount(initialAmount);
        customerLiability.setInitialAmountInOtherCurrency(initialAmountInOtherCcy);
        customerLiability.setCurrentAmount(initialAmount);
        customerLiability.setCurrentAmountInOtherCurrency(initialAmountInOtherCcy);
        customerLiability.setCurrencyId(currencyId);
        customerLiability.setOutgoingDocumentFromExternalSystem(invoiceNumber);
        customerLiability.setIncomeAccountNumber(numberOfIncomeAccount);
        customerLiability.setCostCenterControllingOrder(costCenterControllingOrder);
        customerLiability.setBasisForIssuing(basisForIssuing);
        customerLiability.setContractBillingGroupId(billingGroupId);
        customerLiability.setAltInvoiceRecipientCustomerId(alternativeRecipient);
        customerLiability.setDirectDebit(directDebit);
        customerLiability.setCustomerId(customerId);
        customerLiability.setLiabilityNumber("temp");
        customerLiability.setStatus(EntityStatus.ACTIVE);
        customerLiability.setCreationType(CreationType.AUTOMATIC);

        String incomeAccountName = incomeAccountNameRepository.findByDefaultAssignmentType(DefaultAssignmentType.DEFAULT_FOR_LIABILITIES.name());
        if (incomeAccountName != null) {
            IncomeAccountName incomeAccount = incomeAccountNameRepository.findByName(incomeAccountName)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Income account name not found with name: %s;".formatted(incomeAccountName)));
            customerLiability.setIncomeAccountNumber(incomeAccount.getNumber());
        }
        customerLiability.setOccurrenceDate(LocalDate.now());

        customerLiabilityRepository.saveAndFlush(customerLiability);
        customerLiability.setLiabilityNumber(CUSTOMER_LIABILITY_PREFIX + customerLiability.getId());
        customerLiabilityRepository.saveAndFlush(customerLiability);
    }

    /**
     * Constructs and returns a ManualDebitOrCreditNoteInvoiceSummaryData object based on the provided
     * TaxCalculationResponse, Invoice, and Currency information, along with a specified VAT rate name.
     *
     * @param response the TaxCalculationResponse object containing tax information for reconnection
     * @param invoice  the Invoice object from which the invoice ID is retrieved
     * @param currency the Currency object that provides currency name and exchange rate details
     * @param name     the name of the VAT rate to be set in the summary data
     * @return a populated ManualDebitOrCreditNoteInvoiceSummaryData object containing invoice summary details
     */
    private ManualDebitOrCreditNoteInvoiceSummaryData getManualDebitOrCreditNoteInvoiceSummaryData
    (TaxCalculationResponse response, Invoice invoice, Currency currency, String name) {
        ManualDebitOrCreditNoteInvoiceSummaryData manualDebitOrCreditNoteInvoiceSummaryData = new ManualDebitOrCreditNoteInvoiceSummaryData();
        manualDebitOrCreditNoteInvoiceSummaryData.setInvoiceId(invoice.getId());
        manualDebitOrCreditNoteInvoiceSummaryData.setValue(response.getTaxForReconnection());
        manualDebitOrCreditNoteInvoiceSummaryData.setValueCurrencyId(response.getCurrencyId());
        manualDebitOrCreditNoteInvoiceSummaryData.setPriceComponentOrPriceComponentGroups(response.getPriceComponent());
        manualDebitOrCreditNoteInvoiceSummaryData.setValueCurrencyName(currency.getName());
        manualDebitOrCreditNoteInvoiceSummaryData.setValueCurrencyExchangeRate(currency.getAltCurrencyExchangeRate());
        manualDebitOrCreditNoteInvoiceSummaryData.setVatRateName(name);
        return manualDebitOrCreditNoteInvoiceSummaryData;
    }

    /**
     * Saves the checked PODs for disconnection power supply requests.
     *
     * @param customersForDPSResponses          the list of customer responses containing POD information
     * @param disconnectionPowerSupplyRequestId the ID of the disconnection power supply request
     */
    private void saveCheckedPods(List<CustomersForDPSResponse> customersForDPSResponses, Long disconnectionPowerSupplyRequestId) {
        List<DisconnectionPowerSupplyRequestsCheckedPods> disconnectionPowerSupplyRequestsCheckedPods = new ArrayList<>();
        for (CustomersForDPSResponse record : customersForDPSResponses) {
            DisconnectionPowerSupplyRequestsCheckedPods checkedPods = new DisconnectionPowerSupplyRequestsCheckedPods();
            checkedPods.setPodId(record.getPodId());
            checkedPods.setPowerSupplyDisconnectionRequestId(disconnectionPowerSupplyRequestId);
            checkedPods.setStatus(EntityStatus.ACTIVE);
            disconnectionPowerSupplyRequestsCheckedPods.add(checkedPods);
        }
        disconnectionPowerSupplyRequestsCheckedPodsRepository.saveAll(disconnectionPowerSupplyRequestsCheckedPods);
    }

    /**
     * Converts a list of pod IDs into a comma-separated string.
     *
     * @param excludePodIds a list of pod IDs to be excluded
     * @return a comma-separated string of pod IDs, or an empty string if the list is null or empty
     */
    private String saveExcludePodIds(List<Long> excludePodIds) {
        StringBuilder result = new StringBuilder();

        if (CollectionUtils.isNotEmpty(excludePodIds)) {
            for (int i = 0; i < excludePodIds.size(); i++) {
                result.append(excludePodIds.get(i));
                if (i < excludePodIds.size() - 1) {
                    result.append(",");
                }
            }
        }
        return result.toString();
    }


    /**
     * Retrieves a list of previously checked PODs based on the provided parameters.
     *
     * @param disconnectionReminderId the ID of the disconnection reminder
     * @param gridOperatorId          the ID of the grid operator
     * @param id                      the ID of the power supply disconnection request
     * @return a list of CustomersForDPSResponse objects representing the checked PODs
     */
    public List<CustomersForDPSResponse> getCheckedPods(Long disconnectionReminderId, Long gridOperatorId, Long id) {
        List<Long> checkedPodIds = disconnectionPowerSupplyRequestsCheckedPodsRepository.findByPowerSupplyDisconnectionRequestId(id);
        List<CustomersForDPSResponse> checkedPods = new ArrayList<>();
        Page<CustomersForDPSResponse> customersForDPSResponses = disconnectionPowerSupplyRequestRepository.customersForDPS(
                EPBStringUtils.fromPromptToQueryParameter(""),
                CustomersForDPSSearchByEnums.ALL.getValue(),
                disconnectionReminderId,
                new ArrayList<>(),
                gridOperatorId,
                null,
                null,
                null,
                CustomersForDPSListColumns.CUSTOMER.getValue(),
                null,
                CustomerConditionType.ALL_CUSTOMERS.name(),
                null,
                null
        ).map(CustomersForDPSResponse::new);
        for (CustomersForDPSResponse customersForDPSResponse : customersForDPSResponses) {
            if (checkedPodIds.contains(customersForDPSResponse.getPodId())) {
                customersForDPSResponse.setIsChecked(true);
                checkedPods.add(customersForDPSResponse);
            }
        }
        return checkedPods;
    }

    /**
     * Sets the files associated with a disconnection power supply request.
     *
     * @param requests the disconnection power supply request
     * @param response the response object to set the files on
     */
    private void setFiles(DisconnectionPowerSupplyRequests requests, DPSRequestsResponse response) {
        Optional<List<DisconnectionPowerSupplyRequestsFile>> disconnectionRequestsFiles = disconnectionPowerSupplyRequestsFileRepository.findByPowerSupplyDisconnectionRequestIdAndStatus(requests.getId(), EntityStatus.ACTIVE);
        List<DisconnectionRequestsFileResponse> disconnectionRequestsFileResponses = new ArrayList<>();
        if (disconnectionRequestsFiles.isPresent()) {
            for (DisconnectionPowerSupplyRequestsFile item : disconnectionRequestsFiles.get()) {
                DisconnectionRequestsFileResponse contractFile = new DisconnectionRequestsFileResponse(item, accountManagerRepository.findByUserName(item.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
                disconnectionRequestsFileResponses.add(contractFile);
            }
        }

        disconnectionRequestsFileResponses.addAll(documentsRepository.findDocumentsForDisconnectionRequest(requests.getId()).stream()
                .map(f -> new DisconnectionRequestsFileResponse(f, accountManagerRepository.findByUserName(f.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .toList());
        response.setFiles(disconnectionRequestsFileResponses);
    }
}

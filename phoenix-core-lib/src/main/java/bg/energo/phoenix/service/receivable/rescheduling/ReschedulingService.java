package bg.energo.phoenix.service.receivable.rescheduling;

import bg.energo.common.utils.JsonUtils;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.ReverseReschedulingOffsettingResult;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.receivable.*;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessment;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiability;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByPayment;
import bg.energo.phoenix.model.entity.receivable.customerLiability.CustomerLiabilityPaidByReceivable;
import bg.energo.phoenix.model.entity.receivable.payment.Payment;
import bg.energo.phoenix.model.entity.receivable.rescheduling.*;
import bg.energo.phoenix.model.enums.contract.ContractFileType;
import bg.energo.phoenix.model.enums.customer.CommunicationDataType;
import bg.energo.phoenix.model.enums.receivable.DirectOffsettingSourceType;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineOutDocType;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.process.latePaymentFIne.InterestCalculationResponseDTO;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.receivable.rescheduling.*;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.receivable.rescheduling.*;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.customerAssessment.CustomerAssessmentRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityPaidByPaymentRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityPaidByReceivableRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityPaidByReschedulingRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.repository.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.*;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.ReschedulingDocumentCreationService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.service.receivable.customerLiability.LiabilityDirectOffsettingService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.service.receivable.latePaymentFine.LatePaymentFineService;
import bg.energo.phoenix.service.receivable.manualLiabilityOffsetting.ManualLiabilityOffsettingService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.RESCHEDULING;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReschedulingService {
    private static final String RESCHEDULING_PREFIX = "Rescheduling-";
    private final ReschedulingRepository reschedulingRepository;
    private final ReschedulingMapperService reschedulingMapperService;
    private final PermissionService permissionService;
    private final ReschedulingTasksRepository reschedulingTasksRepository;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final FileService fileService;
    private final ReschedulingFilesRepository reschedulingFilesRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ReschedulingTemplatesRepository reschedulingTemplatesRepository;
    private final CustomerAssessmentRepository customerAssessmentRepository;
    private final ReschedulingPlansRepository reschedulingPlansRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CurrencyRepository currencyRepository;
    private final InterestRateRepository interestRateRepository;
    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final ReschedulingCalculationService reschedulingCalculationService;
    private final CustomerLiabilityService customerLiabilityService;
    private final LatePaymentFineService latePaymentFineService;
    private final ReschedulingDocumentCreationService reschedulingDocumentCreationService;
    private final DocumentsRepository documentsRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final FileArchivationService fileArchivationService;
    private final ManualLiabilityOffsettingRepository manualLiabilityOffsettingRepository;
    private final ManualLiabilityOffsettingService manualLiabilityOffsettingService;
    private final CustomerLiabilityPaidByPaymentRepository customerLiabilityPaidByPaymentRepository;
    private final CustomerReceivableService customerReceivableService;
    private final DirectReceivableOffsettingService directReceivableOffsettingService;
    private final CustomerLiabilityPaidByReceivableRepository customerLiabilityPaidByReceivableRepository;
    private final ReverseOffsettingService reverseOffsettingService;
    private final ReschedulingLiabilitiesRepository reschedulingLiabilitiesRepository;
    private final CustomerLiabilityPaidByReschedulingRepository customerLiabilityPaidByResSchedulingRepository;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final AutomaticOffsettingService automaticOffsettingService;
    private final ReschedulingDraftLiabilitiesRepository reschedulingDraftLiabilitiesRepository;
    private final ReverseReschedulingOffsettingService reverseReschedulingOffsettingService;
    private final ReschedulingReversalTransactionRepository reschedulingReversalTransactionRepository;

    private final LiabilityDirectOffsettingService liabilityDirectOffsettingService;
    private final ReverseReschedulingDataService reverseReschedulingDataService;

    @PersistenceContext
    private final EntityManager em;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    private static ReschedulingReversalTransaction getReschedulingReversalTransaction(Long reschedulingId, Long transactionId) {
        ReschedulingReversalTransaction reschedulingReversalTransaction = new ReschedulingReversalTransaction();
        reschedulingReversalTransaction.setReschedulingId(reschedulingId);
        reschedulingReversalTransaction.setTransactionId(transactionId);
        return reschedulingReversalTransaction;
    }

    /**
     * Creates a new Rescheduling entity and saves it to the database.
     *
     * @param request the ReschedulingRequest containing the parameters to create the Rescheduling
     * @return the ID of the newly created Rescheduling
     * @throws ClientException if there are any errors during the creation process
     */
    @Transactional
    public Long create(ReschedulingRequest request) {
        log.info("Creating Rescheduling with request: %s".formatted(request));
        List<String> errorMessages = new ArrayList<>();
        Rescheduling rescheduling = reschedulingMapperService.mapParametersForCreate(request);
        checkCustomerAssessment(rescheduling, request, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        reschedulingRepository.saveAndFlush(rescheduling);
        saveTemplates(request.getTemplateRequests().stream().map(ReschedulingTemplateRequest::getTemplateId).collect(Collectors.toSet()), rescheduling.getId(), errorMessages);
        String reschedulingNumber = RESCHEDULING_PREFIX + rescheduling.getId();
        rescheduling.setReschedulingNumber(reschedulingNumber);
        validateFiles(request.getFiles(), errorMessages, rescheduling.getId());
        archiveFiles(rescheduling);
        request.setReschedulingLpfs(CollectionUtils.isEmpty(request.getReschedulingLpfs()) ? new ArrayList<>() : request.getReschedulingLpfs());
        checkIfLiabilitiesCanReschedule(request, errorMessages);

        if (request.getReschedulingStatus().equals(ReschedulingStatus.EXECUTED)) {
            List<Long> liabilityFromInstallment = createLiabilityFromInstallment(request, rescheduling.getId());
            BigDecimal amount = customerLiabilityRepository.sumLiabilityAmounts(liabilityFromInstallment);
            rescheduling.setCurrentAmount(amount);
            rescheduling.setInitialAmount(amount);
            reschedulingRepository.saveAndFlush(rescheduling);
            offsetOldLiabilities(request.getLiabilityIdsForRescheduling(), rescheduling.getId(), errorMessages);
            createLatePaymentFineAndLiability(request.getReschedulingLpfs(), rescheduling.getId());
            reschedulingMapperService.createReschedulingLiabilities(request.getLiabilityIdsForRescheduling(), rescheduling.getId(), request.getCustomerId(), errorMessages);
            reschedulingDocumentCreationService.generateDocument(rescheduling.getId(), request.getTemplateRequests());
        }
        saveReschedulingDraftLiabilities(rescheduling.getId(), request.getLiabilityIdsForRescheduling(), request.getCustomerId(), errorMessages);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return rescheduling.getId();
    }

    private void saveReschedulingDraftLiabilities(Long reschedulingId, List<Long> customerLiabilityIds, Long customerId, List<String> errorMessages) {
        if (customerLiabilityIds != null) {
            Set<Long> customerLiabilitiesByCustomerIdAndIdInAndStatusIn = customerLiabilityRepository.findIdByCustomerIdAndIdInAndStatusIn(customerId, customerLiabilityIds, List.of(EntityStatus.ACTIVE));

            customerLiabilityIds.forEach(liability -> {
                if (!customerLiabilitiesByCustomerIdAndIdInAndStatusIn.contains(liability)) {
                    errorMessages.add("Customer Liability ID " + liability + "attached to customer " + customerId + " not found!;");
                }
                ReschedulingDraftLiabilities reschedulingDraftLiabilities = new ReschedulingDraftLiabilities();
                reschedulingDraftLiabilities.setCustomerLiabilityId(liability);
                reschedulingDraftLiabilities.setReschedulingId(reschedulingId);
                reschedulingDraftLiabilitiesRepository.save(reschedulingDraftLiabilities);
            });
        }
    }

    @Transactional
    public void reversal(Long reschedulingId) {
        List<String> errorMessages = new ArrayList<>();
        Rescheduling rescheduling = reschedulingRepository.findByIdAndStatus(reschedulingId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Rescheduling with id: %s not found", reschedulingId)));

        if (!rescheduling.getReschedulingStatus().equals(ReschedulingStatus.EXECUTED)) {
            throw new OperationNotAllowedException("Can not reverse rescheduling , status is not executed!");
        }

        if (rescheduling.getReversed()) {
            throw new OperationNotAllowedException("Rescheduling is already reversed!");
        }

        List<CustomerLiability> installments = customerLiabilityRepository.findInstallments(reschedulingId);
        List<CustomerLiability> paidInstallments = installments.stream().
                filter(customerLiability -> !customerLiability.getInitialAmount().equals(customerLiability.getCurrentAmount())).toList();
        List<Long> paidLiabilityIds = paidInstallments.stream().map(CustomerLiability::getId).collect(Collectors.toList());

        List<Long> lpfs = customerLiabilityRepository.findLpfsConnectedToLiabilities(installments.stream()
                .map(CustomerLiability::getId).collect(Collectors.toList()));
        lpfs.forEach(lpf -> latePaymentFineService.reverse(lpf, true));

        List<Long> mloIds = manualLiabilityOffsettingRepository.findMlosConnectedToLiabilities(paidLiabilityIds);

        mloIds.forEach(manualLiabilityOffsettingService::reversal);


        List<Object[]> liabilitiesPaidByPayment = customerLiabilityPaidByPaymentRepository.findPaymentsByLiabilityIds(paidLiabilityIds);
        liabilitiesPaidByPayment.forEach(object -> {
                    CustomerLiabilityPaidByPayment customerLiabilityPaidByPayment = (CustomerLiabilityPaidByPayment) object[1];
                    Payment payment = (Payment) object[0];
                    Long liabilityId = (Long) object[2];
                    Long receivableId = customerReceivableService.createFromPayment(payment, customerLiabilityPaidByPayment.getAmount(), false);
                    directReceivableOffsettingService.directReceivableOffsetting(
                            DirectOffsettingSourceType.LIABILITY,
                            liabilityId,
                            receivableId,
                            permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                            permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                            customerLiabilityPaidByPayment.getAmount(),
                            customerLiabilityPaidByPayment.getCurrencyId(),
                            LocalDateTime.now(),
                            OperationContext.RSR
                    );
                }
        );

        List<CustomerLiabilityPaidByReceivable> offsetingsToReverse = customerLiabilityPaidByReceivableRepository.findByLiabilitieIds(paidLiabilityIds);
        offsetingsToReverse.forEach(offsetting -> reverseOffsettingService.reverseOffsetting(
                offsetting.getId(), LocalDateTime.now(), permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
        ));

        List<Long> offsettingsToReverse = customerLiabilityPaidByResSchedulingRepository.findByReschedulingId(rescheduling.getId());
        offsettingsToReverse.forEach(reverse -> reverseOffsettingService.reverseOffsetting(
                reverse, LocalDateTime.now(), permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
        ));

        List<Long> lpfForOldLiabilities = reschedulingLiabilitiesRepository.findFinesByReschedulingId(rescheduling.getId());
        lpfForOldLiabilities.forEach(lpf -> latePaymentFineService.reverse(lpf, true));


        ReverseReschedulingDataService.ReverseReschedulingResult reverseReschedulingData = reverseReschedulingDataService.getReverseReschedulingData(reschedulingId, LocalDateTime.now(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId());

        List<Long> oldLiabs = new ArrayList<>(Arrays.asList(reverseReschedulingData.getOldLiabilities()));
        List<Long> receivables = new ArrayList<>(Arrays.asList(reverseReschedulingData.getRestoredReceivables()));
        for (int i = 0; i < oldLiabs.size(); i++) {
            CustomerLiability liability = customerLiabilityRepository.findById(oldLiabs.get(i)).orElseThrow(() -> new DomainEntityNotFoundException("Not found!"));
            em.refresh(liability);
            if (liability.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
                for (Long receivable : receivables) {
                    ReverseReschedulingOffsettingResult reversalData = reverseReschedulingOffsettingService.getReversalData(reschedulingId, receivable, liability.getId(), LocalDateTime.now());
                    if (reversalData.getOffsettingAmount().compareTo(BigDecimal.ZERO) > 0) {
                        Long transactionId = liabilityDirectOffsettingService.directOffsetting(DirectOffsettingSourceType.RECEIVABLE,
                                receivable, liability.getId(), permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(), "RSR", reversalData.getOffsettingAmount(), reversalData.getOffsettingCurrencyId().longValue());
                        log.info("Transaction id {}", transactionId);
                        ReschedulingReversalTransaction reschedulingReversalTransaction = getReschedulingReversalTransaction(reschedulingId, transactionId);
                        reschedulingReversalTransactionRepository.saveAndFlush(reschedulingReversalTransaction);
                    }
                    em.refresh(liability);
                    if (liability.getCurrentAmount().compareTo(BigDecimal.ZERO) == 0) {
                        try {
                            String lpfJson = customerLiabilityRepository.calculateLatePaymentAndInterestRateOnlinePayment(liability.getId(), liability.getFullOffsetDate());
                            if (lpfJson != null) {
                                ObjectMapper objectMapper = new ObjectMapper();
                                objectMapper.registerModule(new JavaTimeModule());
                                InterestCalculationResponseDTO interestCalc = objectMapper.readValue(lpfJson, InterestCalculationResponseDTO.class);
                                BigDecimal amount = interestCalc.getCalculatedInterest();
                                Long lpfLiabilityId = generateLpfNeededReturningLiabilityId(liability, errorMessages, amount, reschedulingId);
                                if (lpfLiabilityId != null) {
                                    oldLiabs.add(i + 1, lpfLiabilityId);
                                }
                                break;
                            }

                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        for (Long liab : oldLiabs) {
            executeAutomaticLiabilityOffsetting(liab);
        }

        for (Long receivable : receivables) {
            executeAutomaticReceivableOffsetting(receivable);
        }

        rescheduling.setReversed(true);
        reschedulingRepository.save(rescheduling);
    }

    private void executeAutomaticLiabilityOffsetting(Long customerLiability) {
        automaticOffsettingService.offsetOfLiabilityAndReceivable(
                null,
                customerLiability,
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
        );
    }

    public void executeAutomaticReceivableOffsetting(Long customerReceivable) {
        automaticOffsettingService.offsetOfLiabilityAndReceivable(
                customerReceivable,
                null,
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId()
        );
    }

    private Long generateLpfNeededReturningLiabilityId(CustomerLiability customerLiability, List<String> errorMessages, BigDecimal amount, Long reschedulingId) {
        return latePaymentFineService.createLatePaymentFineAndLiability(
                customerLiability,
                amount,
                errorMessages,
                new ArrayList<>(),
                LatePaymentFineOutDocType.RESCHEDULING,
                LocalDate.now(),
                reschedulingId
        );
    }

    public List<CustomerCommunicationDataResponse> getCustomerCommunicationData(Long customerId, Long customerDetailId, CommunicationDataType communicationDataType) {
        if (!customerDetailsRepository.existsByIdAndCustomerId(customerDetailId, customerId)) {
            throw new DomainEntityNotFoundException("Customer do not have Detail with id %s;".formatted(customerDetailId));
        }
        log.debug("Retrieving communication data for customer details id: {} and data type: {}", customerDetailId, communicationDataType);
        List<CustomerCommunicationDataResponse> communicationDataResponseList = customerRepository.customerCommunicationDataList(
                customerDetailId,
                communicationDataType.equals(CommunicationDataType.BILLING)
                        ? communicationContactPurposeProperties.getBillingCommunicationId()
                        : communicationContactPurposeProperties.getContractCommunicationId()
        );
        if (communicationDataResponseList != null) {
            for (CustomerCommunicationDataResponse com : communicationDataResponseList) {
                com.setConcatPurposes(customerRepository.getConcatPurposeFromCustomerCommunicationData(com.getId()));
            }
        }
        return communicationDataResponseList;
    }

    private void checkIfLiabilitiesCanReschedule(ReschedulingRequest request, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(request.getLiabilityIdsForRescheduling())) {
            errorMessages.add("No Liabilities selected for rescheduling;");
        } else {
            if (customerLiabilityService.existsReschedulingLiabilitiesByCustomerIdAndLiabilityIds(request.getCustomerId(), request.getLiabilityIdsForRescheduling())) {
                errorMessages.add("Incorrect Liability or Customer, not acceptable for rescheduling;");
            }
        }
    }

    /**
     * Calculates the rescheduling details based on the provided request parameters and returns the calculation response.
     *
     * @param calculateRequest the request containing the parameters for the rescheduling calculation
     * @return the ReschedulingCalculationResponse containing the calculated rescheduling details
     */
    @Transactional
    public ReschedulingCalculationResponse calculate(ReschedulingCalculateRequest calculateRequest) {
        List<String> errorMessages = new ArrayList<>();

        List<Long> liabilityIds = calculateRequest.getLiabilityIds();
        for (Long liabilityId : liabilityIds) {
            Optional<CustomerLiability> liability = customerLiabilityRepository.findById(liabilityId);
            if (liability.isEmpty()) {
                errorMessages.add("liabilityIds-Liability with id %s not found;".formatted(liabilityId));
            }
        }

        Integer currencyId = calculateRequest.getInstallmentCurrencyId();
        Optional<Currency> currency = currencyRepository.findById((long) currencyId);
        if (currency.isEmpty()) {
            errorMessages.add("installmentCurrencyId-Currency with id %s not found;".formatted(currencyId));
        }

        Long replaceInstallmentRateId = calculateRequest.getReplaceInstallmentRateId();
        if (Objects.nonNull(replaceInstallmentRateId)) {
            Optional<InterestRate> interestRate = interestRateRepository.findById(replaceInstallmentRateId);
            if (interestRate.isEmpty()) {
                errorMessages.add("replaceInstallmentRateId-Interest rate with id %s not found;".formatted(replaceInstallmentRateId));
            }
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        String calculateResult;
        try {
            calculateResult = reschedulingCalculationService.calculate(calculateRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReschedulingCalculationResponse reschedulingCalculationResponse = JsonUtils.fromJson(calculateResult, ReschedulingCalculationResponse.class);
        if (Objects.nonNull(reschedulingCalculationResponse)) {
            roundResponse(reschedulingCalculationResponse);
            calculateSumOfAmounts(reschedulingCalculationResponse);
        }
        return reschedulingCalculationResponse;
    }

    private void roundResponse(ReschedulingCalculationResponse reschedulingCalculationResponse) {
        for (ReschedulingInstallment reschedulingInstallment : reschedulingCalculationResponse.getInstalments()) {
            reschedulingInstallment.setInstallmentAmount(reschedulingInstallment.getInstallmentAmount().setScale(2, RoundingMode.DOWN));
        }
    }

    /**
     * Retrieves and returns a Rescheduling entity by its ID, along with its associated tasks and contract templates.
     *
     * @param id the ID of the Rescheduling to retrieve
     * @return a ReschedulingResponse containing the Rescheduling details, its associated tasks, and contract templates
     * @throws DomainEntityNotFoundException if the Rescheduling with the given ID is not found
     * @throws ClientException               if the user does not have the necessary permissions to view the Rescheduling
     */
    @Transactional
    public ReschedulingResponse view(Long id) {
        log.info("Previewing Rescheduling with id: %s".formatted(id));

        Rescheduling rescheduling = reschedulingRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Rescheduling with id: %s;".formatted(id)));
        if (rescheduling.getStatus().equals(EntityStatus.DELETED)) {
            if (!hasDeletedPermission()) {
                throw new ClientException("You don't have View deleted Rescheduling Permission;", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!hasViewPermission()) {
                throw new ClientException("You don't have View Rescheduling Permission;", ErrorCode.ACCESS_DENIED);
            }
        }

        ReschedulingResponse response = reschedulingMapperService.mapToReschedulingResponse(rescheduling);
        response.setCommunicationDataResponseForBilling(getCustomerCommunicationData(rescheduling.getCustomerId(), rescheduling.getCustomerDetailId(), CommunicationDataType.BILLING));
        response.setCommunicationDataResponseForContract(getCustomerCommunicationData(rescheduling.getCustomerId(), rescheduling.getCustomerDetailId(), CommunicationDataType.CONTRACT));
        response.setReschedulingTasksResponses(getTasks(id));
        response.setContractTemplateShortResponses(findTemplatesForContract(rescheduling.getId()));
        setReschedulingFiles(rescheduling, response);

        return response;
    }

    /**
     * Sets the rescheduling files in the provided ReschedulingResponse.
     * <p>
     * This method retrieves the active rescheduling files associated with the given Rescheduling entity,
     * creates ReschedulingFileResponse objects for each file, and adds them to the ReschedulingResponse.
     * It also retrieves any additional documents associated with the Rescheduling and adds them to the
     * ReschedulingFileResponse list.
     *
     * @param rescheduling The Rescheduling entity to retrieve the files for.
     * @param response     The ReschedulingResponse to set the files on.
     */
    private void setReschedulingFiles(Rescheduling rescheduling, ReschedulingResponse response) {
        List<ReschedulingFiles> reschedulingFiles = reschedulingFilesRepository.findByReschedulingIdAndStatus(rescheduling.getId(), EntityStatus.ACTIVE).stream().toList();
        List<ReschedulingFileResponse> reschedulingFilesResponse = new ArrayList<>();
        for (ReschedulingFiles item : reschedulingFiles) {
            ReschedulingFileResponse contractFile = new ReschedulingFileResponse(item, accountManagerRepository.findByUserName(item.getSystemUserId())
                    .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
            reschedulingFilesResponse.add(contractFile);
        }
        reschedulingFilesResponse.addAll(documentsRepository.findDocumentsForRescheduling(rescheduling.getId()).stream()
                .map(f -> new ReschedulingFileResponse(f, accountManagerRepository.findByUserName(f.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .toList());
        response.setFiles(reschedulingFilesResponse);
    }

    /**
     * Updates an existing Rescheduling entity with the provided request parameters.
     *
     * @param id      The ID of the Rescheduling to update.
     * @param request The ReschedulingUpdateRequest containing the updated parameters.
     * @return The ID of the updated Rescheduling entity.
     * @throws DomainEntityNotFoundException If the Rescheduling with the given ID is not found.
     * @throws ClientException               If the Rescheduling has an Executed status, which makes it not possible to edit.
     */
    @Transactional
    public Long update(Long id, ReschedulingUpdateRequest request) {
        log.info("Updating Rescheduling with id: %s".formatted(id));
        List<String> errorMessages = new ArrayList<>();

        Rescheduling rescheduling = reschedulingRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Rescheduling not found by ID: %s;".formatted(id)));

        if (rescheduling.getReschedulingStatus().equals(ReschedulingStatus.EXECUTED))
            throw new ClientException("It is not possible to edit Rescheduling with Executed status;", ErrorCode.OPERATION_NOT_ALLOWED);

        request.setReschedulingLpfs(CollectionUtils.isEmpty(request.getReschedulingLpfs()) ? new ArrayList<>() : request.getReschedulingLpfs());
        checkCustomerAssessment(rescheduling, request, errorMessages);
        reschedulingMapperService.mapParametersForUpdate(request, rescheduling, errorMessages);
        updateTemplates(request.getTemplateRequests().stream().map(ReschedulingTemplateRequest::getTemplateId).collect(Collectors.toSet()), rescheduling.getId(), errorMessages);
        editFiles(rescheduling, errorMessages, request);
        archiveFiles(rescheduling);
        rescheduling.setCustomerDetailId(request.getCustomerDetailId());
        checkIfLiabilitiesCanReschedule(request, errorMessages);

        if (request.getReschedulingStatus().equals(ReschedulingStatus.EXECUTED)) {
            List<Long> liabilityFromInstallment = createLiabilityFromInstallment(request, rescheduling.getId());
            BigDecimal amount = customerLiabilityRepository.sumLiabilityAmounts(liabilityFromInstallment);
            rescheduling.setCurrentAmount(amount);
            rescheduling.setInitialAmount(amount);
            reschedulingRepository.saveAndFlush(rescheduling);
            offsetOldLiabilities(request.getLiabilityIdsForRescheduling(), rescheduling.getId(), errorMessages);
            createLatePaymentFineAndLiability(request.getReschedulingLpfs(), rescheduling.getId());
            reschedulingDocumentCreationService.generateDocument(rescheduling.getId(), request.getTemplateRequests());
        }
        updateReschedulingDraftLiabilities(rescheduling.getId(), rescheduling.getCustomerId(), request.getLiabilityIdsForRescheduling(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        reschedulingRepository.save(rescheduling);
        return rescheduling.getId();
    }

    private void updateReschedulingDraftLiabilities(Long reschedulingId, Long customerId, List<Long> liabilityIds, List<String> errorMessages) {
        List<Long> existing = reschedulingDraftLiabilitiesRepository.findByReschedulingId(reschedulingId);
        reschedulingDraftLiabilitiesRepository.deleteAllById(existing);
        saveReschedulingDraftLiabilities(reschedulingId, liabilityIds, customerId, errorMessages);
    }

    /**
     * Uploads a file to the rescheduling files repository and returns a response containing the file details.
     *
     * @param file     The file to be uploaded.
     * @param statuses The statuses to be associated with the uploaded file.
     * @return A ReschedulingFileResponse containing the details of the uploaded file.
     * @throws ClientException If the file name is null.
     */
    @Transactional
    public FileWithStatusesResponse upload(MultipartFile file, List<DocumentFileStatus> statuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null;", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "rescheduling_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        ReschedulingFiles reschedulingFiles = ReschedulingFiles
                .builder()
                .name(formattedFileName)
                .localFileUrl(url)
                .fileStatuses(statuses)
                .status(EntityStatus.ACTIVE)
                .build();

        ReschedulingFiles saved = reschedulingFilesRepository.saveAndFlush(reschedulingFiles);
        return new FileWithStatusesResponse(saved, accountManagerRepository.findByUserName(saved.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    /**
     * Downloads a file from the rescheduling files repository.
     *
     * @param id The ID of the file to be downloaded.
     * @return A ReschedulingFileContent object containing the file name and content.
     * @throws DomainEntityNotFoundException If the file with the given ID is not found.
     */
    public ReschedulingFileContent download(Long id) {
        ReschedulingFiles reschedulingFiles = reschedulingFilesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found;"));

        ByteArrayResource resource = fileService.downloadFile(reschedulingFiles.getLocalFileUrl());

        return new ReschedulingFileContent(reschedulingFiles.getName(), resource.getByteArray());
    }


    /**
     * Downloads a file from the rescheduling files repository or the documents repository, depending on the provided file type.
     * If the file is archived, it will attempt to download the archived file. Otherwise, it will download the file from the local file URL.
     *
     * @param id       The ID of the file to be downloaded.
     * @param fileType The type of the file to be downloaded (either UPLOADED_FILE or a document).
     * @return A ReschedulingFileContent object containing the file name and content.
     * @throws DomainEntityNotFoundException If the file or document with the given ID is not found.
     * @throws Exception                     If there is an error downloading the file.
     */
    public ReschedulingFileContent checkForArchivationAndDownload(Long id, ContractFileType fileType) throws Exception {
        if (fileType == ContractFileType.UPLOADED_FILE) {
            ReschedulingFiles reschedulingFiles = reschedulingFilesRepository
                    .findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found;"));

            if (Boolean.TRUE.equals(reschedulingFiles.getIsArchived())) {
                if (Objects.isNull(reschedulingFiles.getLocalFileUrl())) {
                    ByteArrayResource fileContent = archivationService.downloadArchivedFile(reschedulingFiles.getDocumentId(), reschedulingFiles.getFileId());

                    return new ReschedulingFileContent(fileContent.getFilename(), fileContent.getContentAsByteArray());
                }
            }

            ByteArrayResource resource = fileService.downloadFile(reschedulingFiles.getLocalFileUrl());

            return new ReschedulingFileContent(reschedulingFiles.getName(), resource.getByteArray());
        } else {
            Document document = documentsRepository
                    .findById(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found;"));

            ByteArrayResource resource = fileService.downloadFile(document.getSignedFileUrl());

            return new ReschedulingFileContent(document.getName(), resource.getByteArray());
        }
    }

    /**
     * Deletes a Rescheduling entity by the given ID.
     *
     * @param id The ID of the Rescheduling entity to be deleted.
     * @return The ID of the deleted Rescheduling entity.
     * @throws DomainEntityNotFoundException If the Rescheduling entity with the given ID is not found.
     * @throws ClientException               If the Rescheduling entity has a status of EXECUTED, which is not allowed to be deleted.
     */
    public Long delete(Long id) {
        log.info("Deleting Rescheduling with id: %s".formatted(id));

        Rescheduling rescheduling = reschedulingRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Rescheduling with id: %s;".formatted(id)));

        if (rescheduling.getReschedulingStatus().equals(ReschedulingStatus.EXECUTED))
            throw new ClientException("It is not possible to delete Rescheduling with Executed status;", ErrorCode.OPERATION_NOT_ALLOWED);

        rescheduling.setStatus(EntityStatus.DELETED);
        reschedulingRepository.save(rescheduling);
        return rescheduling.getId();
    }

    /**
     * Retrieves a paginated list of rescheduling records based on the provided request parameters.
     *
     * @param request The request object containing the search and pagination criteria.
     * @return A page of rescheduling listing responses.
     */
    public Page<ReschedulingListingResponse> list(ReschedulingListingRequest request) {
        log.info("Calling Rescheduling listing with request: %s".formatted(request));
        List<EntityStatus> statuses = getEntityStatuses(request);

        return reschedulingRepository.filter(
                getSearchByEnum(request.getSearchFields()),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getNumberOfInstallmentFrom(),
                request.getNumberOfInstallmentTo(),
                request.getInstallmentDueDayFrom(),
                request.getInstallmentDueDayTo(),
                request.getCreateDateFrom(),
                request.getCreateDateTo(),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(statuses),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getReschedulingStatuses()),
                CollectionUtils.isEmpty(request.getReverseStatuses()) ? new ArrayList<Boolean>() : request.getReverseStatuses(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(
                                new Sort.Order(request.getDirection(), getSorByEnum(request.getColumns()))
                        )
                )
        );
    }

    private List<EntityStatus> getEntityStatuses(ReschedulingListingRequest request) {
        List<EntityStatus> statuses = request.getStatuses() == null || request.getStatuses().isEmpty()
                ? new ArrayList<>(Arrays.asList(EntityStatus.ACTIVE, EntityStatus.DELETED))
                : new ArrayList<>(request.getStatuses());

        List<EntityStatus> allowedStatuses;
        if (hasViewPermission() && hasDeletedPermission()) {
            allowedStatuses = Arrays.asList(EntityStatus.values());
        } else if (hasDeletedPermission()) {
            allowedStatuses = List.of(EntityStatus.DELETED);
        } else if (hasViewPermission()) {
            allowedStatuses = List.of(EntityStatus.ACTIVE);
        } else {
            allowedStatuses = List.of();
        }
        statuses.retainAll(allowedStatuses);
        return statuses;
    }

    /**
     * Retrieves the search field value based on the provided ReschedulingSearchFields enum.
     *
     * @param searchFields The ReschedulingSearchFields enum value.
     * @return The search field value as a String, or the value of ReschedulingSearchFields.ALL.getValue() if searchFields is null.
     */
    private String getSearchByEnum(ReschedulingSearchFields searchFields) {
        return searchFields != null ? searchFields.getValue() : ReschedulingSearchFields.ALL.getValue();
    }

    /**
     * Retrieves the sort field value based on the provided ReschedulingListColumns enum.
     *
     * @param sortByColumn The ReschedulingListColumns enum value.
     * @return The sort field value as a String, or the value of ReschedulingListColumns.CREATION_DATE.getValue() if sortByColumn is null.
     */
    private String getSorByEnum(ReschedulingListColumns sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : ReschedulingListColumns.CREATION_DATE.getValue();
    }

    /**
     * Retrieves a list of tasks associated with the specified rescheduling ID.
     *
     * @param id the ID of the rescheduling to retrieve tasks for
     * @return a list of {@link TaskShortResponse} objects representing the tasks associated with the rescheduling
     */
    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByReschedulingId(id);
    }

    /**
     * Checks if the current user has the necessary permissions to delete rescheduling tasks.
     *
     * @return true if the user has the RESCHEDULING_VIEW_DELETE permission, false otherwise
     */
    private boolean hasDeletedPermission() {
        return permissionService.permissionContextContainsPermissions(RESCHEDULING, List.of(PermissionEnum.RESCHEDULING_VIEW_DELETE));
    }

    /**
     * Checks if the current user has the necessary permissions to view rescheduling tasks.
     *
     * @return true if the user has the RESCHEDULING_VIEW permission, false otherwise
     */
    private boolean hasViewPermission() {
        return permissionService.permissionContextContainsPermissions(RESCHEDULING, List.of(PermissionEnum.RESCHEDULING_VIEW));
    }

    /**
     * Saves a set of rescheduling templates associated with the specified rescheduling ID.
     *
     * @param templateIds    The set of template IDs to save.
     * @param reschedulingId The ID of the rescheduling to associate the templates with.
     * @param errorMessages  A list to store any error messages encountered during the operation.
     */
    public void saveTemplates(Set<Long> templateIds, Long reschedulingId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templateIds, ContractTemplatePurposes.RESCHEDULING, ContractTemplateStatus.ACTIVE);

        List<ReschedulingTemplates> cbgTemplates = new ArrayList<>();
        int i = 0;
        for (Long templateId : templateIds) {
            if (!allIdByIdAndStatus.contains(templateId)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i, templateId));
            }
            cbgTemplates.add(new ReschedulingTemplates(templateId, reschedulingId));

            i++;
        }
        if (!errorMessages.isEmpty()) {
            return;
        }
        reschedulingTemplatesRepository.saveAll(cbgTemplates);
    }

    /**
     * Updates the templates associated with a rescheduling object.
     *
     * @param templateIds         The set of template IDs to update.
     * @param objectionToChangeId The ID of the rescheduling object to associate the templates with.
     * @param errorMessages       A list to store any error messages encountered during the operation.
     */
    public void updateTemplates(Set<Long> templateIds, Long objectionToChangeId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Map<Long, ReschedulingTemplates> templateMap = reschedulingTemplatesRepository.findByProductDetailId(objectionToChangeId).stream().collect(Collectors.toMap(ReschedulingTemplates::getTemplateId, j -> j));
        List<ReschedulingTemplates> templatesToSave = new ArrayList<>();
        Map<Long, Integer> templatesToCheck = new HashMap<>();
        int i = 0;
        for (Long templateId : templateIds) {
            ReschedulingTemplates remove = templateMap.remove(templateId);
            if (remove == null) {
                templatesToSave.add(new ReschedulingTemplates(templateId, objectionToChangeId));
                templatesToCheck.put(templateId, i);
            }
            i++;
        }
        Set<Long> allIdByIdAndStatus = contractTemplateRepository.findAllIdByIdAndStatusAndPurpose(templatesToCheck.keySet(), ContractTemplatePurposes.RESCHEDULING, ContractTemplateStatus.ACTIVE);
        templatesToCheck.forEach((key, value) -> {
            if (!allIdByIdAndStatus.contains(key)) {
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(value, key));
            }
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        Collection<ReschedulingTemplates> values = templateMap.values();
        for (ReschedulingTemplates value : values) {
            value.setStatus(EntityStatus.DELETED);
            templatesToSave.add(value);
        }
        reschedulingTemplatesRepository.saveAll(templatesToSave);

    }

    /**
     * Retrieves a list of contract template short responses for the given product detail ID.
     *
     * @param productDetailId The ID of the product detail to find templates for.
     * @return A list of {@link ContractTemplateShortResponse} objects representing the templates found for the given product detail.
     */
    public List<ContractTemplateShortResponse> findTemplatesForContract(Long productDetailId) {
        return reschedulingTemplatesRepository.findForContract(productDetailId, LocalDate.now());
    }

    /**
     * Checks the customer assessment associated with the given rescheduling request.
     * If the request is to save and execute the rescheduling, and the customer assessment ID
     * in the request matches the rescheduling's customer assessment ID, but the customer
     * assessment is already connected to another executed rescheduling, an error message
     * is added to the provided list of error messages.
     *
     * @param rescheduling  The rescheduling object to check.
     * @param request       The rescheduling update request.
     * @param errorMessages The list of error messages to add to if an issue is found.
     */
    public void checkCustomerAssessment(Rescheduling rescheduling, ReschedulingRequest request, List<String> errorMessages) {
        List<CustomerAssessment> customerAssessmentsForRescheduling = customerAssessmentRepository.getCustomerAssessmentsForRescheduling(rescheduling.getCustomerId());
        List<Long> customerAssessmentIds = EPBListUtils.transform(customerAssessmentsForRescheduling, CustomerAssessment::getId);
        if (request.getReschedulingStatus().equals(ReschedulingStatus.EXECUTED)) {
            if ((request.getCustomerAssessmentId().equals(rescheduling.getCustomerAssessmentId())
                    && !customerAssessmentIds.contains(rescheduling.getCustomerAssessmentId()))
                    || !customerAssessmentIds.contains(request.getCustomerAssessmentId())) {
                errorMessages.add("Cannot execute this rescheduling because a customer assessment is already connected with another executed rescheduling;");
            }
        }
    }

    /**
     * Validates the files associated with a rescheduling request.
     * <p>
     * This method retrieves the active rescheduling files with the given IDs, and ensures
     * that all the files exist. If a file is not found, an error message is added to the
     * provided list of error messages. The method also sets the reschedulingId property
     * of the retrieved files.
     *
     * @param fileIds        The IDs of the files to validate.
     * @param errorMessages  The list of error messages to add to if a file is not found.
     * @param reschedulingId The ID of the rescheduling to associate the files with.
     */
    private void validateFiles(List<Long> fileIds, List<String> errorMessages, Long reschedulingId) {
        Map<Long, ReschedulingFiles> fileMap = reschedulingFilesRepository.findByIdsAndStatuses(fileIds, List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ReschedulingFiles::getId, f -> f));
        for (Long fileId : fileIds) {
            ReschedulingFiles reschedulingFiles = fileMap.get(fileId);
            if (reschedulingFiles == null) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else {
                reschedulingFiles.setReschedulingId(reschedulingId);
            }
        }
    }

    /**
     * Archives the files associated with the given rescheduling.
     * <p>
     * This method retrieves all active rescheduling files for the given rescheduling,
     * downloads the file content, and archives the files in the EDMS system. The archived
     * files are associated with the rescheduling number, customer identifier, and other
     * relevant metadata.
     *
     * @param rescheduling The rescheduling object for which to archive the files.
     */
    private void archiveFiles(Rescheduling rescheduling) {
        Set<ReschedulingFiles> reschedulingFiles = reschedulingFilesRepository.findByReschedulingIdAndStatus(rescheduling.getId(), EntityStatus.ACTIVE);

        if (CollectionUtils.isNotEmpty(reschedulingFiles)) {
            for (ReschedulingFiles reschedulingFile : reschedulingFiles) {
                try {
                    reschedulingFile.setNeedArchive(true);
                    reschedulingFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_RESCHEDULING_FILE);
                    reschedulingFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_RESCHEDULING_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), rescheduling.getReschedulingNumber()),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );

                    fileArchivationService.archive(reschedulingFile);
                } catch (Exception e) {
                    log.error("Cannot archive file: [%s]".formatted(reschedulingFile.getLocalFileUrl()), e);
                }
            }
        }
    }

    /**
     * Edits the files associated with the given rescheduling.
     * <p>
     * This method retrieves the files specified in the request, associates them with the
     * given rescheduling, and marks any existing files that are not in the request as
     * deleted.
     *
     * @param rescheduling  The rescheduling object for which to edit the files.
     * @param errorMessages A list to store any error messages encountered during the operation.
     * @param request       The request containing the file IDs to be associated with the rescheduling.
     */
    public void editFiles(Rescheduling rescheduling, List<String> errorMessages, ReschedulingUpdateRequest request) {
        Map<Long, ReschedulingFiles> fileMap = reschedulingFilesRepository.findByIdsAndStatuses(request.getFiles(), List.of(EntityStatus.ACTIVE))
                .stream().collect(Collectors.toMap(ReschedulingFiles::getId, f -> f));
        Set<ReschedulingFiles> existing = reschedulingFilesRepository.findByReschedulingIdAndStatus(rescheduling.getId(), EntityStatus.ACTIVE);

        for (Long fileId : request.getFiles()) {
            ReschedulingFiles reschedulingFile = fileMap.get(fileId);
            if (reschedulingFile == null) {
                errorMessages.add("file not found with id : " + fileId + ";");
            } else {
                reschedulingFile.setReschedulingId(rescheduling.getId());
            }
        }

        for (ReschedulingFiles existingFile : existing) {
            ReschedulingFiles reconnectionFile = fileMap.get(existingFile.getId());
            if (reconnectionFile == null) {
                existingFile.setStatus(EntityStatus.DELETED);
            }
        }
    }

    /**
     * Calculates the sum of amounts for the installments in the provided rescheduling calculation response.
     *
     * @param reschedulingCalculationResponse The rescheduling calculation response containing the installments.
     */
    private void calculateSumOfAmounts(ReschedulingCalculationResponse reschedulingCalculationResponse) {
        if (Objects.nonNull(reschedulingCalculationResponse.getInstalments())) {
            BigDecimal sumOfAmount = BigDecimal.ZERO;
            BigDecimal sumOfPrincipalAmount = BigDecimal.ZERO;
            BigDecimal sumOfInterestAmount = BigDecimal.ZERO;
            BigDecimal sumOfFeeAmount = BigDecimal.ZERO;
            for (ReschedulingInstallment installment : reschedulingCalculationResponse.getInstalments()) {
                sumOfAmount = sumOfAmount.add(installment.getInstallmentAmount());
                sumOfPrincipalAmount = sumOfPrincipalAmount.add(installment.getPrincipalAmount());
                sumOfInterestAmount = sumOfInterestAmount.add(installment.getInterestAmount());
                sumOfFeeAmount = sumOfFeeAmount.add(installment.getFee());
            }
            reschedulingCalculationResponse.setSumOfAmount(sumOfAmount.setScale(2, RoundingMode.DOWN));
            reschedulingCalculationResponse.setSumOfPrincipalAmount(sumOfPrincipalAmount.setScale(2, RoundingMode.DOWN));
            reschedulingCalculationResponse.setSumOfInterestAmount(sumOfInterestAmount.setScale(2, RoundingMode.DOWN));
            reschedulingCalculationResponse.setSumOfFeeAmount(sumOfFeeAmount.setScale(2, RoundingMode.DOWN));
        }
    }

    /**
     * Creates a customer liability from the installment details in the provided rescheduling request.
     *
     * @param request The rescheduling request containing the installment details.
     */
    private List<Long> createLiabilityFromInstallment(ReschedulingRequest request, Long reschedulingId) {
        List<ReschedulingInstallment> instalments = request.getInstallments();
        List<Long> liabilityInstallments = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(instalments)) {
            List<ReschedulingPlans> reschedulingPlans = new ArrayList<>();
            for (ReschedulingInstallment instalment : instalments) {
                ReschedulingPlans reschedulingPlan = createPlansData(instalment, reschedulingId);
                reschedulingPlans.add(reschedulingPlan);
                Long liabilityFromRescheduling = customerLiabilityService.createLiabilityFromRescheduling(reschedulingId, instalment, request.getCurrencyId(), request.getCustomerId());
                liabilityInstallments.add(liabilityFromRescheduling);
            }
            reschedulingPlansRepository.saveAll(reschedulingPlans);
        }
        return liabilityInstallments;
    }

    /**
     * Offsets old liabilities by creating a record of the liabilities paid by the rescheduling.
     *
     * @param liabilityIds   The IDs of the liabilities to be offset.
     * @param reschedulingId The ID of the rescheduling.
     * @param errorMessages  A list to store any error messages that occur during the process.
     */
    public void offsetOldLiabilities(List<Long> liabilityIds, Long reschedulingId, List<String> errorMessages) {
        List<CustomerLiability> customerLiabilities = customerLiabilityRepository.findAllById(liabilityIds);
        customerLiabilities.forEach(liability -> {
            if (liability.getCurrentAmount().compareTo(BigDecimal.ZERO) != 0) {
                saveLiabilitiesPayedByRescheduling(reschedulingId, liability);
            } else {
                errorMessages.add("Liability with id %s has a current amount of zero and cannot be offset;".formatted(liability.getId()));
            }
        });

        customerLiabilityRepository.saveAll(customerLiabilities);
    }

    /**
     * Creates late payment fine liabilities and associates them with the rescheduling.
     *
     * @param reschedulingLpfs The list of rescheduling late payment fine details.
     * @param reschedulingId   The ID of the rescheduling.
     */
    public void createLatePaymentFineAndLiability(List<ReschedulingLpfs> reschedulingLpfs, Long reschedulingId) {
        List<CustomerLiability> customerLiabilityList = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        for (ReschedulingLpfs entry : reschedulingLpfs) {
            if (!entry.getInterestDefaultCurrency().equals(BigDecimal.ZERO)) {
                Long liabilityId = entry.getId();
                BigDecimal liabilityAmount = entry.getInterestDefaultCurrency();
                CustomerLiability customerLiability = latePaymentFineService.getCustomerLiability(liabilityId);
                if (Objects.isNull(customerLiability)) {
                    errorMessages.add("Customer liability not found with id %s".formatted(entry));
                    continue;
                }

                Long latePaymentFineLiabilityId = latePaymentFineService.createLatePaymentFineAndLiability(
                        customerLiability,
                        liabilityAmount,
                        errorMessages,
                        new ArrayList<>(),
                        LatePaymentFineOutDocType.RESCHEDULING,
                        LocalDate.now(),
                        reschedulingId
                );

                Optional<CustomerLiability> customerLiabilityFromLatePaymentFine = customerLiabilityRepository.findById(latePaymentFineLiabilityId);
                if (customerLiabilityFromLatePaymentFine.isPresent()) {
                    CustomerLiability liability = customerLiabilityFromLatePaymentFine.get();
                    saveLiabilitiesPayedByRescheduling(reschedulingId, liability);
                    customerLiabilityList.add(liability);
                }
            }
        }

        customerLiabilityRepository.saveAll(customerLiabilityList);
        //TODO Temporarily commented
//        customerLiabilityPaidByReschedulingRepository.saveAll(customerLiabilityPaidByReschedulingList);
    }

    /**
     * Saves a customer liability that has been paid by a rescheduling.
     *
     * @param reschedulingId The ID of the rescheduling.
     * @param liability      The customer liability that has been paid by the rescheduling.
     */
    private void saveLiabilitiesPayedByRescheduling(Long reschedulingId, CustomerLiability liability) {
        liabilityDirectOffsettingService
                .directOffsetting(
                        DirectOffsettingSourceType.RESCHEDULING,
                        reschedulingId,
                        liability.getId(),
                        permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                        permissionService.getLoggedInUserId() == null ? "system.admin" : permissionService.getLoggedInUserId(),
                        OperationContext.RSC.name(),
                        null,
                        null
                );
    }

    private ReschedulingPlans createPlansData(ReschedulingInstallment instalment, Long reschedulingId) {
        ReschedulingPlans reschedulingPlans = new ReschedulingPlans();
        reschedulingPlans.setReschedulingId(reschedulingId);
        reschedulingPlans.setInstallmentNumber(instalment.getInstallmentNumber().toString());
        reschedulingPlans.setAmount(instalment.getInstallmentAmount());
        reschedulingPlans.setPrincipalAmount(instalment.getPrincipalAmount());
        reschedulingPlans.setInterestAmount(instalment.getInterestAmount());
        reschedulingPlans.setFee(instalment.getFee());
        reschedulingPlans.setDueDate(instalment.getDueDate());
        reschedulingPlans.setAmountWithoutInterests(instalment.getInterestAmount());
        return reschedulingPlans;
    }

}
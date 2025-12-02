package bg.energo.phoenix.service.receivable.deposit;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.entity.receivable.deposit.*;
import bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting.ReceivableTemplateRequest;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.deposit.ContractType;
import bg.energo.phoenix.model.enums.receivable.deposit.DepositListingSortingType;
import bg.energo.phoenix.model.enums.receivable.deposit.DepositListingType;
import bg.energo.phoenix.model.enums.receivable.deposit.DepositPaymentDeadlineExclude;
import bg.energo.phoenix.model.enums.receivable.manualLiabilityOffsetting.ReceivableTemplateType;
import bg.energo.phoenix.model.enums.receivable.offsetting.ObjectOffsettingType;
import bg.energo.phoenix.model.enums.template.ContractTemplateLanguage;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.request.receivable.deposit.DepositContractRequest;
import bg.energo.phoenix.model.request.receivable.deposit.DepositCreateRequest;
import bg.energo.phoenix.model.request.receivable.deposit.DepositListingRequest;
import bg.energo.phoenix.model.request.receivable.deposit.PaymentDeadlineAfterWithdrawalRequest;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.model.response.proxy.FileContent;
import bg.energo.phoenix.model.response.receivable.CustomerOffsettingResponse;
import bg.energo.phoenix.model.response.receivable.deposit.*;
import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.ReceivableTemplateResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityPaidByDepositRepository;
import bg.energo.phoenix.repository.receivable.deposit.*;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.receivable.ObjectOffsettingService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityService;
import bg.energo.phoenix.service.receivable.customerReceivables.CustomerReceivableService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {
    private final static String DEPOSIT_PREFIX = "Deposit-";
    private final DepositProductContractsService depositProductContractsService;
    private final DepositServiceContractService depositServiceContractService;
    private final DepositRepository depositRepository;
    private final CustomerRepository customerRepository;
    private final CurrencyRepository currencyRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final CalendarRepository calendarRepository;
    private final DepositPaymentDeadlineAfterWithdrawalRepository depositPaymentDeadlineAfterWithdrawalRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final DepositProductContractRepository depositProductContractRepository;
    private final DepositServiceContractRepository depositServiceContractRepository;
    private final PermissionService permissionService;
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final CustomerLiabilityService customerLiabilityService;
    private final CustomerReceivableService customerReceivableService;
    private final ContractTemplateRepository contractTemplateRepository;
    private final DepositTemplateRepository depositTemplateRepository;
    private final DepositDocumentFileRepository depositDocumentFileRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final FileService fileService;
    private final CustomerLiabilityPaidByDepositRepository customerLiabilityPaidByDepositRepository;
    private final DocumentsRepository documentsRepository;
    private final ObjectOffsettingService objectOffsettingService;

    /**
     * Creates a new deposit based on the provided request.
     *
     * @param request The DepositCreateRequest containing the details for the new deposit.
     * @return The ID of the newly created deposit.
     * @throws RuntimeException if there are validation errors during the creation process.
     */
    @Transactional
    public Long create(DepositCreateRequest request) {
        log.debug("creating deposit with request: {};", request);
        List<String> errorMessages = new ArrayList<>();

        if (request.getRefundDate() != null) {
            errorMessages.add("refundDate-[refundDate] is disabled;");
        }
        if (request.getCurrentAmount() != null) {
            errorMessages.add("currentAmount-[currentAmount] is disabled;");
        }
        Deposit deposit = depositMapper(request);
        validateCurrency(request, deposit, errorMessages);
        validateCustomer(request, deposit, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        depositRepository.saveAndFlush(deposit);
        deposit.setDepositNumber(DEPOSIT_PREFIX + deposit.getId());
        DepositPaymentDeadlineAfterWithdrawal withdrawal = fromRequest(deposit.getId(), request.getPaymentDeadlineAfterWithdrawalRequest(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        depositPaymentDeadlineAfterWithdrawalRepository.save(withdrawal);
        depositRepository.saveAndFlush(deposit);

        if (request.getDepositContractRequest() != null) {
            validateContracts(request, deposit.getId(), errorMessages);
        }
        saveTemplates(request.getTemplateIds(), deposit.getId(), errorMessages);
        customerLiabilityService.createLiabilityFromDeposit(deposit.getId(), request.getInitialAmount(), errorMessages, false, null);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return deposit.getId();
    }

    /**
     * Deletes a deposit with the specified ID.
     *
     * @param id The ID of the deposit to be deleted.
     * @return The ID of the deleted deposit.
     * @throws DomainEntityNotFoundException if the deposit is not found.
     */
    @Transactional
    public Long delete(Long id) {
        log.info("Deleting Customer Deposit with id: %s".formatted(id));

        Deposit deposit = depositRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Customer Deposit with id: %s;".formatted(id)));

        //TODO: This not works as it should work
//        DepositPaymentDeadlineAfterWithdrawal withdrawal = depositPaymentDeadlineAfterWithdrawalRepository.findByDepositIdAndStatusIn(deposit.getId(), List.of(EntityStatus.ACTIVE))
//                .orElseThrow(() -> new DomainEntityNotFoundException("can't find payment after withdrawal with deposit id: %s;".formatted(id)));
//        withdrawal.setStatus(EntityStatus.DELETED);
//        depositPaymentDeadlineAfterWithdrawalRepository.save(withdrawal);

        boolean canDeleteDeposit = depositRepository.isDepositUsedInOffsetting(id);
        if (canDeleteDeposit) {
            throw new OperationNotAllowedException("Can't delete deposit because it's liability is used in offsetting");
        }
        List<String> errorMessages = new ArrayList<>();
        if (deposit.getInitialAmount().compareTo(BigDecimal.ZERO) != 0) {
            customerReceivableService.createFromDeposit(deposit, errorMessages, deposit.getInitialAmount());
        }
        deposit.setStatus(EntityStatus.DELETED);
        depositRepository.save(deposit);

        return deposit.getId();
    }

    /**
     * Retrieves a detailed view of a deposit with the specified ID.
     *
     * @param depositId The ID of the deposit to view.
     * @return A DepositResponse object containing the deposit details.
     * @throws DomainEntityNotFoundException if the deposit is not found.
     * @throws ClientException               if the user doesn't have permission to view the deposit.
     */
    @Transactional(readOnly = true)
    public DepositResponse view(Long depositId) {
        log.info("Previewing Customer Deposit with id: %s".formatted(depositId));

        List<EntityStatus> depositStatuses = new ArrayList<>();
        if (hasViewPermission()) {
            depositStatuses.add(EntityStatus.ACTIVE);
        }
        if (hasDeletedPermission()) {
            depositStatuses.add(EntityStatus.DELETED);
        }
        Deposit deposit = depositRepository
                .findByIdAndStatusIn(depositId, depositStatuses)
                .orElseThrow(
                        () -> new DomainEntityNotFoundException("id-Deposit not found!;")
                );

        if (deposit.getStatus().equals(EntityStatus.DELETED)) {
            if (!hasDeletedPermission()) {
                throw new ClientException("You do not have access to view deleted Deposit", ErrorCode.ACCESS_DENIED);
            }
        }

        DepositResponse depositResponse = new DepositResponse(deposit);

        List<DepositCustomerResponse> depositCustomerResponses = customerDetailsRepository.findCustomerDetailsForDeposit(deposit.getCustomerId());
        depositResponse.setCustomerResponse(depositCustomerResponses.get(0));

        Currency currency = currencyRepository.findCurrencyByIdAndStatuses(deposit.getCurrencyId(), List.of(ACTIVE, INACTIVE)).orElseThrow(
                () -> new DomainEntityNotFoundException("id-Currency not found;")
        );

        CurrencyShortResponse currencyResponse = new CurrencyShortResponse(currency);
        depositResponse.setCurrencyShortResponse(currencyResponse);


        DepositPaymentDeadlineAfterWithdrawal withdrawal = depositPaymentDeadlineAfterWithdrawalRepository
                .findByDepositIdAndStatusIn(deposit.getId(), List.of(EntityStatus.ACTIVE, EntityStatus.DELETED)).orElseThrow(
                        () -> new DomainEntityNotFoundException("id-paymentDeadlineAfterWithdrawal not found")
                );
        Calendar calendar = calendarRepository.getReferenceById(withdrawal.getCalendarId());

        DepositPaymentDdlAftWithdrawalResponse withdrawalResponse = new DepositPaymentDdlAftWithdrawalResponse(withdrawal, calendar);
        depositResponse.setWithdrawalResponse(withdrawalResponse);

        List<DepositProductContract> depositProductContracts = depositProductContractRepository
                .findDepositProductContractByDepositIdAndStatus(deposit.getId(), List.of(EntityStatus.ACTIVE));
        if (!depositProductContracts.isEmpty()) {
            List<DepositProductContractResponse> depositProductContractResponseList = new ArrayList<>();
            for (DepositProductContract productContract : depositProductContracts) {
                ProductContract contract = productContractRepository.findById(productContract.getContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("cant find product contract with id: %s".formatted(productContract.getContractId())));
                DepositProductContractResponse depositProductContractResponse = new DepositProductContractResponse(productContract, contract.getContractNumber());
                depositProductContractResponseList.add(depositProductContractResponse);
            }
            depositResponse.setDepositProductContractResponse(depositProductContractResponseList);
        }

        List<DepositServiceContract> depositServiceContracts = depositServiceContractRepository
                .findDepositServiceContractByContractIdAndStatus(deposit.getId(), List.of(EntityStatus.ACTIVE));
        if (!depositServiceContracts.isEmpty()) {
            List<DepositServiceContractResponse> depositServiceContractResponseList = new ArrayList<>();
            for (DepositServiceContract serviceContract : depositServiceContracts) {
                ServiceContracts serviceContracts = serviceContractsRepository.findById(serviceContract.getContractId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("can't find service contract with id: %s".formatted(serviceContract.getContractId())));
                DepositServiceContractResponse depositServiceContractResponse = new DepositServiceContractResponse(serviceContract, serviceContracts.getContractNumber());
                depositServiceContractResponseList.add(depositServiceContractResponse);
            }
            depositResponse.setDepositServiceContractResponse(depositServiceContractResponseList);
        }

        depositResponse.setTemplateResponses(getTemplateResponse(deposit.getId()));
        calculateAndSetAmountsInOtherCurrency(currency, depositResponse);

        depositResponse.setCustomerDepositOffsettingResponseList(getOffsettingResponseList(depositId));
        depositResponse.setCanDelete(!depositRepository.isDepositUsedInOffsetting(deposit.getId()));

        List<FileWithStatusesResponse> files = documentsRepository.findDocumentsForDeposit(List.of(deposit.getId()))
                .stream().map(file -> new FileWithStatusesResponse(file, accountManagerRepository.findByUserName(file.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""))).toList();
        depositResponse.setFiles(files);

        return depositResponse;
    }

    /**
     * Retrieves a list of CustomerOffsettingResponse for the provided deposit ID.
     *
     * @param depositId the ID of the deposit for which offsetting responses are to be fetched
     * @return a list of CustomerOffsettingResponse objects representing the offsetting details for the given deposit ID
     */
    private List<CustomerOffsettingResponse> getOffsettingResponseList(Long depositId) {
        return objectOffsettingService
                .fetchObjectOffsettings(ObjectOffsettingType.DEPOSIT, depositId)
                .stream()
                .map(CustomerOffsettingResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Lists deposits based on the provided criteria.
     *
     * @param request The DepositListingRequest containing filter and pagination parameters.
     * @return A Page of DepositListingResponse objects.
     */
    public Page<DepositListingResponse> list(DepositListingRequest request) {
        List<EntityStatus> depositStatuses = new ArrayList<>();
        if (hasDeletedPermission()) {
            depositStatuses.add(EntityStatus.DELETED);
        }
        if (hasViewPermission()) {
            depositStatuses.add(EntityStatus.ACTIVE);
        }
        return depositRepository
                .filter(
                        ListUtils.emptyIfNull(request.getCurrencyIds()),
                        request.getPaymentDeadlineFrom(),
                        request.getPaymentDeadlineTo(),
                        request.getInitialAmountFrom(),
                        request.getInitialAmountTo(),
                        request.getCurrentAmountFrom(),
                        request.getCurrentAmountTo(),
                        Objects.requireNonNullElse(request.getDirection(), Sort.Direction.ASC).name(),
                        Objects.requireNonNullElse(request.getDepositListingType(), DepositListingType.ALL).name(),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(depositStatuses),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), getSearchByEnum(request.getSortBy()))
                                )
                        )
                )
                .map(DepositListingResponse::new);
    }

    /**
     * Retrieves the sorting field based on the provided DepositListingSortingType.
     *
     * @param sortBy The DepositListingSortingType enum value.
     * @return The corresponding sorting field as a String.
     */
    private String getSearchByEnum(DepositListingSortingType sortBy) {
        return sortBy != null ? sortBy.getValue() : DepositListingSortingType.ID.getValue();
    }

    private boolean hasDeletedPermission() {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.DEPOSIT, List.of(PermissionEnum.DEPOSIT_VIEW_DELETE));
    }

    private boolean hasViewPermission() {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.DEPOSIT, List.of(PermissionEnum.DEPOSIT_VIEW));
    }

    private Deposit depositMapper(DepositCreateRequest request) {
        Deposit deposit = new Deposit();
        deposit.setPaymentDeadline(request.getPaymentDeadline());
        deposit.setDepositNumber(DEPOSIT_PREFIX);
        deposit.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount()));
        deposit.setCurrentAmount(BigDecimal.ZERO);
        deposit.setIncomeAccountNumber(request.getNumberOfIncomeAccount());
        deposit.setCostCenter(request.getCostCentre());
        deposit.setStatus(EntityStatus.ACTIVE);
        deposit.setCreateDate(LocalDateTime.now());
        deposit.setModifyDate(LocalDateTime.now());

        return deposit;
    }

    /**
     * Validates and processes contracts associated with a deposit creation request.
     *
     * @param request       The DepositCreateRequest containing contract information.
     * @param depositId     The ID of the deposit being created or updated.
     * @param errorMessages A list to collect error messages for invalid contracts.
     */
    private void validateContracts(DepositCreateRequest request, Long depositId, List<String> errorMessages) {
        Set<Long> serviceContractIds = request.getDepositContractRequest().stream().filter(y -> y.getContractType().equals(ContractType.SERVICE_CONTRACT)).map(DepositContractRequest::getContractId).collect(Collectors.toSet());
        Set<Long> productContractIds = request.getDepositContractRequest().stream().filter(y -> y.getContractType().equals(ContractType.PRODUCT_CONTRACT)).map(DepositContractRequest::getContractId).collect(Collectors.toSet());

        Set<Long> productContractFetched = productContractDetailsRepository.checkForIds(productContractIds, request.getCustomerId(), ProductContractStatus.ACTIVE);
        Set<Long> serviceContractFetched = serviceContractDetailsRepository.checkForIds(serviceContractIds, request.getCustomerId(), EntityStatus.ACTIVE);

        List<DepositServiceContract> depositServiceContracts = serviceContractIds.stream().filter(contract -> {
            boolean contains = serviceContractFetched.contains(contract);
            if (!contains) {
                errorMessages.add(String.format("depositCreateRequest.depositContractRequest.contractId[%s]-Unable to find service contract;", contract));
            }
            return contains;
        }).map(contract -> depositServiceContractService.depositServiceContractMapper(depositId, contract)).toList();

        List<DepositProductContract> depositProductContracts = productContractIds.stream().filter(contract -> {
            boolean contains = productContractFetched.contains(contract);
            if (!contains) {
                errorMessages.add(String.format("depositCreateRequest.depositContractRequest.contractId[%s]-Unable to find product contract;", contract));
            }
            return contains;
        }).map(contract -> depositProductContractsService.depositProductContractMapper(depositId, contract)).toList();

        depositProductContractRepository.saveAll(depositProductContracts);
        depositServiceContractRepository.saveAll(depositServiceContracts);
    }

    /**
     * Validates the currency for a deposit creation request.
     *
     * @param request       The DepositCreateRequest containing the currency ID to validate.
     * @param deposit       The Deposit entity to update with the currency ID if valid.
     * @param errorMessages A list to collect error messages if validation fails.
     */
    private void validateCurrency(DepositCreateRequest request, Deposit deposit, List<String> errorMessages) {
        if (!currencyRepository.existsByIdAndStatus(request.getCurrencyId(), NomenclatureItemStatus.ACTIVE)) {
            errorMessages.add(String.format("depositCreateRequest.depositContractRequest.currencyId[%s]-Unable to find currency;", request.getCurrencyId()));
        } else {
            deposit.setCurrencyId(request.getCurrencyId());
        }
    }

    /**
     * Validates the customer for a deposit creation request.
     *
     * @param request       The DepositCreateRequest containing the customer ID to validate.
     * @param deposit       The Deposit entity to update with the customer ID if valid.
     * @param errorMessages A list to collect error messages if validation fails.
     */
    private void validateCustomer(DepositCreateRequest request, Deposit deposit, List<String> errorMessages) {
        if (!customerRepository.existsByIdAndStatusIn(request.getCustomerId(), List.of(CustomerStatus.ACTIVE))) {
            errorMessages.add(String.format("depositCreateRequest.depositContractRequest.customerId[%s]-Unable to find customer;", request.getCustomerId()));
        } else {
            deposit.setCustomerId(request.getCustomerId());
        }
    }

    /**
     * Validates the calendar for a payment deadline after withdrawal request.
     *
     * @param request                 The PaymentDeadlineAfterWithdrawalRequest containing the calendar ID to validate.
     * @param errorMessages           A list to collect error messages if validation fails.
     * @param deadlineAfterWithdrawal The DepositPaymentDeadlineAfterWithdrawal entity to update if valid.
     */
    private void validateCalendar(PaymentDeadlineAfterWithdrawalRequest request, List<String> errorMessages, DepositPaymentDeadlineAfterWithdrawal deadlineAfterWithdrawal) {
        if (!calendarRepository.existsByIdAndStatus(request.getCalendarId(), ACTIVE)) {
            log.error("Error while fetching calendar with id: {}", request.getCalendarId());
            errorMessages.add(String.format("depositCreateRequest.paymentDeadlineAfterWithdrawalRequest.calendarId[%s]-Unable to find calendar;", request.getCalendarId()));
        } else {
            deadlineAfterWithdrawal.setCalendarId(request.getCalendarId());
        }
    }

    /**
     * Validates the calendar during the edit process of a DepositPaymentDeadlineAfterWithdrawal.
     *
     * @param depositPaymentTerm The DepositPaymentDeadlineAfterWithdrawal being edited.
     * @param request            The PaymentDeadlineAfterWithdrawalRequest containing the edit details.
     * @throws DomainEntityNotFoundException if the specified calendar cannot be found.
     * @throws OperationNotAllowedException  if an attempt is made to assign a new INACTIVE calendar.
     */
    private void validateCalendarOnEdit(DepositPaymentDeadlineAfterWithdrawal depositPaymentTerm, PaymentDeadlineAfterWithdrawalRequest request) {
        Optional<Calendar> calendarOptional = calendarRepository.findByIdAndStatusIsIn(request.getCalendarId(), List.of(ACTIVE, INACTIVE));
        if (calendarOptional.isEmpty()) {
            log.error("Error while fetching calendar with id: {}", request.getCalendarId());
            throw new DomainEntityNotFoundException("paymentDeadlineAfterWithdrawalRequest.calendarId-Unable to find calendar with Id : %s;"
                    .formatted(depositPaymentTerm.getCalendarId()));
        } else {
            Calendar calendar = calendarOptional.get();
            if (calendar.getStatus().equals(INACTIVE)) {
                if (!Objects.equals(depositPaymentTerm.getCalendarId(), request.getCalendarId())) {
                    throw new OperationNotAllowedException("paymentDeadlineAfterWithdrawalRequest.calendarId-Unable to assign INACTIVE calendar with Id : %s;"
                            .formatted(request.getCalendarId()));
                }
            }
        }
    }

    /**
     * Creates a DepositPaymentDeadlineAfterWithdrawal entity from a request.
     *
     * @param depositId     The ID of the deposit associated with this payment deadline.
     * @param request       The PaymentDeadlineAfterWithdrawalRequest containing the details for the new entity.
     * @param errorMessages A list to store any error messages that may occur during the creation process.
     * @return A new DepositPaymentDeadlineAfterWithdrawal entity populated with data from the request.
     */
    private DepositPaymentDeadlineAfterWithdrawal fromRequest(Long depositId, PaymentDeadlineAfterWithdrawalRequest request, List<String> errorMessages) {
        Set<DepositPaymentDeadlineExclude> depositPaymentDeadlineExcludes = getDepositPaymentExcludes(request);
        DepositPaymentDeadlineAfterWithdrawal deadlineAfterWithdrawal = new DepositPaymentDeadlineAfterWithdrawal();

        if (request.getDueDateChange() != null) {
            deadlineAfterWithdrawal.setDueDateChange(List.of(request.getDueDateChange()));
        }
        deadlineAfterWithdrawal.setCalendarType(request.getCalendarType());
        deadlineAfterWithdrawal.setValue(request.getValue());
        validateCalendar(request, errorMessages, deadlineAfterWithdrawal);
        deadlineAfterWithdrawal.setDepositPaymentDeadlineExcludes(depositPaymentDeadlineExcludes.stream().toList());
        deadlineAfterWithdrawal.setDepositId(depositId);
        deadlineAfterWithdrawal.setName(request.getName());
        deadlineAfterWithdrawal.setStatus(EntityStatus.ACTIVE);

        return deadlineAfterWithdrawal;
    }

    /**
     * Determines the set of DepositPaymentDeadlineExclude options based on the provided request.
     *
     * @param request The PaymentDeadlineAfterWithdrawalRequest containing exclusion preferences.
     * @return A Set of DepositPaymentDeadlineExclude enums representing the chosen exclusions.
     */
    private Set<DepositPaymentDeadlineExclude> getDepositPaymentExcludes(PaymentDeadlineAfterWithdrawalRequest request) {
        Set<DepositPaymentDeadlineExclude> depositPaymentDeadlineExclude = new HashSet<>();
        if (BooleanUtils.isTrue(request.getExcludeHolidays())) {
            depositPaymentDeadlineExclude.add(DepositPaymentDeadlineExclude.HOLIDAYS);
        }
        if (BooleanUtils.isTrue(request.getExcludeWeekends())) {
            depositPaymentDeadlineExclude.add(DepositPaymentDeadlineExclude.WEEKENDS);
        }
        return depositPaymentDeadlineExclude;
    }

    /**
     * Updates an existing deposit with the provided information.
     *
     * @param id      The ID of the deposit to update.
     * @param request The DepositCreateRequest containing the updated deposit information.
     * @return The ID of the updated deposit.
     * @throws DomainEntityNotFoundException if the deposit is not found.
     * @throws RuntimeException              if there are validation errors during the update process.
     */
    @Transactional
    public Long update(Long id, DepositCreateRequest request) {
        log.info("Updating customer deposit with id: {}", id);
        List<String> errorMessages = new ArrayList<>();

        Deposit deposit = depositRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Could not find deposit with that id: %s;".formatted(id)));
        if (!deposit.getInitialAmount().equals(request.getInitialAmount()) && customerLiabilityPaidByDepositRepository.existsByCustomerDepositIdAndStatus(deposit.getId(), EntityStatus.ACTIVE)) {
            throw new OperationNotAllowedException("Can't edit initial amount of deposit because deposit has participated in offsetting");
        }
        BigDecimal depositInitialAmount = deposit.getInitialAmount();

        if (!Objects.equals(request.getCustomerId(), deposit.getCustomerId())) {
            errorMessages.add("customer-[Customer] is disabled;");
        }
        if (request.getCurrentAmount() != null) {
            if (request.getCurrentAmount().compareTo(deposit.getCurrentAmount()) != 0) {
                errorMessages.add("currentAmount-[currentAmount] is disabled;");
            }
        } else {
            errorMessages.add("currentAmount-[currentAmount] is disabled;");
        }

        deposit.setIncomeAccountNumber(request.getNumberOfIncomeAccount());
        deposit.setCostCenter(request.getCostCentre());

        if (!deposit.getCurrencyId().equals(request.getCurrencyId())) {
            validateCurrency(request, deposit, errorMessages);
        }

        //TODO paymentDeadline, initial amount, currency after adding offsetting objects
        deposit.setInitialAmount(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount()));
        if (request.getRefundDate() != null) {
            if (request.getRefundDate().isBefore(request.getPaymentDeadline())) {
                errorMessages.add("refundDate-[refundDate] can not be early that paymentDeadline;");
            } else {
                deposit.setRefundDate(request.getRefundDate());
            }
        } else {
            deposit.setRefundDate(request.getRefundDate()); //should be null
        }
        deposit.setPaymentDeadline(request.getPaymentDeadline());

        DepositPaymentDeadlineAfterWithdrawal withdrawal = depositPaymentDeadlineAfterWithdrawalRepository
                .findByDepositIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Could not find payment deadline after withdrawal with given id"));
        depositPaymentDeadlineAfterWithdrawalRepository.save(editPaymentDeadline(withdrawal, request, errorMessages));

        List<DepositProductContract> productContracts = depositProductContractRepository
                .findDepositProductContractByDepositIdAndStatus(id, List.of(EntityStatus.ACTIVE));
        List<DepositServiceContract> serviceContracts = depositServiceContractRepository
                .findDepositServiceContractByContractIdAndStatus(id, List.of(EntityStatus.ACTIVE));

        List<Long> productContractIds = productContracts.stream().map(DepositProductContract::getContractId).collect(Collectors.toList()); //remove
        List<Long> serviceContractIds = serviceContracts.stream().map(DepositServiceContract::getContractId).collect(Collectors.toList());


        if (request.getDepositContractRequest() != null) {
            List<Long> productContractRequestIds = request.getDepositContractRequest().stream().filter(contract -> contract.getContractType().equals(ContractType.PRODUCT_CONTRACT)).map(DepositContractRequest::getContractId).toList();
            List<Long> serviceContractRequestIds = request.getDepositContractRequest().stream().filter(contract -> contract.getContractType().equals(ContractType.SERVICE_CONTRACT)).map(DepositContractRequest::getContractId).toList();


            List<Long> updatedProductContractIds = getUpdatedIds(productContractRequestIds, productContractIds); //add
            List<Long> updatedServiceContractIds = getUpdatedIds(serviceContractRequestIds, serviceContractIds);


            productContractIds.removeAll(updatedProductContractIds);
            serviceContractIds.removeAll(updatedServiceContractIds);


            updatedProductContractIds.removeAll(productContractIds);
            updatedServiceContractIds.removeAll(serviceContractIds);


            if (!productContractIds.isEmpty()) {
                deleteProductContracts(productContractIds, deposit.getId(), errorMessages);
            }
            if (!serviceContractIds.isEmpty()) {
                deleteServiceContracts(serviceContractIds, deposit.getId(), errorMessages);
            }

            if (!updatedProductContractIds.isEmpty()) {
                saveProductContracts(updatedProductContractIds, deposit.getCustomerId(), errorMessages, deposit.getId());
            }
            if (!updatedServiceContractIds.isEmpty()) {
                saveServiceContracts(updatedServiceContractIds, deposit.getCustomerId(), errorMessages, deposit.getId());
            }

        } else {
            if (!productContractIds.isEmpty()) {
                deleteProductContracts(productContractIds, deposit.getId(), errorMessages);
            }
            if (!serviceContractIds.isEmpty()) {
                deleteServiceContracts(serviceContractIds, deposit.getId(), errorMessages);
            }

        }

        depositRepository.save(deposit);
        updateTemplates(request.getTemplateIds(), deposit.getId(), errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        int comparison = depositInitialAmount.compareTo(EPBDecimalUtils.roundToTwoDecimalPlaces(request.getInitialAmount()));
        if (comparison > 0) {
            customerReceivableService.createFromDeposit(deposit, errorMessages, depositInitialAmount.subtract(request.getInitialAmount()));
        } else if (comparison < 0) {
            customerLiabilityService.createLiabilityFromDeposit(deposit.getId(), request.getInitialAmount().subtract(depositInitialAmount), errorMessages, false, null);
        }
        depositRepository.save(deposit);
        return deposit.getId();
    }

    /**
     * Deletes product contracts associated with a deposit.
     *
     * @param productContractIds A list of product contract IDs to delete.
     * @param depositId          The ID of the deposit associated with these contracts.
     * @param errorMessages      A list to store any error messages.
     * @throws DomainEntityNotFoundException if a DepositProductContract is not found.
     */
    public void deleteProductContracts(List<Long> productContractIds, Long depositId, List<String> errorMessages) {
        for (Long id : productContractIds) {
            DepositProductContract depositProductContract = depositProductContractRepository.findByContractIdAndDepositIdAndStatus(id, depositId, EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("can't find DepositProductContract with contract id: "));
            depositProductContract.setStatus(EntityStatus.DELETED);
            depositProductContractRepository.save(depositProductContract);
        }
    }

    /**
     * Deletes service contracts associated with a deposit.
     *
     * @param serviceContractIds A list of service contract IDs to delete.
     * @param depositId          The ID of the deposit associated with these contracts.
     * @param errorMessages      A list to store any error messages.
     * @throws DomainEntityNotFoundException if a DepositServiceContract is not found.
     */
    public void deleteServiceContracts(List<Long> serviceContractIds, Long depositId, List<String> errorMessages) {
        for (Long id : serviceContractIds) {
            DepositServiceContract depositServiceContract = depositServiceContractRepository.findByContractIdAndDepositIdAndStatus(id, depositId, EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("can't find DepositServiceContract with contract id: "));
            depositServiceContract.setStatus(EntityStatus.DELETED);
            depositServiceContractRepository.save(depositServiceContract);
        }
    }

    /**
     * Saves product contracts associated with a deposit.
     *
     * @param updatedProductContractIds A list of product contract IDs to save.
     * @param customerId                The ID of the customer associated with these contracts.
     * @param errorMessages             A list to store error messages for contracts that cannot be found or saved.
     * @param depositId                 The ID of the deposit associated with these contracts.
     */
    public void saveProductContracts(List<Long> updatedProductContractIds, Long customerId, List<String> errorMessages, Long depositId) {
        for (Long id : updatedProductContractIds) {
            if (!productContractDetailsRepository.existsByCustomerIdAndContractId(customerId, id, ProductContractStatus.ACTIVE)) {
                errorMessages.add(String.format("depositCreateRequest.depositContractRequest.contractId[%s]-Unable to find product contract;", id));
            } else {
                depositProductContractsService.saveDepositProductContract(depositProductContractsService.depositProductContractMapper(depositId, id));
            }
        }
    }

    /**
     * Saves service contracts associated with a deposit.
     *
     * @param updatedServiceContractIds A list of service contract IDs to save.
     * @param customerId                The ID of the customer associated with these contracts.
     * @param errorMessages             A list to store error messages for contracts that cannot be found or saved.
     * @param depositId                 The ID of the deposit associated with these contracts.
     */
    public void saveServiceContracts(List<Long> updatedServiceContractIds, Long customerId, List<String> errorMessages, Long depositId) {
        for (Long id : updatedServiceContractIds) {
            if (!serviceContractDetailsRepository.existsByCustomerIdAndContractId(id, customerId, EntityStatus.ACTIVE)) {
                errorMessages.add(String.format("depositCreateRequest.depositContractRequest.contractId[%s]-Unable to find service contract;", id));
            } else {
                depositServiceContractService.saveDepositServiceContract(depositServiceContractService.depositServiceContractMapper(depositId, id));
            }
        }
    }

    /**
     * Identifies new IDs present in the request but not in the existing list.
     *
     * @param requestIds A list of IDs from the request.
     * @param ids        A list of existing IDs. This list is modified during the process.
     * @return A List of new IDs that were present in requestIds but not in ids.
     */
    public List<Long> getUpdatedIds(List<Long> requestIds, List<Long> ids) { //added new ids from request
        List<Long> updatedIds = new ArrayList<>();

        for (Long id : requestIds) {
            if (!ids.contains(id)) {
                updatedIds.add(id);
            } else {
                ids.remove(id);
            }
        }
        return updatedIds;
    }

    /**
     * Edits a DepositPaymentDeadlineAfterWithdrawal entity based on the provided request.
     *
     * @param withdrawal    The DepositPaymentDeadlineAfterWithdrawal entity to be edited.
     * @param request       The DepositCreateRequest containing the new values for the withdrawal entity.
     * @param errorMessages A list to store any error messages that may occur during the editing process.
     * @return The updated DepositPaymentDeadlineAfterWithdrawal entity.
     */
    private DepositPaymentDeadlineAfterWithdrawal editPaymentDeadline(DepositPaymentDeadlineAfterWithdrawal withdrawal, DepositCreateRequest request, List<String> errorMessages) {
        Set<DepositPaymentDeadlineExclude> depositPaymentDeadlineExcludes = getDepositPaymentExcludes(request.getPaymentDeadlineAfterWithdrawalRequest());
        PaymentDeadlineAfterWithdrawalRequest withdrawalRequest = request.getPaymentDeadlineAfterWithdrawalRequest();
        if (withdrawalRequest.getDueDateChange() != null) {
            withdrawal.setDueDateChange(List.of(withdrawalRequest.getDueDateChange()));
        } else {
            withdrawal.setDueDateChange(null);
        }
        withdrawal.setCalendarType(withdrawalRequest.getCalendarType());
        withdrawal.setValue(withdrawalRequest.getValue());
        validateCalendarOnEdit(withdrawal, request.getPaymentDeadlineAfterWithdrawalRequest());
        withdrawal.setCalendarId(request.getPaymentDeadlineAfterWithdrawalRequest().getCalendarId());
        withdrawal.setDepositPaymentDeadlineExcludes(depositPaymentDeadlineExcludes.stream().toList());
        withdrawal.setName(withdrawal.getName());
        return withdrawal;
    }

    /**
     * Saves templates associated with a deposit.
     *
     * @param templateRequests A set of ReceivableTemplateRequest objects to be saved.
     * @param depositId        The ID of the deposit associated with these templates.
     * @param errorMessages    A list to store any error messages that occur during the process.
     */
    public void saveTemplates(Set<ReceivableTemplateRequest> templateRequests, Long depositId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateRequests)) {
            return;
        }
        Map<ReceivableTemplateType, List<Long>> requestMap = new HashMap<>();

        for (ReceivableTemplateRequest templateRequest : templateRequests) {
            if (!requestMap.containsKey(templateRequest.getTemplateType())) {
                List<Long> value = new ArrayList<>();
                value.add(templateRequest.getTemplateId());
                requestMap.put(templateRequest.getTemplateType(), value);
            } else {
                requestMap.get(templateRequest.getTemplateType()).add(templateRequest.getTemplateId());
            }
        }

        List<DepositTemplate> productContractTemplates = new ArrayList<>();
        createNewProductTemplates(depositId, errorMessages, requestMap, productContractTemplates);
        if (!errorMessages.isEmpty()) {
            return;
        }
        depositTemplateRepository.saveAll(productContractTemplates);
    }

    /**
     * Creates new product templates for a given MLO based on the provided request map.
     *
     * @param mloId         The ID of the MLO for which templates are being created.
     * @param errorMessages A list to store error messages for invalid templates.
     * @param requestMap    A map of ReceivableTemplateType to a list of template IDs to be processed.
     * @param mloTemplates  A list to store the newly created DepositTemplate entities.
     */
    private void createNewProductTemplates(Long mloId, List<String> errorMessages, Map<ReceivableTemplateType, List<Long>> requestMap, List<DepositTemplate> mloTemplates) {
        AtomicInteger i = new AtomicInteger(0);
        requestMap.forEach((key, value) -> {
            Set<Long> allIdByIdAndLanguages = contractTemplateRepository.findAllIdByIdAndLanguages(value, ContractTemplatePurposes.DEPOSIT, List.of(ContractTemplateLanguage.BILINGUAL, ContractTemplateLanguage.BULGARIAN), List.of(key.getTemplateType()), ContractTemplateStatus.ACTIVE, LocalDate.now());
            for (Long l : value) {
                if (!allIdByIdAndLanguages.contains(l)) {
                    errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i.getAndIncrement(), l));
                    continue;
                }
                mloTemplates.add(new DepositTemplate(l, mloId, key));
            }
        });
    }

    /**
     * Updates the templates associated with a product detail.
     *
     * @param templateIds     A set of ReceivableTemplateRequest objects representing the desired template configuration.
     * @param productDetailId The ID of the product detail for which templates are being updated.
     * @param errorMessages   A list to store error messages that may occur during the update process.
     */
    public void updateTemplates(Set<ReceivableTemplateRequest> templateIds, Long productDetailId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Map<ReceivableTemplateType, List<Long>> templatesToCreate = new HashMap<>();
        Map<Long, DepositTemplate> templateMap = depositTemplateRepository.findByDepositId(productDetailId).stream().collect(Collectors.toMap(DepositTemplate::getTemplateId, j -> j));
        List<DepositTemplate> productContractTemplates = new ArrayList<>();
        for (ReceivableTemplateRequest templateRequest : templateIds) {
            DepositTemplate remove = templateMap.remove(templateRequest.getTemplateId());
            if (remove == null) {
                if (!templatesToCreate.containsKey(templateRequest.getTemplateType())) {
                    List<Long> value = new ArrayList<>();
                    value.add(templateRequest.getTemplateId());
                    templatesToCreate.put(templateRequest.getTemplateType(), value);
                } else {
                    templatesToCreate.get(templateRequest.getTemplateType()).add(templateRequest.getTemplateId());
                }
            }
        }
        createNewProductTemplates(productDetailId, errorMessages, templatesToCreate, productContractTemplates);
        Collection<DepositTemplate> values = templateMap.values();
        values.forEach(x -> {
            x.setStatus(EntityStatus.DELETED);
            productContractTemplates.add(x);
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        depositTemplateRepository.saveAll(productContractTemplates);
    }

    /**
     * Retrieves a list of ReceivableTemplateResponse objects for a given MLO ID.
     *
     * @param mloId The ID of the MLO for which template responses are being retrieved.
     * @return A List of ReceivableTemplateResponse objects containing template information for the specified MLO.
     */
    private List<ReceivableTemplateResponse> getTemplateResponse(Long mloId) {
        return depositTemplateRepository.findForContract(mloId, LocalDate.now());
    }

    /**
     * Calculates and sets the initial and current amounts in an alternative currency for a deposit.
     *
     * @param currency        The Currency object containing the exchange rate for the alternative currency.
     * @param depositResponse The DepositResponse object to be updated with the calculated amounts.
     */
    private void calculateAndSetAmountsInOtherCurrency(Currency currency, DepositResponse depositResponse) {
        depositResponse.setInitialAmountInOtherCurrency(depositResponse.getInitialAmount().multiply(currency.getAltCurrencyExchangeRate()));
        depositResponse.setCurrentAmountInOtherCurrency(depositResponse.getCurrentAmount().multiply(currency.getAltCurrencyExchangeRate()));
    }

    /**
     * Downloads a file from the file service based on the provided ID.
     *
     * @param id The ID of the file to be downloaded.
     * @return A FileContent object containing the file name and byte array of the downloaded file.
     * @throws DomainEntityNotFoundException if the file with the provided ID is not found.
     */
    public FileContent download(Long id) {
        Document document = documentsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Document with presented id not found;"));

        ByteArrayResource resource = fileService.downloadFile(document.getSignedFileUrl());

        return new FileContent(document.getName(), resource.getByteArray());
    }

}

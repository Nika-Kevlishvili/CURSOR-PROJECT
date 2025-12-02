package bg.energo.phoenix.service.contract.order.goods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.order.goods.*;
import bg.energo.phoenix.model.entity.customer.*;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.nomenclature.product.terms.Calendar;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderBasicParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderCreateRequest;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderEditRequest;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderPaymentTermRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderPaymentTermResponse;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderSubObjectShortResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import bg.energo.phoenix.repository.contract.order.goods.*;
import bg.energo.phoenix.repository.customer.*;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ExternalIntermediaryRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.relatedEntities.RelatedContractsAndOrdersService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsOrderBasicParametersService {
    private final ContractTemplateRepository contractTemplateRepository;
    private final CampaignRepository campaignRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final UnwantedCustomerRepository unwantedCustomerRepository;
    private final InterestRateRepository interestRateRepository;

    private final GoodsOrderProxyRepository goodsOrderProxyRepository;
    private final GoodsOrderProxyFilesRepository goodsOrderProxyFilesRepository;
    private final GoodsOrderProxyManagersRepository goodsOrderProxyManagersRepository;
    private final ManagerRepository managerRepository;
    private final GoodsOrderPaymentTermRepository goodsOrderPaymentTermRepository;
    private final CalendarRepository calendarRepository;

    private final GoodsOrderInternalIntermediaryRepository goodsOrderInternalIntermediaryRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final GoodsOrderExternalIntermediaryRepository goodsOrderExternalIntermediaryRepository;
    private final ExternalIntermediaryRepository externalIntermediaryRepository;
    private final GoodsOrderAssistingEmployeeRepository goodsOrderAssistingEmployeeRepository;

    private final RelatedContractsAndOrdersService relatedContractsAndOrdersService;
    private final BankRepository bankRepository;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final TaskService taskService;
    private final PermissionService permissionService;

    public GoodsOrder createGoodsOrderFromRequest(GoodsOrderCreateRequest request, String orderNumber, List<String> errorMessages) {
        GoodsOrderBasicParametersCreateRequest basicParameters = request.getBasicParameters();
        return GoodsOrder.builder()
                .orderNumber(orderNumber)
                .orderStatus(request.getOrderStatus())
                .directDebit(basicParameters.getDirectDebit())
                .bankId(basicParameters.getBankId())
                .iban(basicParameters.getIban())
                .applicableInterestRateId(checkInterestRate(basicParameters.getInterestRateId()))
                .campaignId(checkCampaign(basicParameters.getCampaignId()))
                .prepaymentTermInCalendarDays(basicParameters.getPaymentTermInCalendarDays())
                .customerDetailId(checkCustomer(basicParameters.getCustomerDetailId()))
                .customerCommunicationIdForBilling(basicParameters.getCustomerCommunicationIdForBilling())
                .status(EntityStatus.ACTIVE)
                .globalVatRate(request.getGoodsParameters().isGlobalVatRate())
                .statusModifyDate(request.getStatusModifyDate())
                .employeeId(getEmployeeOnCreate(errorMessages))
                .noInterestOnOverdueDebts(basicParameters.isNoInterestInOverdueDebts())
                .orderInvoiceStatus(OrderInvoiceStatus.NOT_GENERATED)
                .build();
    }

    /**
     * Gets the ID of the employee who is creating the goods order.
     *
     * @param errorMessages list of error messages to be populated if the employee is not found
     * @return ID of the employee who is creating the goods order
     */
    private Long getEmployeeOnCreate(List<String> errorMessages) {
        String loggedInUserName = permissionService.getLoggedInUserId();
        Optional<AccountManager> employeeOptional = accountManagerRepository.findByUserNameAndStatusIn(loggedInUserName, List.of(Status.ACTIVE));
        if (employeeOptional.isEmpty()) {
            log.error("Unable to find employee with username %s;".formatted(loggedInUserName));
            errorMessages.add("Unable to find employee with username %s;".formatted(loggedInUserName));
            return null;
        }
        return employeeOptional.get().getId();
    }

    private Long checkInterestRate(Long interestRateId) {
        if (interestRateId != null) {
            Optional<InterestRate> interestRateOptional =
                    interestRateRepository.findByIdAndStatusIn(interestRateId, List.of(InterestRateStatus.ACTIVE));
            if (interestRateOptional.isPresent()) {
                return interestRateOptional.get().getId();
            }
        }
        return null;
    }

    private Long checkCustomer(Long customerDetailId) {
        CustomerDetails customerDetails = customerDetailsRepository.findById(customerDetailId)
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("Can't find Customer details with id: %s;".formatted(customerDetailId)));
        Customer customer = customerRepository.findById(customerDetails.getCustomerId())
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("Can't find Customer with id: %s;".formatted(customerDetails.getCustomerId())));
        checkUnwantedCustomer(customer.getIdentifier());
        return customerDetails.getId();
    }

    private void checkUnwantedCustomer(String identifier) {
        Optional<UnwantedCustomer> unwantedCustomerOptional =
                unwantedCustomerRepository.findByIdentifierAndStatusIn(identifier, List.of(UnwantedCustomerStatus.ACTIVE));
        if (unwantedCustomerOptional.isPresent()) {
            UnwantedCustomer unwantedCustomer = unwantedCustomerOptional.get();
            if (unwantedCustomer.getCreateOrderRestriction()) {
                throw new ClientException("Customer with identifier: %s is unwanted;".formatted(identifier), ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }
    }

    private Long checkCampaign(Long campaignId) {
        if (campaignId != null) {
            Campaign campaign = campaignRepository.findByIdAndStatusIn(campaignId, List.of(NomenclatureItemStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Active Campaign with id: %s;".formatted(campaignId)));
            return campaign.getId();
        }
        return null;
    }

    @Transactional
    public void createSubObjects(GoodsOrderCreateRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        log.debug("Creating Goods Order sub objects");
        createProxies(getCustomer(goodsOrder), request, goodsOrder, errorMessages);
        GoodsOrderBasicParametersCreateRequest basicParameters = request.getBasicParameters();
        createPaymentTerms(basicParameters.getPaymentTerm(), goodsOrder, errorMessages);
        createInternalIntermediaries(basicParameters.getInternalIntermediaries(), goodsOrder, errorMessages);
        createExternalIntermediaries(basicParameters.getExternalIntermediaries(), goodsOrder, errorMessages);
        createAssistingEmployees(basicParameters.getAssistingEmployees(), goodsOrder, errorMessages);
        setTemplates(goodsOrder, basicParameters);
        relatedContractsAndOrdersService.createEntityRelations(
                goodsOrder.getId(),
                RelatedEntityType.GOODS_ORDER,
                basicParameters.getRelatedEntities(),
                errorMessages
        );
    }

    private void setTemplates(GoodsOrder goodsOrder, GoodsOrderBasicParametersCreateRequest basicParameters) {
        if (!Objects.equals(goodsOrder.getEmailTemplateId(), basicParameters.getEmailTemplateId())) {
            if (basicParameters.getEmailTemplateId() != null) {
                validate(basicParameters.getEmailTemplateId());
                goodsOrder.setEmailTemplateId(basicParameters.getEmailTemplateId());
            } else {
                goodsOrder.setEmailTemplateId(null);
            }
        }
        if (!Objects.equals(goodsOrder.getInvoiceTemplateId(), basicParameters.getInvoiceTemplateId())) {
            if (basicParameters.getInvoiceTemplateId() != null) {
                validate(basicParameters.getInvoiceTemplateId());
                goodsOrder.setInvoiceTemplateId(basicParameters.getInvoiceTemplateId());
            } else {
                goodsOrder.setInvoiceTemplateId(null);
            }
        }

    }

    private void createProxies(Customer customer, GoodsOrderCreateRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        log.debug("Creating proxies for Goods order: {}", goodsOrder.getId());
        if (CollectionUtils.isEmpty(request.getBasicParameters().getProxy())) {
            return; // it's an optional field
        }

        Optional<Customer> customerOptional = customerRepository
                .findByCustomerDetailIdAndStatusIn(
                        request.getBasicParameters().getCustomerDetailId(),
                        List.of(CustomerStatus.ACTIVE)
                );

        if (customerOptional.isEmpty()) {
            log.error("basicParameters.customerDetailId-Customer not found for customer version with ID {}", request.getBasicParameters().getCustomerDetailId());
            errorMessages.add("basicParameters.customerDetailId-Customer not found for customer version.");
            return;
        }

        validateProxiesForCustomerType(customerOptional.get().getCustomerType(), request.getBasicParameters().getProxy().size(), errorMessages);

        List<ProxyEditRequest> proxies = request.getBasicParameters().getProxy();
        for (int i = 0; i < proxies.size(); i++) {
            ProxyEditRequest goodsOrderProxyAddRequest = proxies.get(i);
            if (!customerOptional.get().getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)
                && CollectionUtils.isEmpty(goodsOrderProxyAddRequest.getManagerIds())) {
                log.error("basicParameters.proxies[%s]-Unable to create proxy without managers for non-private customer.".formatted(i));
                errorMessages.add("basicParameters.proxies[%s]-Unable to create proxy without managers for non-private customer.".formatted(i));
                continue;
            }
            createProxy(getCostomerWithDetails(request.getBasicParameters().getCustomerDetailId()), request.getBasicParameters().getCustomerDetailId(), goodsOrderProxyAddRequest, goodsOrder, errorMessages);
        }
    }

    private void validateProxiesForCustomerType(CustomerType customerType, int size, List<String> errorMessages) {
        if (customerType.equals(CustomerType.PRIVATE_CUSTOMER) && size > 1) {
            log.error("basicParameters.proxies-It is not possible to add more than one proxy for private customer.");
            errorMessages.add("basicParameters.proxies-It is not possible to add more than one proxy for private customer.");
        }
    }


    private Customer getCustomer(GoodsOrder goodsOrder) {
        Optional<CustomerDetails> customerDetails = customerDetailsRepository.findById(goodsOrder.getCustomerDetailId());
        if (customerDetails.isPresent()) {
            Optional<Customer> customer = customerRepository.findByIdAndStatuses(customerDetails.get().getCustomerId(), List.of(CustomerStatus.ACTIVE));
            if (customer.isPresent()) {
                return customer.get();
            }
        }
        return null;
    }

    private Customer getCostomerWithDetails(Long id) {
        Optional<CustomerDetails> customerDetails = customerDetailsRepository.findById(id);
        if (customerDetails.isPresent()) {
            Optional<Customer> customer = customerRepository.findByIdAndStatuses(customerDetails.get().getCustomerId(), List.of(CustomerStatus.ACTIVE));
            if (customer.isPresent()) {
                return customer.get();
            }
        }
        return null;
    }

    @Transactional
    public void updateSubObjects(GoodsOrderEditRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        updateProxies(request, goodsOrder, errorMessages);
        updatePaymentTerms(request.getBasicParameters().getPaymentTerm(), goodsOrder, errorMessages);
        updateInternalIntermediaries(request.getBasicParameters().getInternalIntermediaries(), goodsOrder, errorMessages);
        updateExternalIntermediaries(request.getBasicParameters().getExternalIntermediaries(), goodsOrder, errorMessages);
        updateAssistingEmployees(request.getBasicParameters().getAssistingEmployees(), goodsOrder, errorMessages);
        setTemplates(goodsOrder, request.getBasicParameters());
        relatedContractsAndOrdersService.updateEntityRelations(
                goodsOrder.getId(),
                RelatedEntityType.GOODS_ORDER,
                request.getBasicParameters().getRelatedEntities(),
                errorMessages
        );
    }

    private void updateProxies(GoodsOrderEditRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        List<Long> proxyIdList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getBasicParameters().getProxy())) {
            for (ProxyEditRequest proxies : request.getBasicParameters().getProxy()) {
                if (proxies.getId() == null) {
                    proxyIdList.add(createProxy(getCostomerWithDetails(request.getBasicParameters().getCustomerDetailId()), request.getBasicParameters().getCustomerDetailId(), proxies, goodsOrder, errorMessages));
                } else {
                    proxyIdList.add(updateProxy(getCostomerWithDetails(request.getBasicParameters().getCustomerDetailId()), request.getBasicParameters().getCustomerDetailId(), proxies, goodsOrder, errorMessages));
                }
            }
            if (!CollectionUtils.isEmpty(proxyIdList)) {
                List<GoodsOrderProxies> porxyList = goodsOrderProxyRepository.findByIdNotInAndStatusInAndOrderId(proxyIdList, List.of(EntityStatus.ACTIVE), goodsOrder.getId());
                if (!CollectionUtils.isEmpty(porxyList)) {
                    for (GoodsOrderProxies item : porxyList) {
                        item.setStatus(EntityStatus.DELETED);
                        goodsOrderProxyRepository.save(item);
                    }
                }
            }
        } else {
            List<GoodsOrderProxies> porxyList = goodsOrderProxyRepository.findByOrderIdAndStatusIn(goodsOrder.getId(), List.of(EntityStatus.ACTIVE));
            if (!CollectionUtils.isEmpty(porxyList)) {
                for (GoodsOrderProxies item : porxyList) {
                    item.setStatus(EntityStatus.DELETED);
                    goodsOrderProxyRepository.save(item);
                }
            }
        }

    }


    /**
     * Creates internal intermediaries for the given goods order.
     *
     * @param internalIntermediaries the internal intermediaries to be created
     * @param goodsOrder             the goods order to be updated
     * @param errorMessages          the list of error messages to be populated in case of an error
     */
    private void createInternalIntermediaries(List<Long> internalIntermediaries,
                                              GoodsOrder goodsOrder,
                                              List<String> errorMessages) {
        if (CollectionUtils.isEmpty(internalIntermediaries)) {
            return; // it's an optional field
        }

        log.debug("Creating internal intermediaries for goods order: {}", internalIntermediaries);

        List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), internalIntermediaries);

        List<GoodsOrderInternalIntermediary> tempList = new ArrayList<>();
        for (int i = 0; i < internalIntermediaries.size(); i++) {
            Long internalIntermediary = internalIntermediaries.get(i);
            if (!systemUsers.contains(internalIntermediary)) {
                log.error("basicParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                errorMessages.add("basicParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                continue;
            }

            createInternalIntermediary(goodsOrder, tempList, internalIntermediary);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            goodsOrderInternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Creates a new internal intermediary for the given goods order.
     *
     * @param goodsOrder           the goods order to be updated
     * @param tempList             the list of internal intermediaries to be saved
     * @param internalIntermediary the internal intermediary to be created
     */
    private void createInternalIntermediary(GoodsOrder goodsOrder, List<GoodsOrderInternalIntermediary> tempList, Long internalIntermediary) {
        GoodsOrderInternalIntermediary intermediary = new GoodsOrderInternalIntermediary();
        intermediary.setStatus(EntityStatus.ACTIVE);
        intermediary.setAccountManagerId(internalIntermediary);
        intermediary.setOrderId(goodsOrder.getId());
        tempList.add(intermediary);
    }


    /**
     * Validates and updates internal intermediaries for the goods order.
     *
     * @param internalIntermediaries list of internal intermediary IDs
     * @param goodsOrder             goods order to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    public void updateInternalIntermediaries(List<Long> internalIntermediaries,
                                             GoodsOrder goodsOrder,
                                             List<String> errorMessages) {
        List<GoodsOrderInternalIntermediary> persistedInternalIntermediaries = goodsOrderInternalIntermediaryRepository
                .findByOrderIdAndStatusIn(goodsOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(internalIntermediaries)) {
            if (CollectionUtils.isNotEmpty(persistedInternalIntermediaries)) {
                // user has removed all internal intermediaries, should set deleted status to them
                persistedInternalIntermediaries.forEach(intermediary -> intermediary.setStatus(EntityStatus.DELETED));
                goodsOrderInternalIntermediaryRepository.saveAll(persistedInternalIntermediaries);
            }
            return;
        }

        List<GoodsOrderInternalIntermediary> tempList = new ArrayList<>();

        // at this moment we already know that internalIntermediaries list is not empty
        log.debug("Updating internal intermediaries for goods order: {}", internalIntermediaries);

        if (CollectionUtils.isEmpty(persistedInternalIntermediaries)) {
            // user has added new internal intermediaries, should create them
            createInternalIntermediaries(internalIntermediaries, goodsOrder, errorMessages);
            return;
        } else {
            // user has modified (added/edited) internal intermediaries, should update them
            List<Long> persistedInternalIntermediaryIds = persistedInternalIntermediaries
                    .stream()
                    .map(GoodsOrderInternalIntermediary::getAccountManagerId)
                    .toList();

            List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), internalIntermediaries);

            for (int i = 0; i < internalIntermediaries.size(); i++) {
                Long internalIntermediary = internalIntermediaries.get(i);
                if (!systemUsers.contains(internalIntermediary)) {
                    log.error("basicParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                            .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                    errorMessages.add("basicParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                            .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                    continue;
                }

                if (!persistedInternalIntermediaryIds.contains(internalIntermediary)) {
                    createInternalIntermediary(goodsOrder, tempList, internalIntermediary);
                } else {
                    Optional<GoodsOrderInternalIntermediary> persistedInternalIntermediaryOptional = persistedInternalIntermediaries
                            .stream()
                            .filter(contractInternalIntermediary -> contractInternalIntermediary.getAccountManagerId().equals(internalIntermediary))
                            .findFirst();
                    if (persistedInternalIntermediaryOptional.isEmpty()) {
                        log.error("Unable to find persisted internal intermediary with ID %s".formatted(internalIntermediary));
                        errorMessages.add("Unable to find persisted internal intermediary with ID %s".formatted(internalIntermediary));
                    } else {
                        GoodsOrderInternalIntermediary orderInternalIntermediary = persistedInternalIntermediaryOptional.get();
                        orderInternalIntermediary.setAccountManagerId(internalIntermediary);
                        tempList.add(orderInternalIntermediary);
                    }
                }
            }

            // user has removed some internal intermediaries, should set deleted status to them
            for (GoodsOrderInternalIntermediary internalIntermediary : persistedInternalIntermediaries) {
                if (!internalIntermediaries.contains(internalIntermediary.getAccountManagerId())) {
                    internalIntermediary.setStatus(EntityStatus.DELETED);
                    tempList.add(internalIntermediary);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            goodsOrderInternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Validates and creates external intermediaries for the goods order.
     *
     * @param externalIntermediaries list of external intermediary IDs
     * @param goodsOrder             goods order to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void createExternalIntermediaries(List<Long> externalIntermediaries,
                                              GoodsOrder goodsOrder,
                                              List<String> errorMessages) {
        if (CollectionUtils.isEmpty(externalIntermediaries)) {
            // adding external intermediaries is not mandatory
            return;
        }

        log.debug("Creating external intermediaries for goods order: {}", externalIntermediaries);
        List<GoodsOrderExternalIntermediary> tempList = new ArrayList<>();

        for (int i = 0; i < externalIntermediaries.size(); i++) {
            Long externalIntermediary = externalIntermediaries.get(i);
            if (!externalIntermediaryRepository.existsByIdAndStatusIn(externalIntermediary, List.of(ACTIVE))) {
                log.error("basicParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                        .formatted(i, externalIntermediary, List.of(ACTIVE)));
                errorMessages.add("basicParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                        .formatted(i, externalIntermediary, List.of(ACTIVE)));
                continue;
            }

            createExternalIntermediary(goodsOrder, tempList, externalIntermediary);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            goodsOrderExternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Creates an external intermediary for the given goods order.
     *
     * @param goodsOrder           goods order to be updated
     * @param tempList             list of external intermediaries to be saved
     * @param externalIntermediary ID of the external intermediary to be created
     */
    private void createExternalIntermediary(GoodsOrder goodsOrder,
                                            List<GoodsOrderExternalIntermediary> tempList,
                                            Long externalIntermediary) {
        GoodsOrderExternalIntermediary intermediary = new GoodsOrderExternalIntermediary();
        intermediary.setStatus(EntityStatus.ACTIVE);
        intermediary.setExternalIntermediaryId(externalIntermediary);
        intermediary.setOrderId(goodsOrder.getId());
        tempList.add(intermediary);
    }


    /**
     * Validates and updates external intermediaries for the goods order.
     *
     * @param externalIntermediaries list of external intermediary IDs
     * @param goodsOrder             goods order object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    public void updateExternalIntermediaries(List<Long> externalIntermediaries,
                                             GoodsOrder goodsOrder,
                                             List<String> errorMessages) {
        List<GoodsOrderExternalIntermediary> persistedExternalIntermediaries = goodsOrderExternalIntermediaryRepository
                .findByOrderIdAndStatusIn(goodsOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(externalIntermediaries)) {
            if (CollectionUtils.isNotEmpty(persistedExternalIntermediaries)) {
                // user has removed all external intermediaries, should set deleted status to them
                persistedExternalIntermediaries.forEach(externalIntermediary -> externalIntermediary.setStatus(EntityStatus.DELETED));
                goodsOrderExternalIntermediaryRepository.saveAll(persistedExternalIntermediaries);
            }
            return;
        }

        List<GoodsOrderExternalIntermediary> tempList = new ArrayList<>();

        // at this moment we already know that external intermediaries are present in request
        log.debug("Updating external intermediaries for goods order: {}", externalIntermediaries);

        if (CollectionUtils.isEmpty(persistedExternalIntermediaries)) {
            // user has added new external intermediaries, should create them
            createExternalIntermediaries(externalIntermediaries, goodsOrder, errorMessages);
            return;
        } else {
            // user has modified existing external intermediaries, should update them
            List<Long> persistedExternalIntermediaryIds = persistedExternalIntermediaries
                    .stream()
                    .map(GoodsOrderExternalIntermediary::getExternalIntermediaryId)
                    .toList();

            for (int i = 0; i < externalIntermediaries.size(); i++) {
                Long externalIntermediary = externalIntermediaries.get(i);

                if (persistedExternalIntermediaryIds.contains(externalIntermediary)) {
                    // if the external intermediary is already persisted, we validate its presence in ACTIVE and INACTIVE nomenclatures
                    if (!externalIntermediaryRepository.existsByIdAndStatusIn(externalIntermediary, List.of(ACTIVE, INACTIVE))) {
                        log.error("basicParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                                .formatted(i, externalIntermediary, List.of(ACTIVE, INACTIVE)));
                        errorMessages.add("basicParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                                .formatted(i, externalIntermediary, List.of(ACTIVE, INACTIVE)));
                        continue;
                    }

                    Optional<GoodsOrderExternalIntermediary> persistedExternalIntermediaryOptional = persistedExternalIntermediaries
                            .stream()
                            .filter(contractExternalIntermediary -> contractExternalIntermediary.getExternalIntermediaryId().equals(externalIntermediary))
                            .findFirst();
                    if (persistedExternalIntermediaryOptional.isEmpty()) {
                        log.error("basicParameters.externalIntermediaries[%s]-Unable to find persisted external intermediary with ID %s;"
                                .formatted(i, externalIntermediary));
                        errorMessages.add("basicParameters.externalIntermediaries[%s]-Unable to find persisted external intermediary with ID %s;"
                                .formatted(i, externalIntermediary));
                    } else {
                        GoodsOrderExternalIntermediary persistedExternalIntermediary = persistedExternalIntermediaryOptional.get();
                        persistedExternalIntermediary.setExternalIntermediaryId(externalIntermediary);
                        tempList.add(persistedExternalIntermediary);
                    }
                } else {
                    // if the external intermediary is not persisted, we validate its presence in ACTIVE nomenclature
                    if (!externalIntermediaryRepository.existsByIdAndStatusIn(externalIntermediary, List.of(ACTIVE))) {
                        log.error("basicParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                                .formatted(i, externalIntermediary, List.of(ACTIVE)));
                        errorMessages.add("basicParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                                .formatted(i, externalIntermediary, List.of(ACTIVE)));
                        continue;
                    }

                    createExternalIntermediary(goodsOrder, tempList, externalIntermediary);
                }
            }

            // if the external intermediary is not present in the request, we set its status to DELETED
            for (GoodsOrderExternalIntermediary externalIntermediary : persistedExternalIntermediaries) {
                if (!externalIntermediaries.contains(externalIntermediary.getExternalIntermediaryId())) {
                    externalIntermediary.setStatus(EntityStatus.DELETED);
                    tempList.add(externalIntermediary);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            goodsOrderExternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Validates and creates assisting employees for the goods order.
     *
     * @param assistingEmployees list of assisting employee IDs
     * @param goodsOrder         goods order to be updated
     * @param errorMessages      list of error messages to be populated in case of validation errors
     */
    private void createAssistingEmployees(List<Long> assistingEmployees, GoodsOrder goodsOrder, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(assistingEmployees)) {
            // assisting employees are not mandatory
            return;
        }

        log.debug("Creating assisting employees for goods order: {}", assistingEmployees);

        List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), assistingEmployees);

        List<GoodsOrderAssistingEmployee> tempList = new ArrayList<>();
        for (int i = 0; i < assistingEmployees.size(); i++) {
            Long assistingEmployee = assistingEmployees.get(i);
            if (!systemUsers.contains(assistingEmployee)) {
                log.error("basicParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                errorMessages.add("basicParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                continue;
            }

            createAssistingEmployee(assistingEmployee, goodsOrder, tempList);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            goodsOrderAssistingEmployeeRepository.saveAll(tempList);
        }
    }


    /**
     * Creates assisting employee for the goods order.
     *
     * @param assistingEmployee assisting employee ID
     * @param goodsOrder        goods order object to be updated
     * @param tempList          list of assisting employees to be populated
     */
    private void createAssistingEmployee(Long assistingEmployee, GoodsOrder goodsOrder, List<GoodsOrderAssistingEmployee> tempList) {
        GoodsOrderAssistingEmployee assistant = new GoodsOrderAssistingEmployee();
        assistant.setStatus(EntityStatus.ACTIVE);
        assistant.setAccountManagerId(assistingEmployee);
        assistant.setOrderId(goodsOrder.getId());
        tempList.add(assistant);
    }


    /**
     * Validates and updates assisting employees for the goods order.
     *
     * @param assistingEmployees list of assisting employee IDs
     * @param goodsOrder         goods order object to be updated
     * @param errorMessages      list of error messages to be populated in case of validation errors
     */
    public void updateAssistingEmployees(List<Long> assistingEmployees,
                                         GoodsOrder goodsOrder,
                                         List<String> errorMessages) {
        List<GoodsOrderAssistingEmployee> persistedAssistingEmployees = goodsOrderAssistingEmployeeRepository
                .findByOrderIdAndStatusIn(goodsOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(assistingEmployees)) {
            if (CollectionUtils.isNotEmpty(persistedAssistingEmployees)) {
                // user has removed all assisting employees, should set deleted status to them
                persistedAssistingEmployees.forEach(assistingEmployee -> assistingEmployee.setStatus(EntityStatus.DELETED));
                goodsOrderAssistingEmployeeRepository.saveAll(persistedAssistingEmployees);
            }
            return;
        }

        List<GoodsOrderAssistingEmployee> tempList = new ArrayList<>();

        // at this moment we already know that assisting employees list is not empty
        log.debug("Updating assisting employees for goods order: {}", assistingEmployees);

        if (CollectionUtils.isEmpty(persistedAssistingEmployees)) {
            // user has added new assisting employees, should create them
            createAssistingEmployees(assistingEmployees, goodsOrder, errorMessages);
            return;
        } else {
            // user has modified (added/edited) assisting employees, should update them
            List<Long> persistedAssistingEmployeeIds = persistedAssistingEmployees
                    .stream()
                    .map(GoodsOrderAssistingEmployee::getAccountManagerId)
                    .toList();

            List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), assistingEmployees);

            for (int i = 0; i < assistingEmployees.size(); i++) {
                Long assistingEmployee = assistingEmployees.get(i);
                if (!systemUsers.contains(assistingEmployee)) {
                    log.error("basicParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                            .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                    errorMessages.add("basicParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                            .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                    continue;
                }

                if (!persistedAssistingEmployeeIds.contains(assistingEmployee)) {
                    createAssistingEmployee(assistingEmployee, goodsOrder, tempList);
                } else {
                    Optional<GoodsOrderAssistingEmployee> persistedAssistingEmployeeOptional = persistedAssistingEmployees
                            .stream()
                            .filter(assistingEmployee1 -> assistingEmployee1.getAccountManagerId().equals(assistingEmployee))
                            .findFirst();

                    if (persistedAssistingEmployeeOptional.isEmpty()) {
                        log.error("Unable to find persisted internal intermediary with ID %s".formatted(assistingEmployee));
                        errorMessages.add("Unable to find persisted internal intermediary with ID %s".formatted(assistingEmployee));
                    } else {
                        GoodsOrderAssistingEmployee persistedAssistingEmployee = persistedAssistingEmployeeOptional.get();
                        persistedAssistingEmployee.setAccountManagerId(assistingEmployee);
                        tempList.add(persistedAssistingEmployee);
                    }
                }
            }

            // user has removed some assisting employees, should set deleted status to them
            for (GoodsOrderAssistingEmployee assistingEmployee : persistedAssistingEmployees) {
                if (!assistingEmployees.contains(assistingEmployee.getAccountManagerId())) {
                    assistingEmployee.setStatus(EntityStatus.DELETED);
                    tempList.add(assistingEmployee);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            goodsOrderAssistingEmployeeRepository.saveAll(tempList);
        }
    }


    protected List<GoodsOrderSubObjectShortResponse> getInternalIntermediaries(Long orderId) {
        log.debug("Getting internal intermediaries for goods order with ID: {}", orderId);
        return goodsOrderInternalIntermediaryRepository.getShortResponseByOrderIdAndStatusIn(orderId, List.of(EntityStatus.ACTIVE));
    }

    protected List<GoodsOrderSubObjectShortResponse> getExternalIntermediaries(Long orderId) {
        log.debug("Getting external intermediaries for goods order with ID: {}", orderId);
        return goodsOrderExternalIntermediaryRepository.getShortResponseByOrderIdAndStatusIn(orderId, List.of(EntityStatus.ACTIVE));
    }

    protected List<GoodsOrderSubObjectShortResponse> getAssistingEmployees(Long orderId) {
        log.debug("Getting assisting employees for goods order with ID: {}", orderId);
        return goodsOrderAssistingEmployeeRepository.getShortResponseByOrderIdAndStatusIn(orderId, List.of(EntityStatus.ACTIVE));
    }


    @Transactional
    public Long createProxy(Customer customer, Long customerDetailId, ProxyEditRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        if (request != null) {
            log.debug("Creating proxy object: {}", request);
            List<Manager> managers = getManagers(customer, customerDetailId, request, errorMessages);
            List<GoodsOrderProxyFiles> proxyFile = getProxyFiles(request.getFileIds(), errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            GoodsOrderProxies proxy = mapProxyRequestToProxyEntity(request, goodsOrder.getId());
            GoodsOrderProxies dbProxy = goodsOrderProxyRepository.save(proxy);
            updateProxyFiles(dbProxy, proxyFile, errorMessages);
            updateProxyManagers(dbProxy, managers, errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            return dbProxy.getId();
        }
        return null;
    }

    @Transactional
    public Long updateProxy(Customer customer, Long customerDetailId, ProxyEditRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        log.debug("updating proxy object: {}", request);
        Optional<GoodsOrderProxies> dbProxyOptional = goodsOrderProxyRepository.findByIdAndStatus(request.getId(), EntityStatus.ACTIVE);
        if (dbProxyOptional.isPresent()) {
            GoodsOrderProxies dbPoxy = dbProxyOptional.get();
            List<Manager> managers = getManagers(customer, customerDetailId, request, errorMessages);
            List<GoodsOrderProxyFiles> proxyFile = getProxyFiles(request.getFileIds(), errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            //GoodsOrderProxies proxyToSave = mapProxyRequestToProxyEntity(request, goodsOrder.getId());
            GoodsOrderProxies proxyToSave = mapRequestToProxy(request, dbPoxy);
            GoodsOrderProxies savedProxy = goodsOrderProxyRepository.save(proxyToSave);
            updateProxyFiles(savedProxy, proxyFile, errorMessages);
            updateProxyManagers(savedProxy, managers, errorMessages);
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
            return savedProxy.getId();
        } else {
            errorMessages.add("can't find proxy with id:%s;".formatted(request.getId()));
        }
        return null;
    }

    private GoodsOrderProxies mapRequestToProxy(ProxyEditRequest request, GoodsOrderProxies dbPoxy) {
        dbPoxy.setProxyForeignEntityPerson(request.getProxyForeignEntityPerson());
        dbPoxy.setProxyName(request.getProxyName());
        dbPoxy.setProxyPersonalIdentifier(request.getProxyCustomerIdentifier());
        dbPoxy.setProxyEmail(request.getProxyEmail());
        dbPoxy.setProxyMobilePhone(request.getProxyPhone());
        dbPoxy.setProxyAttorneyPowerNumber(request.getProxyPowerOfAttorneyNumber());
        dbPoxy.setProxyDate(request.getProxyData());
        dbPoxy.setProxyValidTill(request.getProxyValidTill());
        dbPoxy.setProxyNotaryPublic(request.getNotaryPublic());
        dbPoxy.setProxyRegistrationNumber(request.getRegistrationNumber());
        dbPoxy.setProxyOperationArea(request.getAreaOfOperation());
        dbPoxy.setProxyByProxyForeignEntityPerson(request.getAuthorizedProxyForeignEntityPerson());
        dbPoxy.setProxyByProxyPersonalIdentifier(request.getAuthorizedProxyCustomerIdentifier());
        dbPoxy.setProxyByProxyEmail(request.getAuthorizedProxyEmail());
        dbPoxy.setProxyByProxyMobilePhone(request.getAuthorizedProxyPhone());
        dbPoxy.setProxyByProxyAttorneyPowerNumber(request.getAuthorizedProxyPowerOfAttorneyNumber());
        dbPoxy.setProxyByProxyDate(request.getAuthorizedProxyData());
        dbPoxy.setProxyByProxyValidTill(request.getAuthorizedProxyValidTill());
        dbPoxy.setProxyNotaryPublic(request.getNotaryPublic());
        dbPoxy.setProxyByProxyOperationArea(request.getAreaOfOperation());
        return dbPoxy;
    }

    private void updateProxyManagers(GoodsOrderProxies dbProxy, List<Manager> managers, List<String> errorMessages) {
        if (managers != null) {
            Long proxyId = dbProxy.getId();
            List<GoodsOrderProxyManagers> proxyManagers = new ArrayList<>();
            for (Manager item : managers) {
                GoodsOrderProxyManagers proxyManager = new GoodsOrderProxyManagers();
                proxyManager.setOrderProxyId(proxyId);
                proxyManager.setCustomerManagerId(item.getId());
                proxyManager.setStatus(EntityStatus.ACTIVE);
                proxyManagers.add(proxyManager);
            }
            if (CollectionUtils.isNotEmpty(proxyManagers)) {
                goodsOrderProxyManagersRepository.saveAll(proxyManagers);
            }
        }
    }

    private void updateProxyFiles(GoodsOrderProxies dbProxy, List<GoodsOrderProxyFiles> proxyFile, List<String> errorMessages) {
        Long proxyId = dbProxy.getId();
        for (GoodsOrderProxyFiles item : proxyFile) {
            item.setOrderProxyId(proxyId);
            item.setCreateDate(LocalDateTime.now());
            goodsOrderProxyFilesRepository.save(item);
        }
    }

    private GoodsOrderProxies mapProxyRequestToProxyEntity(ProxyEditRequest request, Long orderId) {
        return GoodsOrderProxies.builder()
                .proxyName(request.getProxyName())
                .proxyForeignEntityPerson(request.getProxyForeignEntityPerson())
                .proxyPersonalIdentifier(request.getProxyCustomerIdentifier())
                .proxyEmail(request.getProxyEmail())
                .proxyMobilePhone(request.getProxyPhone())
                .proxyAttorneyPowerNumber(request.getProxyPowerOfAttorneyNumber())
                .proxyDate(request.getProxyData())
                .proxyValidTill(request.getProxyValidTill())
                .proxyNotaryPublic(request.getNotaryPublic())
                .proxyRegistrationNumber(request.getRegistrationNumber())
                .proxyOperationArea(request.getAreaOfOperation())
                .proxyByProxyForeignEntityPerson(request.getAuthorizedProxyForeignEntityPerson() != null ? request.getAuthorizedProxyForeignEntityPerson() : false)
                .proxyByProxyName(request.getProxyAuthorizedByProxy())
                .proxyByProxyPersonalIdentifier(request.getAuthorizedProxyCustomerIdentifier())
                .proxyByProxyEmail(request.getAuthorizedProxyEmail())
                .proxyByProxyMobilePhone(request.getAuthorizedProxyPhone())
                .proxyByProxyAttorneyPowerNumber(request.getAuthorizedProxyPowerOfAttorneyNumber())
                .proxyByProxyDate(request.getAuthorizedProxyData())
                .proxyByProxyValidTill(request.getAuthorizedProxyValidTill())
                .proxyByProxyNotaryPublic(request.getAuthorizedProxyNotaryPublic())
                .proxyByProxyRegistrationNumber(request.getAuthorizedProxyRegistrationNumber())
                .proxyByProxyOperationArea(request.getAuthorizedProxyAreaOfOperation())
                .status(EntityStatus.ACTIVE)
                .orderId(orderId)
                .build();
    }

    private List<Manager> getManagers(Customer customer, Long customerDetailId, ProxyEditRequest request, List<String> exceptionMessages) {
        List<Manager> managers = new ArrayList<>();
        Set<Long> managerIds = request.getManagerIds();
        if (!customer.getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)) {
            if (!CollectionUtils.isEmpty(managerIds)) {
                for (Long id : managerIds) {
                    Optional<Manager> manager = managerRepository.findByIdAndStatus(id, Status.ACTIVE);
                    if (manager.isEmpty()) {
                        exceptionMessages.add("managerIds-[managerIds] can't find account manager with id: %s;".formatted(id));
                    } else {
                        if (manager.get().getCustomerDetailId().equals(customerDetailId)) {
                            managers.add(manager.get());
                        } else {
                            exceptionMessages.add("managerIds-[managerIds] this manager doesn't belong to the customer: %s;".formatted(id));
                        }
                    }
                }
            } else {
                exceptionMessages.add("managerIds-[managerIds] should be present when customer is LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY;");
            }
        } else {
            if (!CollectionUtils.isEmpty(managers)) {
                exceptionMessages.add("managerIds-[managerIds] should not be present when customer is PRIVATE_CUSTOMER;");
            }
        }
        return managers;
    }

    private List<GoodsOrderProxyFiles> getProxyFiles(Set<Long> fileIds, List<String> exceptionMessages) {
        List<GoodsOrderProxyFiles> proxyFiles = new ArrayList<>();
        if (CollectionUtils.isEmpty(proxyFiles)) {
            if (fileIds != null) {
                for (Long id : fileIds) {
                    Optional<GoodsOrderProxyFiles> proxyFile =
                            goodsOrderProxyFilesRepository.findByIdAndStatus(id, EntityStatus.ACTIVE);
                    if (proxyFile.isPresent()) {
                        GoodsOrderProxyFiles dbProxyFile = proxyFile.get();
                        if (proxyFile.get().getOrderProxyId() != null) {
                            GoodsOrderProxyFiles orderProxyFiles = GoodsOrderProxyFiles.builder().
                                    name(dbProxyFile.getName())
                                    .fileUrl(dbProxyFile.getFileUrl())
                                    .orderProxyId(null)
                                    .status(EntityStatus.ACTIVE)
                                    .build();
                            GoodsOrderProxyFiles savedProxyFile = goodsOrderProxyFilesRepository.save(orderProxyFiles);
                            proxyFiles.add(savedProxyFile);
                        } else proxyFiles.add(proxyFile.get());
                    } else {
                        exceptionMessages.add("fileIds-[FileIds] can't find active file with id: %s;".formatted(id));
                    }
                }
            }
            return proxyFiles;
        }
        return null;
    }

    private void createPaymentTerms(GoodsOrderPaymentTermRequest paymentTerm, GoodsOrder goodsOrder, List<String> errorMessages) {
        log.debug("Creating new payment term");
        if (paymentTerm != null) {
            Long orderId = goodsOrder.getId();

            Long calendarId = paymentTerm.getCalendarId();

            Optional<Calendar> calendarOptional = calendarRepository
                    .findByIdAndStatusIsIn(calendarId, List.of(ACTIVE));

            if (calendarOptional.isEmpty()) {
                log.error("Requested calendar not found");
                errorMessages.add("basicParameters.paymentTerm.calendarId-Calendar with presented id: [%s] not found;".formatted(calendarId));
                return;
            }

            goodsOrderPaymentTermRepository.save(
                    new GoodsOrderPaymentTerm(
                            null,
                            paymentTerm.getName(),
                            orderId,
                            paymentTerm.getType(),
                            paymentTerm.getValue(),
                            calendarId,
                            paymentTerm.getExcludes(),
                            paymentTerm.getDueDateChange(),
                            EntityStatus.ACTIVE
                    )
            );
        }
    }

    private void updatePaymentTerms(GoodsOrderPaymentTermRequest paymentTerm, GoodsOrder goodsOrder, List<String> errorMessages) {
        log.debug("Updating payment terms");
        List<GoodsOrderPaymentTerm> uncommittedEntities = new ArrayList<>();

        List<GoodsOrderPaymentTerm> activePaymentTerms = goodsOrderPaymentTermRepository
                .findAllByOrderIdAndStatusIn(goodsOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (!Objects.isNull(paymentTerm)) {
            Long id = paymentTerm.getId();

            if (id != null) {
                Optional<GoodsOrderPaymentTerm> goodsOrderPaymentTermOptional = activePaymentTerms
                        .stream()
                        .filter(goodsOrderPaymentTerm -> goodsOrderPaymentTerm.getId().equals(id))
                        .findFirst();

                if (goodsOrderPaymentTermOptional.isPresent()) {
                    GoodsOrderPaymentTerm goodsOrderPaymentTerm = goodsOrderPaymentTermOptional.get();
                    goodsOrderPaymentTerm.setName(paymentTerm.getName());
                    goodsOrderPaymentTerm.setType(paymentTerm.getType());
                    goodsOrderPaymentTerm.setValue(paymentTerm.getValue());
                    goodsOrderPaymentTerm.setCalendarId(validateCalendarAndReturnId(paymentTerm.getCalendarId(), goodsOrderPaymentTerm, errorMessages));
                    goodsOrderPaymentTerm.setExcludes(paymentTerm.getExcludes());
                    goodsOrderPaymentTerm.setDueDateChange(paymentTerm.getDueDateChange());

                    uncommittedEntities.add(goodsOrderPaymentTerm);
                } else {
                    log.error("Requested payment term not found, update is not possible");
                    errorMessages.add("basicParameters.paymentTerm.id-Payment term with presented id: [%s] not found for goods order with id: [%s];".formatted(id, goodsOrder.getId()));
                }
            } else {
                createPaymentTerms(paymentTerm, goodsOrder, errorMessages);
            }
        }

        log.debug("Deleting outdated payment terms");
        if (!Objects.isNull(paymentTerm)) {
            activePaymentTerms
                    .stream()
                    .filter(pt -> !pt.getId().equals(paymentTerm.getId()))
                    .forEach(pt -> pt.setStatus(EntityStatus.DELETED));
        } else {
            activePaymentTerms
                    .forEach(pt -> pt.setStatus(EntityStatus.DELETED));
        }

        uncommittedEntities.addAll(activePaymentTerms);

        if (CollectionUtils.isEmpty(errorMessages)) {
            log.debug("Saving uncommitted entities to database");
            goodsOrderPaymentTermRepository.saveAll(uncommittedEntities);
        }
    }

    public List<GoodsOrderPaymentTermResponse> getGoodsOrderPaymentTerms(Long orderId) {
        return goodsOrderPaymentTermRepository
                .findAllByOrderIdAndStatusIn(orderId, List.of(EntityStatus.ACTIVE))
                .stream()
                .map(goodsOrderPaymentTerm -> {
                    Long calendarId = goodsOrderPaymentTerm.getCalendarId();
                    return new GoodsOrderPaymentTermResponse(
                            goodsOrderPaymentTerm.getId(),
                            goodsOrderPaymentTerm.getName(),
                            goodsOrderPaymentTerm.getType(),
                            goodsOrderPaymentTerm.getValue(),
                            calendarId == null ? null : fetchCalendarAndMapToResponse(calendarId),
                            goodsOrderPaymentTerm.getExcludes(),
                            goodsOrderPaymentTerm.getDueDateChange()
                    );
                }).toList();
    }

    public void editGoodsOrderFromRequest(GoodsOrderEditRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        log.debug("Editing goods order goods parameters");
        GoodsOrderBasicParametersCreateRequest basicParameters = request.getBasicParameters();
        processEmployeeOnUpdate(basicParameters.getEmployeeId(), goodsOrder, errorMessages);
        validateAssistingEmployees(basicParameters.getEmployeeId(), basicParameters.getAssistingEmployees(), errorMessages);
        goodsOrder.setDirectDebit(basicParameters.getDirectDebit());
        goodsOrder.setIban(basicParameters.getIban());
        goodsOrder.setPrepaymentTermInCalendarDays(basicParameters.getPaymentTermInCalendarDays());

        goodsOrder.setNoInterestOnOverdueDebts(request.getBasicParameters().isNoInterestInOverdueDebts());
        goodsOrder.setBankId(validateBankForEditAndReturnId(basicParameters.getBankId(), goodsOrder, errorMessages));
        goodsOrder.setApplicableInterestRateId(validateInterestRateAndReturnId(basicParameters.getInterestRateId(), errorMessages));
        goodsOrder.setCampaignId(validateCampaignAndReturnId(basicParameters.getCampaignId(), goodsOrder, errorMessages));
        goodsOrder.setCustomerDetailId(validateCustomerDetailsAndReturnId(goodsOrder.getCustomerDetailId(), basicParameters.getCustomerDetailId(), errorMessages));
        goodsOrder.setCustomerCommunicationIdForBilling(validateCommunicationForBillingAndReturnId(basicParameters.getCustomerCommunicationIdForBilling(), errorMessages));
    }

    private void validateAssistingEmployees(Long employeeId, List<Long> assistingEmployees, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(assistingEmployees) && assistingEmployees.contains(employeeId)) {
            errorMessages.add("basicParameters.assistingEmployees-Assisting employee should not match employee;");
        }
    }


    /**
     * Processes employee field on update to be able to change the creator user of the order.
     *
     * @param employeeId    ID of the employee to be set
     * @param goodsOrder    goods order to be updated
     * @param errorMessages list of error messages to be populated in case of validation errors
     */
    private void processEmployeeOnUpdate(Long employeeId, GoodsOrder goodsOrder, List<String> errorMessages) {
        if (employeeId == null) {
            log.error("basicParameters.employeeId-Employee ID is mandatory;");
            errorMessages.add("basicParameters.employeeId-Employee ID is mandatory;");
            return;
        }

        if (!accountManagerRepository.existsByIdAndStatusIn(employeeId, List.of(Status.ACTIVE))) {
            log.error("basicParameters.employeeId-Unable to find employee with ID %s in statuses %s;".formatted(employeeId, List.of(Status.ACTIVE)));
            errorMessages.add("goodsOrder.employeeId-Unable to find employee with ID %s in statuses %s;".formatted(employeeId, List.of(Status.ACTIVE)));
        } else {
            goodsOrder.setEmployeeId(employeeId);
        }
    }


    private Long validateBankForEditAndReturnId(Long requestedBankId, GoodsOrder goodsOrder, List<String> errorMessages) {
        log.debug("Validating bank");
        if (requestedBankId != null) {
            Optional<Bank> requestedBankOptional = bankRepository
                    .findByIdAndStatus(requestedBankId, List.of(ACTIVE, INACTIVE));

            if (requestedBankOptional.isPresent()) {
                Bank requestedBank = requestedBankOptional.get();

                if (requestedBank.getStatus().equals(INACTIVE)) {
                    if (!goodsOrder.getBankId().equals(requestedBankId)) {
                        log.error("Requested bank nomenclature is INACTIVE");
                        errorMessages.add("basicParameters.bankId-You cannot assign bank with INACTIVE status to goods order;");
                    }
                }
            } else {
                log.error("Requested bank nomenclature not found");
                errorMessages.add("basicParameters.bankingDetails.bankId-Bank with presented ID: [%s] not found;".formatted(requestedBankId));
            }
        }
        return requestedBankId;
    }

    private Long validateInterestRateAndReturnId(Long requestedInterestRateId, List<String> errorMessages) {
        log.debug("Validating interest rate");
        Optional<InterestRate> requestedInterestRateOptional = interestRateRepository
                .findByIdAndStatusIn(requestedInterestRateId, List.of(InterestRateStatus.ACTIVE));

        if (requestedInterestRateOptional.isEmpty()) {
            log.error("Requested interest rate not found");
            errorMessages.add("basicParameters.interestRateId-Interest Rate with presented ID: [%s] not found;".formatted(requestedInterestRateId));
        }

        return requestedInterestRateId;
    }

    private Long validateCampaignAndReturnId(Long requestedCampaignId, GoodsOrder goodsOrder, List<String> errorMessages) {
        log.debug("Validating campaign");
        if (requestedCampaignId != null) {
            Optional<Campaign> requestedCampaignOptional = campaignRepository
                    .findByIdAndStatusIn(requestedCampaignId, List.of(ACTIVE, INACTIVE));

            if (requestedCampaignOptional.isPresent()) {
                Campaign requestedCampaign = requestedCampaignOptional.get();

                if (requestedCampaign.getStatus().equals(INACTIVE)) {
                    if (!goodsOrder.getCampaignId().equals(requestedCampaignId)) {
                        log.error("Requested campaing nomenclature is INACTIVE");
                        errorMessages.add("basicParameters.campaignId-You cannot assign campaign with INACTIVE status to goods order;");
                    }
                }
            } else {
                log.error("Requested campaign not found");
                errorMessages.add("basicParameters.campaignId-Campaign with presented ID: [%s] not found;".formatted(requestedCampaignId));
            }
        }

        return requestedCampaignId;
    }

    private Long validateCustomerDetailsAndReturnId(Long currentCustomerDetailsId, Long requestedCustomerDetailsId, List<String> errorMessages) {
        log.debug("Validating customer details");

        Optional<CustomerDetails> requestedCustomerDetailsOptional = customerDetailsRepository
                .findById(requestedCustomerDetailsId);
        if (currentCustomerDetailsId.equals(requestedCustomerDetailsId)) {
            if (requestedCustomerDetailsOptional.isEmpty()) {
                log.error("Requested customer not found");
                errorMessages.add("basicParameters.customerDetailId-Customer Details with presented ID: [%s] not found;".formatted(requestedCustomerDetailsId));
            } else {
                CustomerDetails requestedCustomerDetails = requestedCustomerDetailsOptional.get();

                Optional<Customer> requestedCustomerOptional = customerRepository
                        .findByIdAndStatuses(requestedCustomerDetails.getCustomerId(), List.of(CustomerStatus.ACTIVE));
                if (requestedCustomerOptional.isEmpty()) {
                    log.error("Requested customer is DELETED");
                    errorMessages.add("basicParameters.customerDetailId-Requested Customer is DELETED;");
                }
            }
        } else {
            if (requestedCustomerDetailsOptional.isPresent()) {
                CustomerDetails requestedCustomerDetails = requestedCustomerDetailsOptional.get();

                Optional<Customer> requestedCustomerOptional = customerRepository
                        .findByIdAndStatuses(requestedCustomerDetails.getCustomerId(), List.of(CustomerStatus.ACTIVE));

                if (requestedCustomerOptional.isPresent()) {
                    Customer requestedCustomer = requestedCustomerOptional.get();

                    Optional<UnwantedCustomer> unwantedCustomerOptional = unwantedCustomerRepository
                            .findByIdentifierAndStatusIn(requestedCustomer.getIdentifier(), List.of(UnwantedCustomerStatus.ACTIVE));

                    if (unwantedCustomerOptional.isPresent()) {
                        if (unwantedCustomerOptional.get().getCreateOrderRestriction()) {
                            log.error("Requested customer is unwanted with restriction to create/edit goods order");
                            errorMessages.add("basicParameters.customerDetailId-Requested customer is unwanted with restriction to create/edit goods order;");
                        }
                    }
                } else {
                    log.error("Requested customer is DELETED");
                    errorMessages.add("basicParameters.customerDetailId-Requested Customer is DELETED;");
                }
            } else {
                log.error("Requested customer not found");
                errorMessages.add("basicParameters.customerDetailId-Customer Details with presented ID: [%s] not found;".formatted(requestedCustomerDetailsId));
            }
        }

        return requestedCustomerDetailsId;
    }

    private Long validateCommunicationForBillingAndReturnId(Long customerCommunicationIdForBilling, List<String> errorMessages) {
        log.debug("Validating customer communication");
        Optional<CustomerCommunications> requestedCustomerCommunication = customerCommunicationsRepository
                .findByIdAndStatuses(customerCommunicationIdForBilling, List.of(Status.ACTIVE));

        if (requestedCustomerCommunication.isEmpty()) {
            log.error("Requested customer communication not found");
            errorMessages.add("basicParameters.customerCommunicationIdForBilling-Customer Communication with presented ID: [%s] not found;".formatted(customerCommunicationIdForBilling));
        }

        return customerCommunicationIdForBilling;
    }

    private Long validateCalendarAndReturnId(Long calendarId, GoodsOrderPaymentTerm goodsOrderPaymentTerm, List<String> errorMessages) {
        log.debug("Validating calendar");
        Optional<Calendar> requestedCalendarOptional = calendarRepository
                .findByIdAndStatusIsIn(calendarId, List.of(ACTIVE, INACTIVE));

        if (requestedCalendarOptional.isPresent()) {
            Calendar requestedCalendar = requestedCalendarOptional.get();
            if (requestedCalendar.getStatus().equals(INACTIVE)) {
                if (!goodsOrderPaymentTerm.getCalendarId().equals(calendarId)) {
                    log.error("Requested calendar nomenclature is INACTIVE");
                    errorMessages.add("basicParameters.paymentTerm.calendarId-You cannot assign calendar with INACTIVE status to goods order payment term;");
                }
            }
        } else {
            log.error("Requested calendar not found");
            errorMessages.add("basicParameters.paymentTerm.calendarId-Calendar with presented ID: [%s] not found;".formatted(calendarId));
        }

        return calendarId;
    }

    private CalendarShortResponse fetchCalendarAndMapToResponse(Long calendarId) {
        return new CalendarShortResponse(
                calendarRepository
                        .findByIdAndStatusIsIn(calendarId, List.of(ACTIVE, INACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("Payment term calendar not found or DELETED"))
        );
    }

    public List<TaskShortResponse> getTasks(Long id) {
        return taskService.getTasksByGoodsOrderId(id);
    }

    private void validate(Long templateId) {
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndStatus(templateId, ContractTemplatePurposes.INVOICE, ContractTemplateStatus.ACTIVE)) {
            throw new DomainEntityNotFoundException("templateId-Template with id %s do not exist!;".formatted(templateId));
        }
    }

    public ContractTemplateShortResponse getTemplate(Long invoiceTemplateId) {
        return contractTemplateRepository.findTemplateResponseById(invoiceTemplateId, LocalDate.now()).orElse(null);
    }
}

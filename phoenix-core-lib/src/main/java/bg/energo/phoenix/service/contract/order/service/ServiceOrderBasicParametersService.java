package bg.energo.phoenix.service.contract.order.service;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.*;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.UnwantedCustomer;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.contract.OrderType;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderBasicParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderBasicParametersRequest;
import bg.energo.phoenix.model.request.contract.order.service.ServiceOrderBasicParametersUpdateRequest;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyBaseRequest;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyUpdateRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderBasicParametersResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse;
import bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyBaseResponse;
import bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyManagerResponse;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.order.service.*;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.ManagerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ExternalIntermediaryRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.contract.relatedEntities.RelatedContractsAndOrdersService;
import bg.energo.phoenix.service.customer.UnwantedCustomerService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderBasicParametersService {
    private final ContractTemplateRepository contractTemplateRepository;
    private final CustomerRepository customerRepository;
    private final UnwantedCustomerService unwantedCustomerService;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final CustomerCommContactPurposesRepository contactPurposesRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final BankRepository bankRepository;
    private final InterestRateRepository interestRateRepository;
    private final CampaignRepository campaignRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final ServiceOrderInternalIntermediaryRepository internalIntermediaryRepository;
    private final ServiceOrderExternalIntermediaryRepository serviceOrderExternalIntermediaryRepository;
    private final ExternalIntermediaryRepository externalIntermediaryRepository;
    private final ServiceOrderAssistingEmployeeRepository assistingEmployeeRepository;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final ServiceOrderActivityService serviceOrderActivityService;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderProxyRepository serviceOrderProxyRepository;
    private final ServiceOrderProxyManagerRepository serviceOrderProxyManagerRepository;
    private final ManagerRepository customerManagerRepository;
    private final ServiceOrderProxyFileService serviceOrderProxyFileService;
    private final RelatedContractsAndOrdersService relatedContractsAndOrdersService;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final TaskService taskService;
    private final InvoiceRepository invoiceRepository;

    /**
     * Validates the basic parameters of the service order.
     *
     * @param basicParameters The basic parameters of the service order.
     * @param errorMessages   The list of error messages to be populated in case of validation errors.
     */
    public void validateBasicParametersOnCreate(ServiceOrderBasicParametersRequest basicParameters, List<String> errorMessages) {
        log.debug("Validating basic parameters: {}", basicParameters);
        validateCustomerAndServiceCompatibility(basicParameters, errorMessages, null);
        validateCustomerBillingCommunication(basicParameters, errorMessages);
        validateCampaign(basicParameters.getCampaignId(), List.of(NomenclatureItemStatus.ACTIVE), errorMessages);
        validateBankingDetails(basicParameters.getBankingDetails().getBankId(), List.of(NomenclatureItemStatus.ACTIVE), errorMessages);
        validateApplicableInterestRate(basicParameters.getInterestRateId(), List.of(InterestRateStatus.ACTIVE), errorMessages);
        validateContractTemplates(basicParameters, errorMessages);
    }

    private void validateContractTemplates(ServiceOrderBasicParametersRequest basicParameters, List<String> errorMessages) {
        if (basicParameters.getEmailTemplateId() != null && !contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(basicParameters.getEmailTemplateId(), ContractTemplatePurposes.INVOICE, ContractTemplateType.EMAIL, LocalDate.now())) {
            errorMessages.add("basicParameters.emailTemplateId-Email template does not exist or has wrong purpose!;");
        }
        if (basicParameters.getInvoiceTemplateId() != null && !contractTemplateRepository.existsByIdAndTemplatePurposeAndTemplateType(basicParameters.getInvoiceTemplateId(), ContractTemplatePurposes.INVOICE, ContractTemplateType.DOCUMENT, LocalDate.now())) {
            errorMessages.add("basicParameters.invoiceTemplateId-Invoice template does not exist or has wrong purpose!;");
        }
    }


    /**
     * @param basicParameters The basic parameters of the service order.
     * @param errorMessages   The list of error messages to be populated in case of validation errors.
     */
    private void validateCustomerAndServiceCompatibility(ServiceOrderBasicParametersRequest basicParameters, List<String> errorMessages, Long serviceOrderId) {
        log.debug("Validating customer and service compatibility for customer detail ID: {} and service detail ID: {}", basicParameters.getCustomerDetailId(), basicParameters.getServiceDetailId());

        Optional<Customer> customerOptional = customerRepository
                .findByCustomerDetailIdAndStatusIn(
                        basicParameters.getCustomerDetailId(),
                        List.of(CustomerStatus.ACTIVE)
                );

        if (customerOptional.isEmpty()) {
            log.error("basicParameters.customerDetailId-Customer not found for customer version with ID {}", basicParameters.getCustomerDetailId());
            errorMessages.add("basicParameters.customerDetailId-Customer not found for customer version.");
            return;
        }

        Customer customer = customerOptional.get();

        validateCustomerNotUnwanted(customer, errorMessages);
        validateServiceVersionAvailability(basicParameters, customer, errorMessages, serviceOrderId);
    }


    /**
     * Validates that the customer is not unwanted.
     * Concluding an order is not possible for unwanted customers for which the checkbox "Create order restriction" is checked.
     *
     * @param errorMessages The list of error messages to be populated in case of validation errors.
     */
    private void validateCustomerNotUnwanted(Customer customer, List<String> errorMessages) {
        log.debug("Validating customer not unwanted for customer with ID: {}", customer.getId());

        UnwantedCustomer unwantedCustomer = unwantedCustomerService.checkUnwantedCustomer(customer.getIdentifier());
        if (unwantedCustomer != null && BooleanUtils.isTrue(unwantedCustomer.getCreateOrderRestriction())) {
            log.error("basicParameters.customerDetailId-It is not possible for the unwanted customer to conclude an order.");
            errorMessages.add("basicParameters.customerDetailId-It is not possible for the unwanted customer to conclude an order.");
        }
    }


    /**
     * Validates that the customer communication with the given ID exists in the given statuses.
     * Selected customer communication should contain "Billing" contact purpose, and email and mobile number should be present too.
     *
     * @param basicParameters The basic parameters of the service order.
     * @param errorMessages   The list of error messages to be populated in case of validation errors.
     */
    private void validateCustomerBillingCommunication(ServiceOrderBasicParametersRequest basicParameters, List<String> errorMessages) {
        log.debug("Validating customer billing communication for customer detail ID: {} and customer communication ID: {}", basicParameters.getCustomerDetailId(), basicParameters.getCustomerCommunicationIdForBilling());

        Optional<CustomerCommunications> billingCommunicationOptional = customerCommunicationsRepository
                .findByIdAndStatuses(basicParameters.getCustomerCommunicationIdForBilling(), List.of(Status.ACTIVE));
        if (billingCommunicationOptional.isEmpty()) {
            log.error("basicParameters.customerCommunicationIdForBilling-Unable to find customer communication with ID %s in statuses %s;"
                    .formatted(basicParameters.getCustomerCommunicationIdForBilling(), List.of(Status.ACTIVE)));
            errorMessages.add("basicParameters.customerCommunicationIdForBilling-Unable to find customer communication with ID %s in statuses %s;"
                    .formatted(basicParameters.getCustomerCommunicationIdForBilling(), List.of(Status.ACTIVE)));
            return;
        }

        CustomerCommunications billingCommunication = billingCommunicationOptional.get();
        if (!Objects.equals(billingCommunication.getCustomerDetailsId(), basicParameters.getCustomerDetailId())) {
            log.error("Communication does not belong to the selected customer.");
            errorMessages.add("Communication does not belong to the selected customer.");
            return;
        }

        List<CustomerCommContactPurposes> billingCommContactPurposes = contactPurposesRepository
                .findByCustomerCommId(billingCommunication.getId(), List.of(Status.ACTIVE));

        boolean containsBillingPurpose = billingCommContactPurposes
                .stream()
                .anyMatch(purpose -> purpose.getContactPurposeId().equals(communicationContactPurposeProperties.getBillingCommunicationId()));

        if (!containsBillingPurpose) {
            log.error("basicParameters.customerCommunicationIdForBilling-Unable to find billing contact purpose for customer communication with ID %s;"
                    .formatted(basicParameters.getCustomerCommunicationIdForBilling()));
            errorMessages.add("basicParameters.customerCommunicationIdForBilling-Unable to find billing contact purpose for customer communication with ID %s;"
                    .formatted(basicParameters.getCustomerCommunicationIdForBilling()));
        }

        List<CustomerCommunicationContacts> commContacts = customerCommunicationContactsRepository.findByCustomerCommIdAndStatuses(billingCommunication.getId(), List.of(Status.ACTIVE));

        boolean containsEmail = commContacts
                .stream()
                .anyMatch(contact -> contact.getContactType().equals(CustomerCommContactTypes.EMAIL));

        boolean containsMobileNumber = commContacts
                .stream()
                .anyMatch(contact -> contact.getContactType().equals(CustomerCommContactTypes.MOBILE_NUMBER));

        if (!containsEmail || !containsMobileNumber) {
            log.error("basicParameters.customerCommunicationIdForBilling-Email and mobile number should be added to the selected customer communication with ID %s;"
                    .formatted(basicParameters.getCustomerCommunicationIdForBilling()));
            errorMessages.add("basicParameters.customerCommunicationIdForBilling-Email and mobile number should be added to the selected customer communication with ID %s;"
                    .formatted(basicParameters.getCustomerCommunicationIdForBilling()));
        }
    }


    /**
     * Validates that the service version is available for the given customer detail.
     *
     * @param basicParameters The basic parameters of the service order.
     * @param errorMessages   The list of error messages to be populated in case of validation errors.
     */
    private void validateServiceVersionAvailability(ServiceOrderBasicParametersRequest basicParameters, Customer customer, List<String> errorMessages, Long serviceOrderId) {
        log.debug("Validating service version availability for customer detail ID: {} and service detail ID: {}", basicParameters.getCustomerDetailId(), basicParameters.getServiceDetailId());

        Optional<ServiceDetails> serviceVersionOptional = serviceDetailsRepository.getAvailableVersionForServiceOrdersAndIdIn(
                basicParameters.getCustomerDetailId(),
                basicParameters.getServiceDetailId()
        );

        if (serviceVersionOptional.isEmpty()) {
            log.error("Service version with ID {} is not available for customer detail with ID {}", basicParameters.getServiceDetailId(), basicParameters.getCustomerDetailId());
            errorMessages.add("serviceDetailId-Service version is not available for customer detail");
            return;
        }

        ServiceDetails serviceVersion = serviceVersionOptional.get();
        if (StringUtils.isNotEmpty(serviceVersion.getService().getCustomerIdentifier())
                && !serviceVersion.getService().getCustomerIdentifier().equals(customer.getIdentifier())) {
            log.error("The individual service is not intended for the selected customer.");
            errorMessages.add("The individual service is not intended for the selected customer.");
        }
    }


    /**
     * Validates that the bank with the given ID exists and is in one of the given statuses.
     *
     * @param bankId        ID of the bank to validate
     * @param statuses      List of statuses to check
     * @param errorMessages List of error messages to add to in case of validation failure
     */
    private void validateBankingDetails(Long bankId,
                                        List<NomenclatureItemStatus> statuses,
                                        List<String> errorMessages) {
        log.debug("Validating banking details for bank ID: {} and statuses: {}", bankId, statuses);

        if (bankId != null) {
            if (!bankRepository.existsByIdAndStatusIn(bankId, statuses)) {
                log.error("basicParameters.bankingDetails.bankId-Unable to find bank with ID %s in statuses %s;"
                        .formatted(bankId, statuses));
                errorMessages.add("basicParameters.bankingDetails.bankId-Unable to find bank with ID %s in statuses %s;"
                        .formatted(bankId, statuses));
            }
        }
    }


    /**
     * Validates that the interest rate with the given ID exists and is in one of the given statuses.
     *
     * @param interestRateId ID of the interest rate to validate
     * @param statuses       List of statuses to check
     * @param errorMessages  List of error messages to add to in case of validation failure
     */
    private void validateApplicableInterestRate(Long interestRateId,
                                                List<InterestRateStatus> statuses,
                                                List<String> errorMessages) {
        log.debug("Validating applicable interest rate for interest rate ID: {} and statuses: {}", interestRateId, statuses);

        if (!interestRateRepository.existsByIdAndStatusIn(interestRateId, statuses)) {
            log.error("basicParameters.interestRateId-Unable to find interest rate with ID %s in statuses %s;"
                    .formatted(interestRateId, statuses));
            errorMessages.add("basicParameters.interestRateId-Unable to find interest rate with ID %s in statuses %s;"
                    .formatted(interestRateId, statuses));
        }
    }


    /**
     * Validates that the campaign with the given ID exists and is in one of the given statuses.
     *
     * @param campaignId    ID of the campaign to validate
     * @param statuses      List of statuses to check
     * @param errorMessages List of error messages to add to in case of validation failure
     */
    private void validateCampaign(Long campaignId,
                                  List<NomenclatureItemStatus> statuses,
                                  List<String> errorMessages) {
        log.debug("Validating campaign with ID {} and statuses {}", campaignId, statuses);

        if (campaignId == null) {
            return;
        }

        if (!campaignRepository.existsByIdAndStatusIn(campaignId, statuses)) {
            log.error("basicParameters.campaignId-Unable to find campaign with ID %s in statuses %s;"
                    .formatted(campaignId, statuses));
            errorMessages.add("basicParameters.campaignId-Unable to find campaign with ID %s in statuses %s;"
                    .formatted(campaignId, statuses));
        }
    }


    /**
     * Creates the sub objects related to the basic parameters and adds them to the service order.
     *
     * @param request       The request containing the parameters for the service order.
     * @param serviceOrder  The service order to which the sub objects will be added.
     * @param errorMessages The list of error messages to be populated in case of validation errors.
     */
    public void createBasicParametersSubObjects(ServiceOrderBasicParametersCreateRequest request,
                                                ServiceOrder serviceOrder,
                                                List<String> errorMessages) {
        createInternalIntermediaries(request.getInternalIntermediaries(), serviceOrder, errorMessages);
        createExternalIntermediaries(request.getExternalIntermediaries(), serviceOrder, errorMessages);
        createAssistingEmployees(request.getAssistingEmployees(), serviceOrder, errorMessages);
        createProxies(request, serviceOrder, errorMessages);
        relatedContractsAndOrdersService.createEntityRelations(
                serviceOrder.getId(),
                RelatedEntityType.SERVICE_ORDER,
                request.getRelatedEntities(),
                errorMessages
        );
    }


    /**
     * Creates proxies and adds them to the service order.
     *
     * @param request       The request containing the parameters for the service order.
     * @param serviceOrder  The service order to which the sub objects will be added.
     * @param errorMessages The list of error messages to be populated in case of validation errors.
     */
    private void createProxies(ServiceOrderBasicParametersCreateRequest request, ServiceOrder serviceOrder, List<String> errorMessages) {
        log.debug("Creating proxies for service order: {}", serviceOrder.getId());

        if (CollectionUtils.isEmpty(request.getProxies())) {
            return; // it's an optional field
        }

        Optional<Customer> customerOptional = customerRepository
                .findByCustomerDetailIdAndStatusIn(
                        request.getCustomerDetailId(),
                        List.of(CustomerStatus.ACTIVE)
                );

        if (customerOptional.isEmpty()) {
            log.error("basicParameters.customerDetailId-Customer not found for customer version with ID {}", request.getCustomerDetailId());
            errorMessages.add("basicParameters.customerDetailId-Customer not found for customer version.");
            return;
        }

        validateProxiesForCustomerType(customerOptional.get().getCustomerType(), request.getProxies(), errorMessages);

        List<ServiceOrderProxyBaseRequest> proxies = request.getProxies();
        for (int i = 0; i < proxies.size(); i++) {
            ServiceOrderProxyBaseRequest proxyCreateRequest = proxies.get(i);
            if (!customerOptional.get().getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)
                    && CollectionUtils.isEmpty(proxyCreateRequest.getManagers())) {
                log.error("basicParameters.proxies[%s]-Unable to create proxy without managers for non-private customer.".formatted(i));
                errorMessages.add("basicParameters.proxies[%s]-Unable to create proxy without managers for non-private customer.".formatted(i));
                continue;
            }

            createProxy(request.getCustomerDetailId(), serviceOrder, errorMessages, proxyCreateRequest);
        }
    }


    /**
     * Creates proxy together with its managers and files.
     *
     * @param customerDetailId   The Id of the customer detail, for which the service order is being created.
     * @param serviceOrder       The service order to which the sub objects will be added.
     * @param errorMessages      The list of error messages to be populated in case of validation errors.
     * @param proxyCreateRequest The request containing the parameters for the proxy.
     */
    private void createProxy(Long customerDetailId, ServiceOrder serviceOrder, List<String> errorMessages, ServiceOrderProxyBaseRequest proxyCreateRequest) {
        ServiceOrderProxy orderProxy = createServiceOrderProxy(serviceOrder, proxyCreateRequest, errorMessages);
        createProxyManagers(customerDetailId, errorMessages, proxyCreateRequest, orderProxy);
        serviceOrderProxyFileService.attachFilesToServiceOrderProxy(proxyCreateRequest.getFiles(), orderProxy.getId(), errorMessages);
    }


    /**
     * Validates the proxies against the conditions.
     *
     * @param customerType  The type of the customer for which the proxies are being created.
     * @param errorMessages The list of error messages to be populated in case of validation errors.
     */
    private void validateProxiesForCustomerType(CustomerType customerType, List<? extends ServiceOrderProxyBaseRequest> proxies, List<String> errorMessages) {
        if (customerType.equals(CustomerType.PRIVATE_CUSTOMER) && proxies.size() > 1) {
            log.error("basicParameters.proxies-It is not possible to add more than one proxy for private customer.");
            errorMessages.add("basicParameters.proxies-It is not possible to add more than one proxy for private customer.");
        }
    }


    /**
     * Creates a service order proxy entity.
     *
     * @param serviceOrder The service order to which the proxy will be added.
     * @param request      The request containing the parameters for the proxy.
     * @return The created service order proxy entity.
     */
    private ServiceOrderProxy createServiceOrderProxy(ServiceOrder serviceOrder, ServiceOrderProxyBaseRequest request, List<String> errorMessages) {
        ServiceOrderProxy orderProxy = ServiceOrderMapper.fromProxyCreateRequestToEntity(request, serviceOrder);
        return serviceOrderProxyRepository.saveAndFlush(orderProxy);
    }


    /**
     * Creates managers for the proxy.
     *
     * @param customerDetailId   The customer detail id for which the order is being created.
     * @param errorMessages      The list of error messages to be populated in case of validation errors.
     * @param proxyCreateRequest The request containing the parameters for the proxy.
     * @param orderProxy         The proxy to which the managers will be added.
     */
    private void createProxyManagers(Long customerDetailId,
                                     List<String> errorMessages,
                                     ServiceOrderProxyBaseRequest proxyCreateRequest,
                                     ServiceOrderProxy orderProxy) {
        log.debug("Creating proxy managers for proxy with ID: {}", orderProxy.getId());

        if (CollectionUtils.isEmpty(proxyCreateRequest.getManagers())) {
            return; // the field will be empty in case of private customers
        }

        List<Long> managers = proxyCreateRequest.getManagers();
        List<ServiceOrderProxyManager> tempList = new ArrayList<>();
        for (Long managerId : managers) {
            if (!customerManagerRepository.existsByIdAndCustomerDetailIdAndStatusIn(managerId, customerDetailId, List.of(Status.ACTIVE))) {
                log.debug("basicParameters.proxies.managers-Unable to find manager with ID %s for customer detail with ID %s;"
                        .formatted(managerId, customerDetailId));
                errorMessages.add("basicParameters.proxies.managers-Unable to find manager with ID %s for customer detail with ID %s;"
                        .formatted(managerId, customerDetailId));
                continue;
            }

            ServiceOrderProxyManager proxyManager = new ServiceOrderProxyManager();
            proxyManager.setCustomerManagerId(managerId);
            proxyManager.setOrderProxyId(orderProxy.getId());
            proxyManager.setStatus(EntityStatus.ACTIVE);
            tempList.add(proxyManager);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceOrderProxyManagerRepository.saveAll(tempList);
        }
    }


    public void updateBasicParametersSubObjects(ServiceOrderBasicParametersUpdateRequest request,
                                                ServiceOrder serviceOrder,
                                                List<String> errorMessages) {
        updateInternalIntermediaries(request.getInternalIntermediaries(), serviceOrder, errorMessages);
        updateExternalIntermediaries(request.getExternalIntermediaries(), serviceOrder, errorMessages);
        updateAssistingEmployees(request.getAssistingEmployees(), serviceOrder, errorMessages);
        updateProxies(request, serviceOrder, errorMessages);
        relatedContractsAndOrdersService.updateEntityRelations(
                serviceOrder.getId(),
                RelatedEntityType.SERVICE_ORDER,
                request.getRelatedEntities(),
                errorMessages
        );
    }


    private void updateProxies(ServiceOrderBasicParametersUpdateRequest request, ServiceOrder serviceOrder, List<String> errorMessages) {
        log.debug("Updating proxies for service order: {}", serviceOrder.getId());

        List<ServiceOrderProxy> persistedProxies = serviceOrderProxyRepository.findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        List<ServiceOrderProxyUpdateRequest> proxies = request.getProxies();

        if (proxies.isEmpty()) { // this means that user has deleted all proxies (if present)
            for (ServiceOrderProxy proxy : persistedProxies) {
                proxy.setStatus(EntityStatus.DELETED);
                serviceOrderProxyRepository.save(proxy);
            }
            return;
        }

        Optional<Customer> customerOptional = customerRepository
                .findByCustomerDetailIdAndStatusIn(
                        request.getCustomerDetailId(),
                        List.of(CustomerStatus.ACTIVE)
                );

        if (customerOptional.isEmpty()) {
            log.error("basicParameters.customerDetailId-Customer not found for customer version with ID {}", request.getCustomerDetailId());
            errorMessages.add("basicParameters.customerDetailId-Customer not found for customer version.");
            return;
        }

        validateProxiesForCustomerType(customerOptional.get().getCustomerType(), request.getProxies(), errorMessages);

        for (int i = 0; i < proxies.size(); i++) {
            ServiceOrderProxyUpdateRequest proxyRequest = proxies.get(i);
            if (!customerOptional.get().getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER) && CollectionUtils.isEmpty(proxyRequest.getManagers())) {
                log.error("basicParameters.proxies[%s]-Unable to process proxy without managers for non-private customer.".formatted(i));
                errorMessages.add("basicParameters.proxies[%s]-Unable to process proxy without managers for non-private customer.".formatted(i));
                continue;
            }

            if (proxyRequest.getId() == null) {
                createProxy(request.getCustomerDetailId(), serviceOrder, errorMessages, proxyRequest);
            } else {
                updateProxy(request.getCustomerDetailId(), persistedProxies, proxyRequest, i, errorMessages);
            }
        }

        List<Long> proxyIds = proxies
                .stream()
                .map(ServiceOrderProxyUpdateRequest::getId)
                .filter(Objects::nonNull)
                .toList();

        for (ServiceOrderProxy serviceOrderProxy : persistedProxies) {
            if (!proxyIds.contains(serviceOrderProxy.getId())) {
                serviceOrderProxy.setStatus(EntityStatus.DELETED);
                serviceOrderProxyRepository.save(serviceOrderProxy);
            }
        }
    }


    /**
     * Updates proxy entity with the given request.
     *
     * @param persistedProxies The list of persisted proxies.
     * @param request          The request containing the parameters for the proxy.
     * @param index            The index of the proxy in the list.
     * @param errorMessages    The list of error messages to be populated in case of validation errors.
     */
    private void updateProxy(Long customerDetailId,
                             List<ServiceOrderProxy> persistedProxies,
                             ServiceOrderProxyUpdateRequest request,
                             int index,
                             List<String> errorMessages) {
        Optional<ServiceOrderProxy> proxyOptional = persistedProxies
                .stream()
                .filter(p -> p.getId().equals(request.getId()))
                .findFirst();

        if (proxyOptional.isEmpty()) {
            log.error("basicParameters.proxies[%s]-Unable to find proxy with ID %s;".formatted(index, request.getId()));
            errorMessages.add("basicParameters.proxies[%s]-Unable to find proxy with ID %s;".formatted(index, request.getId()));
            return;
        }

        ServiceOrderProxy proxy = ServiceOrderMapper.fromProxyUpdateRequestToEntity(proxyOptional.get(), request);
        serviceOrderProxyRepository.save(proxy);
        updateProxyManagers(customerDetailId, errorMessages, request, proxy);
        serviceOrderProxyFileService.updateFiles(request.getFiles(), proxy.getId(), errorMessages);
    }


    /**
     * Updates proxy managers.
     *
     * @param customerDetailId The customer detail id for which the order is being created.
     * @param errorMessages    The list of error messages to be populated in case of validation errors.
     * @param proxyRequest     The request containing the parameters for the proxy.
     * @param proxy            The proxy to which the managers will be added.
     */
    private void updateProxyManagers(Long customerDetailId,
                                     List<String> errorMessages,
                                     ServiceOrderProxyUpdateRequest proxyRequest,
                                     ServiceOrderProxy proxy) {
        log.debug("Updating proxy managers for proxy with ID: {}", proxy.getId());

        List<ServiceOrderProxyManager> persistedManagers = serviceOrderProxyManagerRepository.findByOrderProxyIdAndStatusIn(proxy.getId(), List.of(EntityStatus.ACTIVE));
        List<Long> persistedManagerIds = persistedManagers
                .stream()
                .map(ServiceOrderProxyManager::getId)
                .toList();

        List<Long> managers = proxyRequest.getManagers();
        List<ServiceOrderProxyManager> tempList = new ArrayList<>();

        for (Long managerId : managers) {
            if (!customerManagerRepository.existsByIdAndCustomerDetailIdAndStatusIn(managerId, customerDetailId, List.of(Status.ACTIVE))) {
                log.debug("basicParameters.proxies.managers-Unable to find manager with ID %s for customer detail with ID %s;"
                        .formatted(managerId, customerDetailId));
                errorMessages.add("basicParameters.proxies.managers-Unable to find manager with ID %s for customer detail with ID %s;"
                        .formatted(managerId, customerDetailId));
                continue;
            }

            if (!persistedManagerIds.contains(managerId)) {
                ServiceOrderProxyManager proxyManager = new ServiceOrderProxyManager();
                proxyManager.setCustomerManagerId(managerId);
                proxyManager.setOrderProxyId(proxy.getId());
                proxyManager.setStatus(EntityStatus.ACTIVE);
                tempList.add(proxyManager);
            }
        }

        for (ServiceOrderProxyManager m : persistedManagers) {
            if (!managers.contains(m.getId())) {
                m.setStatus(EntityStatus.DELETED);
                tempList.add(m);
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceOrderProxyManagerRepository.saveAll(tempList);
        }
    }


    private void createInternalIntermediaries(List<Long> internalIntermediaries,
                                              ServiceOrder serviceOrder,
                                              List<String> errorMessages) {
        if (CollectionUtils.isEmpty(internalIntermediaries)) {
            return; // it's an optional field
        }

        log.debug("Creating internal intermediaries for service order: {}", internalIntermediaries);

        List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), internalIntermediaries);

        List<ServiceOrderInternalIntermediary> tempList = new ArrayList<>();
        for (int i = 0; i < internalIntermediaries.size(); i++) {
            Long internalIntermediary = internalIntermediaries.get(i);
            if (!systemUsers.contains(internalIntermediary)) {
                log.error("basicParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                errorMessages.add("basicParameters.internalIntermediaries[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, internalIntermediary, List.of(Status.ACTIVE)));
                continue;
            }

            createInternalIntermediary(serviceOrder, tempList, internalIntermediary);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            internalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Validates and updates internal intermediaries for the service order.
     *
     * @param internalIntermediaries list of internal intermediary IDs
     * @param serviceOrder           service order to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void updateInternalIntermediaries(List<Long> internalIntermediaries,
                                              ServiceOrder serviceOrder,
                                              List<String> errorMessages) {
        List<ServiceOrderInternalIntermediary> persistedInternalIntermediaries = internalIntermediaryRepository
                .findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(internalIntermediaries)) {
            if (CollectionUtils.isNotEmpty(persistedInternalIntermediaries)) {
                // user has removed all internal intermediaries, should set deleted status to them
                persistedInternalIntermediaries.forEach(contractInternalIntermediary -> contractInternalIntermediary.setStatus(EntityStatus.DELETED));
                internalIntermediaryRepository.saveAll(persistedInternalIntermediaries);
            }
            return;
        }

        List<ServiceOrderInternalIntermediary> tempList = new ArrayList<>();

        // at this moment we already know that internalIntermediaries list is not empty
        log.debug("Updating internal intermediaries for service order: {}", internalIntermediaries);

        if (CollectionUtils.isEmpty(persistedInternalIntermediaries)) {
            // user has added new internal intermediaries, should create them
            createInternalIntermediaries(internalIntermediaries, serviceOrder, errorMessages);
            return;
        } else {
            // user has modified (added/edited) internal intermediaries, should update them
            List<Long> persistedInternalIntermediaryIds = persistedInternalIntermediaries
                    .stream()
                    .map(ServiceOrderInternalIntermediary::getAccountManagerId)
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
                    createInternalIntermediary(serviceOrder, tempList, internalIntermediary);
                } else {
                    Optional<ServiceOrderInternalIntermediary> persistedInternalIntermediaryOptional = persistedInternalIntermediaries
                            .stream()
                            .filter(contractInternalIntermediary -> contractInternalIntermediary.getAccountManagerId().equals(internalIntermediary))
                            .findFirst();
                    if (persistedInternalIntermediaryOptional.isEmpty()) {
                        log.error("Unable to find persisted internal intermediary with ID %s".formatted(internalIntermediary));
                        errorMessages.add("Unable to find persisted internal intermediary with ID %s".formatted(internalIntermediary));
                    } else {
                        ServiceOrderInternalIntermediary serviceOrderInternalIntermediary = persistedInternalIntermediaryOptional.get();
                        serviceOrderInternalIntermediary.setAccountManagerId(internalIntermediary);
                        tempList.add(serviceOrderInternalIntermediary);
                    }
                }
            }

            // user has removed some internal intermediaries, should set deleted status to them
            for (ServiceOrderInternalIntermediary internalIntermediary : persistedInternalIntermediaries) {
                if (!internalIntermediaries.contains(internalIntermediary.getAccountManagerId())) {
                    internalIntermediary.setStatus(EntityStatus.DELETED);
                    tempList.add(internalIntermediary);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            internalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Creates a new internal intermediary for the given service order.
     *
     * @param serviceOrder         service order to be updated
     * @param tempList             list of internal intermediaries to be saved
     * @param internalIntermediary ID of the internal intermediary to be created
     */
    private void createInternalIntermediary(ServiceOrder serviceOrder, List<ServiceOrderInternalIntermediary> tempList, Long internalIntermediary) {
        ServiceOrderInternalIntermediary intermediary = new ServiceOrderInternalIntermediary();
        intermediary.setStatus(EntityStatus.ACTIVE);
        intermediary.setAccountManagerId(internalIntermediary);
        intermediary.setOrderId(serviceOrder.getId());
        tempList.add(intermediary);
    }


    /**
     * Validates and creates external intermediaries for the service order.
     *
     * @param externalIntermediaries list of external intermediary IDs
     * @param serviceOrder           service order to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void createExternalIntermediaries(List<Long> externalIntermediaries,
                                              ServiceOrder serviceOrder,
                                              List<String> errorMessages) {
        if (CollectionUtils.isEmpty(externalIntermediaries)) {
            // adding external intermediaries is not mandatory
            return;
        }

        log.debug("Creating external intermediaries for service order: {}", externalIntermediaries);
        List<ServiceOrderExternalIntermediary> tempList = new ArrayList<>();

        for (int i = 0; i < externalIntermediaries.size(); i++) {
            Long externalIntermediary = externalIntermediaries.get(i);
            if (!externalIntermediaryRepository.existsByIdAndStatusIn(externalIntermediary, List.of(ACTIVE))) {
                log.error("basicParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                        .formatted(i, externalIntermediary, List.of(ACTIVE)));
                errorMessages.add("basicParameters.externalIntermediaries[%s]-Unable to find external intermediary with ID %s in statuses %s;"
                        .formatted(i, externalIntermediary, List.of(ACTIVE)));
                continue;
            }

            createExternalIntermediary(serviceOrder, tempList, externalIntermediary);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceOrderExternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Validates and updates external intermediaries for the contract.
     *
     * @param externalIntermediaries list of external intermediary IDs
     * @param serviceOrder           service order object to be updated
     * @param errorMessages          list of error messages to be populated in case of validation errors
     */
    private void updateExternalIntermediaries(List<Long> externalIntermediaries,
                                              ServiceOrder serviceOrder,
                                              List<String> errorMessages) {
        List<ServiceOrderExternalIntermediary> persistedExternalIntermediaries = serviceOrderExternalIntermediaryRepository
                .findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(externalIntermediaries)) {
            if (CollectionUtils.isNotEmpty(persistedExternalIntermediaries)) {
                // user has removed all external intermediaries, should set deleted status to them
                persistedExternalIntermediaries.forEach(contractExternalIntermediary -> contractExternalIntermediary.setStatus(EntityStatus.DELETED));
                serviceOrderExternalIntermediaryRepository.saveAll(persistedExternalIntermediaries);
            }
            return;
        }

        List<ServiceOrderExternalIntermediary> tempList = new ArrayList<>();

        // at this moment we already know that external intermediaries are present in request
        log.debug("Updating external intermediaries for service order: {}", externalIntermediaries);

        if (CollectionUtils.isEmpty(persistedExternalIntermediaries)) {
            // user has added new external intermediaries, should create them
            createExternalIntermediaries(externalIntermediaries, serviceOrder, errorMessages);
            return;
        } else {
            // user has modified existing external intermediaries, should update them
            List<Long> persistedExternalIntermediaryIds = persistedExternalIntermediaries
                    .stream()
                    .map(ServiceOrderExternalIntermediary::getExternalIntermediaryId)
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

                    Optional<ServiceOrderExternalIntermediary> persistedExternalIntermediaryOptional = persistedExternalIntermediaries
                            .stream()
                            .filter(contractExternalIntermediary -> contractExternalIntermediary.getExternalIntermediaryId().equals(externalIntermediary))
                            .findFirst();
                    if (persistedExternalIntermediaryOptional.isEmpty()) {
                        log.error("basicParameters.externalIntermediaries[%s]-Unable to find persisted external intermediary with ID %s;"
                                .formatted(i, externalIntermediary));
                        errorMessages.add("basicParameters.externalIntermediaries[%s]-Unable to find persisted external intermediary with ID %s;"
                                .formatted(i, externalIntermediary));
                    } else {
                        ServiceOrderExternalIntermediary persistedExternalIntermediary = persistedExternalIntermediaryOptional.get();
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

                    createExternalIntermediary(serviceOrder, tempList, externalIntermediary);
                }
            }

            // if the external intermediary is not present in the request, we set its status to DELETED
            for (ServiceOrderExternalIntermediary externalIntermediary : persistedExternalIntermediaries) {
                if (!externalIntermediaries.contains(externalIntermediary.getExternalIntermediaryId())) {
                    externalIntermediary.setStatus(EntityStatus.DELETED);
                    tempList.add(externalIntermediary);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            serviceOrderExternalIntermediaryRepository.saveAll(tempList);
        }
    }


    /**
     * Creates an external intermediary for the given service order.
     *
     * @param serviceOrder         service order to be updated
     * @param tempList             list of external intermediaries to be saved
     * @param externalIntermediary ID of the external intermediary to be created
     */
    private void createExternalIntermediary(ServiceOrder serviceOrder,
                                            List<ServiceOrderExternalIntermediary> tempList,
                                            Long externalIntermediary) {
        ServiceOrderExternalIntermediary intermediary = new ServiceOrderExternalIntermediary();
        intermediary.setStatus(EntityStatus.ACTIVE);
        intermediary.setExternalIntermediaryId(externalIntermediary);
        intermediary.setOrderId(serviceOrder.getId());
        tempList.add(intermediary);
    }


    /**
     * Validates and creates assisting employees for the service order.
     *
     * @param assistingEmployees list of assisting employee IDs
     * @param serviceOrder       service order to be updated
     * @param errorMessages      list of error messages to be populated in case of validation errors
     */
    private void createAssistingEmployees(List<Long> assistingEmployees, ServiceOrder serviceOrder, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(assistingEmployees)) {
            // assisting employees are not mandatory
            return;
        }

        log.debug("Creating assisting employees for service order: {}", assistingEmployees);

        List<Long> systemUsers = accountManagerRepository.findByStatusInAndIdIn(List.of(Status.ACTIVE), assistingEmployees);

        List<ServiceOrderAssistingEmployee> tempList = new ArrayList<>();
        for (int i = 0; i < assistingEmployees.size(); i++) {
            Long assistingEmployee = assistingEmployees.get(i);
            if (!systemUsers.contains(assistingEmployee)) {
                log.error("basicParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                errorMessages.add("basicParameters.assistingEmployees[%s]-Unable to find system user with ID %s in statuses %s;"
                        .formatted(i, assistingEmployee, List.of(Status.ACTIVE)));
                continue;
            }

            createAssistingEmployee(assistingEmployee, serviceOrder, tempList);
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            assistingEmployeeRepository.saveAll(tempList);
        }
    }


    /**
     * Validates and updates assisting employees for the service order.
     *
     * @param assistingEmployees list of assisting employee IDs
     * @param serviceOrder       service order object to be updated
     * @param errorMessages      list of error messages to be populated in case of validation errors
     */
    private void updateAssistingEmployees(List<Long> assistingEmployees,
                                          ServiceOrder serviceOrder,
                                          List<String> errorMessages) {
        List<ServiceOrderAssistingEmployee> persistedAssistingEmployees = assistingEmployeeRepository
                .findByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));

        if (CollectionUtils.isEmpty(assistingEmployees)) {
            if (CollectionUtils.isNotEmpty(persistedAssistingEmployees)) {
                // user has removed all assisting employees, should set deleted status to them
                persistedAssistingEmployees.forEach(assistingEmployee -> assistingEmployee.setStatus(EntityStatus.DELETED));
                assistingEmployeeRepository.saveAll(persistedAssistingEmployees);
            }
            return;
        }

        List<ServiceOrderAssistingEmployee> tempList = new ArrayList<>();

        // at this moment we already know that assisting employees list is not empty
        log.debug("Updating assisting employees for service order: {}", assistingEmployees);

        if (CollectionUtils.isEmpty(persistedAssistingEmployees)) {
            // user has added new assisting employees, should create them
            createAssistingEmployees(assistingEmployees, serviceOrder, errorMessages);
            return;
        } else {
            // user has modified (added/edited) assisting employees, should update them
            List<Long> persistedAssistingEmployeeIds = persistedAssistingEmployees
                    .stream()
                    .map(ServiceOrderAssistingEmployee::getAccountManagerId)
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
                    createAssistingEmployee(assistingEmployee, serviceOrder, tempList);
                } else {
                    Optional<ServiceOrderAssistingEmployee> persistedAssistingEmployeeOptional = persistedAssistingEmployees
                            .stream()
                            .filter(assistingEmployee1 -> assistingEmployee1.getAccountManagerId().equals(assistingEmployee))
                            .findFirst();

                    if (persistedAssistingEmployeeOptional.isEmpty()) {
                        log.error("Unable to find persisted internal intermediary with ID %s".formatted(assistingEmployee));
                        errorMessages.add("Unable to find persisted internal intermediary with ID %s".formatted(assistingEmployee));
                    } else {
                        ServiceOrderAssistingEmployee persistedAssistingEmployee = persistedAssistingEmployeeOptional.get();
                        persistedAssistingEmployee.setAccountManagerId(assistingEmployee);
                        tempList.add(persistedAssistingEmployee);
                    }
                }
            }

            // user has removed some assisting employees, should set deleted status to them
            for (ServiceOrderAssistingEmployee assistingEmployee : persistedAssistingEmployees) {
                if (!assistingEmployees.contains(assistingEmployee.getAccountManagerId())) {
                    assistingEmployee.setStatus(EntityStatus.DELETED);
                    tempList.add(assistingEmployee);
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            assistingEmployeeRepository.saveAll(tempList);
        }
    }


    /**
     * Creates assisting employee for the service order.
     *
     * @param assistingEmployee assisting employee ID
     * @param serviceOrder      service order object to be updated
     * @param tempList          list of assisting employees to be populated
     */
    private void createAssistingEmployee(Long assistingEmployee, ServiceOrder serviceOrder, List<ServiceOrderAssistingEmployee> tempList) {
        ServiceOrderAssistingEmployee assistant = new ServiceOrderAssistingEmployee();
        assistant.setStatus(EntityStatus.ACTIVE);
        assistant.setAccountManagerId(assistingEmployee);
        assistant.setOrderId(serviceOrder.getId());
        tempList.add(assistant);
    }


    /**
     * Returns the basic parameters response of the service order.
     *
     * @param serviceOrder service order object
     * @return basic parameters response populated with the fields and sub object responses
     */
    public ServiceOrderBasicParametersResponse getBasicParametersResponse(ServiceOrder serviceOrder) {
        Long serviceOrderId = serviceOrder.getId();
        log.debug("Getting basic parameters response for service order with ID {}", serviceOrderId);
        ServiceOrderBasicParametersResponse basicParametersResponse = serviceOrderRepository.getBasicParametersByServiceOrder(serviceOrderId);
        basicParametersResponse.setEmployee(getEmployeeResponse(serviceOrder));
        basicParametersResponse.setActivities(serviceOrderActivityService.getActivitiesByConnectedObjectId(serviceOrderId));

        if (basicParametersResponse.getCustomerCommunicationIdForBilling() != null) {
            basicParametersResponse.setConcatPurposes(customerRepository.getConcatPurposeFromCustomerCommunicationData(basicParametersResponse.getCustomerCommunicationIdForBilling()));
        }

        // NOTE: id field in the following objects represents the account manager id (in internal intermediaries and assisting employees)
        // and external intermediary id (in external intermediaries), and not a db record id.
        basicParametersResponse.setInternalIntermediaries(internalIntermediaryRepository.getShortResponseByOrderIdAndStatusIn(serviceOrderId, List.of(EntityStatus.ACTIVE)));
        basicParametersResponse.setExternalIntermediaries(serviceOrderExternalIntermediaryRepository.getShortResponseByOrderIdAndStatusIn(serviceOrderId, List.of(EntityStatus.ACTIVE)));
        basicParametersResponse.setAssistingEmployees(assistingEmployeeRepository.getShortResponseByOrderIdAndStatusIn(serviceOrderId, List.of(EntityStatus.ACTIVE)));

        basicParametersResponse.setRelatedEntities(relatedContractsAndOrdersService.getRelatedEntities(serviceOrderId, RelatedEntityType.SERVICE_ORDER));
        basicParametersResponse.setProxies(getProxiesResponse(serviceOrder));
        basicParametersResponse.setTasks(taskService.getTasksByServiceOrderId(serviceOrderId));
        basicParametersResponse.setInvoices(invoiceRepository.findOrderInvoices(serviceOrderId, InvoiceStatus.REAL, OrderType.SERVICE_ORDER.name()));
        basicParametersResponse.setOrderInvoiceStatus(serviceOrder.getOrderInvoiceStatus());
        contractTemplateRepository.findTemplateResponseById(serviceOrder.getEmailTemplateId(),LocalDate.now())
                        .ifPresent(basicParametersResponse::setEmailTemplateResponse);
        contractTemplateRepository.findTemplateResponseById(serviceOrder.getInvoiceTemplateId(),LocalDate.now())
                .ifPresent(basicParametersResponse::setInvoiceTemplateResponse);
        return basicParametersResponse;
    }


    /**
     * Returns the proxies response of the service order.
     *
     * @param serviceOrder service order object
     * @return proxies response populated with the fields, managers and files
     */
    private List<ServiceOrderProxyBaseResponse> getProxiesResponse(ServiceOrder serviceOrder) {
        List<ServiceOrderProxyBaseResponse> proxies = serviceOrderProxyRepository.getByOrderIdAndStatusIn(serviceOrder.getId(), List.of(EntityStatus.ACTIVE));
        for (ServiceOrderProxyBaseResponse proxy : proxies) {
            proxy.setFiles(serviceOrderProxyFileService.getFilesByOrderProxyId(proxy.getId(), List.of(EntityStatus.ACTIVE)));
            List<ServiceOrderProxyManagerResponse> managers = serviceOrderProxyManagerRepository.getManagersByOrderProxyIdAndStatusIn(proxy.getId(), List.of(EntityStatus.ACTIVE));
            proxy.setManagers(managers);
            proxy.setName(
                    "%s%s (%s)".formatted(
                            proxy.getProxy().getName(),
                            StringUtils.isEmpty(proxy.getAuthorizedProxy().getName()) ? "" : " - " + proxy.getAuthorizedProxy().getName(),
                            String.join(", ", managers.stream().map(ServiceOrderProxyManagerResponse::getManagerName).toList())
                    )
            );
        }
        return proxies;
    }


    /**
     * Returns the creator employee response of the service order.
     *
     * @param serviceOrder service order object
     * @return creator employee response populated with the fields
     */
    private ServiceOrderSubObjectShortResponse getEmployeeResponse(ServiceOrder serviceOrder) {
        AccountManager employee = accountManagerRepository
                .findById(serviceOrder.getEmployeeId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Unable to find employee with ID %s".formatted(serviceOrder.getEmployeeId())));
        ServiceOrderSubObjectShortResponse employeeResponse = new ServiceOrderSubObjectShortResponse();
        employeeResponse.setId(employee.getId());
        employeeResponse.setName("%s (%s)".formatted(employee.getDisplayName(), employee.getUserName()));
        return employeeResponse;
    }


    /**
     * Validates basic parameters on update.
     *
     * @param serviceOrder    service order object
     * @param basicParameters basic parameters request containing the parameters to be validated
     * @param errorMessages   list of error messages to be populated in case of validation errors
     */
    public void validateBasicParametersOnUpdate(ServiceOrder serviceOrder, ServiceOrderBasicParametersUpdateRequest basicParameters, List<String> errorMessages) {
        log.debug("Validating basic parameters on update for service order with ID {}", serviceOrder.getId());
        validateCustomerAndServiceCompatibility(basicParameters, errorMessages, serviceOrder.getId());
        validateCustomerBillingCommunication(basicParameters, errorMessages);
        validateCampaign(
                basicParameters.getCampaignId(),
                !Objects.equals(serviceOrder.getCampaignId(), basicParameters.getCampaignId()) ? List.of(ACTIVE) : List.of(ACTIVE, INACTIVE),
                errorMessages
        );
        validateBankingDetails(
                basicParameters.getBankingDetails().getBankId(),
                !Objects.equals(serviceOrder.getBankId(), basicParameters.getBankingDetails().getBankId()) ? List.of(ACTIVE) : List.of(ACTIVE, INACTIVE),
                errorMessages
        );
        validateApplicableInterestRate(basicParameters.getInterestRateId(), List.of(InterestRateStatus.ACTIVE), errorMessages);
        validateEmployeeOnUpdate(basicParameters.getEmployeeId(), errorMessages);
        validateAssistingEmployees(basicParameters.getEmployeeId(), basicParameters.getAssistingEmployees(), errorMessages);
        validateContractTemplates(basicParameters,errorMessages);
    }


    /**
     * Processes employee field on update to be able to change the creator user of the order.
     *
     * @param employeeId    ID of the employee to be set
     * @param errorMessages list of error messages to be populated in case of validation errors
     */
    private void validateEmployeeOnUpdate(Long employeeId, List<String> errorMessages) {
        if (employeeId == null) {
            log.error("basicParameters.employeeId-Employee ID is mandatory;");
            errorMessages.add("basicParameters.employeeId-Employee ID is mandatory;");
            return;
        }

        if (!accountManagerRepository.existsByIdAndStatusIn(employeeId, List.of(Status.ACTIVE))) {
            log.error("basicParameters.employeeId-Unable to find employee with ID %s in statuses %s;".formatted(employeeId, List.of(Status.ACTIVE)));
            errorMessages.add("goodsOrder.employeeId-Unable to find employee with ID %s in statuses %s;".formatted(employeeId, List.of(Status.ACTIVE)));
        }
    }

    private void validateAssistingEmployees(Long employeeId, List<Long> assistingEmployees, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(assistingEmployees) && assistingEmployees.contains(employeeId)) {
            errorMessages.add("basicParameters.assistingEmployees-Assisting employee should not match employee;");
        }
    }

}

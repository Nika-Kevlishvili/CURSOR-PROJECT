package bg.energo.phoenix.service.customer;

import bg.energo.phoenix.apis.model.ApisCustomer;
import bg.energo.phoenix.apis.model.CustomerCheckRequest;
import bg.energo.phoenix.apis.model.CustomerCheckResponse;
import bg.energo.phoenix.apis.service.ApisService;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.CustomerPreference;
import bg.energo.phoenix.model.entity.customer.CustomerSegment;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.customer.CommunicationDataType;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.filter.*;
import bg.energo.phoenix.model.enums.customer.list.*;
import bg.energo.phoenix.model.enums.customer.list.customerRelatedRelationship.CustomerRelatedRelationshipSearchField;
import bg.energo.phoenix.model.enums.customer.list.customerRelatedRelationship.CustomerRelatedRelationshipSortField;
import bg.energo.phoenix.model.enums.customer.list.customerRelatedRelationship.CustomerRelationshipKindOfCommunication;
import bg.energo.phoenix.model.enums.customer.list.customerRelatedRelationship.KindOfCommunicationsForRelationship;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.shared.InvoicedOptions;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractEditRequest;
import bg.energo.phoenix.model.request.customer.*;
import bg.energo.phoenix.model.request.customer.customerAccountManager.CreateCustomerAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.customerAccountManager.EditCustomerAccountManagerRequest;
import bg.energo.phoenix.model.request.customer.list.*;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityAndReceivableListColumns;
import bg.energo.phoenix.model.request.receivable.payment.PaymentListColumns;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.contract.express.ExpressContractCustomerShortResponse;
import bg.energo.phoenix.model.response.contract.productContract.CustomerVersionedResponse;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractResponse;
import bg.energo.phoenix.model.response.customer.*;
import bg.energo.phoenix.model.response.customer.communicationData.CustomerCommunicationEmailDataResponse;
import bg.energo.phoenix.model.response.customer.communicationData.CustomerCommunicationMobileDataResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.ContactPurposeMiddleResponse;
import bg.energo.phoenix.model.response.customer.communicationData.detailed.CustomerCommunicationsDetailedResponse;
import bg.energo.phoenix.model.response.customer.customerAccountManager.CustomerAccountManagerResponse;
import bg.energo.phoenix.model.response.customer.customerRelated.relationship.CustomerRelatedRelationshipResponse;
import bg.energo.phoenix.model.response.customer.list.CustomerRelatedContractListResponse;
import bg.energo.phoenix.model.response.customer.list.CustomerRelatedOrderListResponse;
import bg.energo.phoenix.model.response.customer.manager.ManagerBasicInfo;
import bg.energo.phoenix.model.response.customer.manager.ManagerResponse;
import bg.energo.phoenix.model.response.customer.owner.CustomerOwnerDetailResponse;
import bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityAndReceivableListingMiddleResponse;
import bg.energo.phoenix.model.response.receivable.customerLiability.CustomerLiabilityAndReceivableListingResponse;
import bg.energo.phoenix.model.response.receivable.customerReceivable.CustomerReceivableListingResponse;
import bg.energo.phoenix.model.response.receivable.payment.PaymentListResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.customerReceivables.CustomerReceivableRepository;
import bg.energo.phoenix.repository.receivable.payment.PaymentRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.contract.product.ProductContractService;
import bg.energo.phoenix.service.contract.service.ServiceContractService;
import bg.energo.phoenix.service.customer.activity.CustomerActivityService;
import bg.energo.phoenix.service.customer.customerCommunications.CustomerCommunicationsService;
import bg.energo.phoenix.service.nomenclature.customer.EconomicBranchCIService;
import bg.energo.phoenix.service.nomenclature.customer.LegalFormService;
import bg.energo.phoenix.service.nomenclature.customer.OwnershipFormService;
import bg.energo.phoenix.service.receivable.customerLiability.CustomerLiabilityMapperService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.customer.CustomerType.LEGAL_ENTITY;
import static bg.energo.phoenix.model.enums.customer.CustomerType.PRIVATE_CUSTOMER;
import static bg.energo.phoenix.model.enums.customer.filter.CustomerSearchFields.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.*;
import static bg.energo.phoenix.permissions.PermissionEnum.*;
import static bg.energo.phoenix.util.epb.EPBListUtils.convertEnumListIntoStringListIfNotNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final ManagerService managerService;
    private final RelatedCustomerService relatedCustomerService;
    private final CustomerCommunicationsService customerCommunicationsService;
    private final ApisService apisApiService;
    private final CustomerOwnerService customerOwnerService;
    private final CustomerSegmentService customerSegmentService;
    private final CustomerPreferenceService customerPreferenceService;
    private final CustomerDetailsService customerDetailsService;
    private final LegalFormService legalFormService;
    private final OwnershipFormService ownershipFormService;
    private final EconomicBranchCIService economicBranchCIService;
    private final ConnectedGroupsService connectedGroupsService;
    private final CustomerAccountManagerService customerAccountManagerService;
    private final PermissionService permissionService;
    private final CustomerActivityService customerActivityService;
    private final TaskService taskService;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final ProductContractRepository productContractRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ProductContractService productContractService;
    private final ServiceContractService serviceContractService;
    private final CustomerMapperService customerMapperService;
    private final CustomerCommunicationsRepository customerCommunicationsRepository;
    private final CustomerReceivableRepository customerReceivableRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerLiabilityRepository customerLiabilityRepository;
    private final CustomerLiabilityMapperService customerLiabilityMapperService;

    /**
     * Retrieves customer, its details and sub objects by id and version, based on corresponding permissions,
     * and maps them to {@link CustomerViewResponse}.
     *
     * @param id      customer id
     * @param version version id
     * @return {@link CustomerViewResponse} object
     */
    public CustomerViewResponse view(Long id, Long version) {
        Customer customer = customerRepository
                .findByIdAndStatuses(id, getPermissionsOfCustomerStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer not found by ID: %s and statuses: %s".formatted(id, getPermissionsOfCustomerStatuses())));

        String managerUsername = getManager();
        if (StringUtils.isNotEmpty(managerUsername)) {
            if (!customerRepository.existsByManager(id, managerUsername)) {
                log.error("Manager {} does not have permission to see customer with id {}", managerUsername, id);
                throw new ClientException("id-You don't have access to see customer with ID %s;".formatted(id), ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }

        CustomerDetails customerDetails;
        if (version != null) {
            customerDetails = customerDetailsService
                    .findByCustomerIdAndVersionId(id, version)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-CustomerDetails with this version id not found;"));
        } else {
            customerDetails = customerDetailsService
                    .findFirstByCustomerId(id, Sort.by(Sort.Direction.DESC, "createDate"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer Details not found;"));
        }

        List<ManagerResponse> managers = managerService.getManagersByCustomerDetailsId(customerDetails.getId());
        List<RelatedCustomerResponse> relatedCustomers = relatedCustomerService.getRelatedCustomersByCustomerId(customer.getId());
        List<CustomerOwnerDetailResponse> owners = customerOwnerService.getOwnersByCustomerId(customer.getId());
        List<CustomerCommunicationsDetailedResponse> customerCommunications = customerCommunicationsService.getCustomerCommunicationsByCustomerDetailId(customerDetails.getId());
        if (customerCommunications != null) {
            fillCommunicationDataPurposeConcat(customerCommunications);
        }
        List<ConnectedGroupResponse> connectedGroupResponses = connectedGroupsService.listForCustomer(customer);
        List<CustomerAccountManagerResponse> customerAccountManagers = customerAccountManagerService.getCustomerAccountManagersByCustomerDetailsId(customerDetails.getId());
        List<SystemActivityShortResponse> activities = customerActivityService.getActivitiesByConnectedObjectId(id);
        List<TaskShortResponse> tasks = taskService.getTasksByCustomerId(id);

        CustomerViewResponse response = customerMapperService.getCustomerViewResponse(
                customer,
                customerDetails,
                managers,
                relatedCustomers,
                owners,
                customerCommunications,
                connectedGroupResponses,
                customerAccountManagers,
                activities,
                tasks
        );
        response.setLocked(customerDetailsService.checkForIfLockedForPreview(customerDetails));
        // modify response according to GDPR permissions
        CustomerViewResponse customerViewResponse = customerMapperService.modifyCustomerViewResponse(response);

        customerViewResponse.setIsAllowedToEditCustomer(assertIfAllowedToEditCustomer(customerDetails.getId()));
        return customerViewResponse;
    }

    private void fillCommunicationDataPurposeConcat(List<CustomerCommunicationsDetailedResponse> customerCommunications) {
        for (CustomerCommunicationsDetailedResponse resp : customerCommunications) {
            if (resp.getContactPurposes() != null) {
                resp.setConcatPurposes(String.join(",", resp.getContactPurposes().stream().map(model -> String.valueOf(model.getName())).toList()));
            }
        }
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request, Set<String> permissions) {
        List<String> exceptionMessages = new ArrayList<>();
        Customer customer = createCustomer(request, exceptionMessages);
        CustomerDetails customerDetails = customerDetailsService.createCustomerdetails(request, customer, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);

        if (customerDetails == null) {
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        }

        customerCommunicationsService.createCustomerCommunicationsData(request.getCommunicationData(), customerDetails, exceptionMessages);
        customerSegmentService.createCustomerSegment(request, customerDetails, List.of(NomenclatureItemStatus.ACTIVE), permissions, exceptionMessages, customerDetails.getStatus());
        customerPreferenceService.createCustomerPreference(request, customerDetails, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);

        customerOwnerService.saveCustomerOwner(request.getOwner(), customer, exceptionMessages);
        managerService.addManagers(request.getManagers(), customerDetails, exceptionMessages);
        relatedCustomerService.addRelatedCustomers(request.getRelatedCustomers(), customer, exceptionMessages);
        customerAccountManagerService.createCustomerAccountManagers(request.getAccountManagers(), customerDetails, exceptionMessages);

        throwExceptionIfRequired(exceptionMessages);

        CustomerResponse customerResponse = new CustomerResponse();
        if (customerDetails != null && customer != null) {
            customer.setLastCustomerDetailId(customerDetails.getId());
            customerRepository.save(customer);
            customerResponse = customerMapperService.createCustomerResponse(customer, customerDetails);
        }

        return customerResponse;
    }

    /**
     * <h1>Customer update</h1>
     * function updates customer,customer details and subObject infos.
     * firstly it select customer form the database, checks if it is not deleted , if not
     * it checks {@link EditCustomerRequest#getUpdateExistingVersion()} boolean param:
     * if it's true function will update version based on this parameter: {@link EditCustomerRequest#getCustomerDetailsVersion()}
     * if it's false function will find customer details version
     * then create new version of the customer details based on {@link EditCustomerRequest#getCustomerDetailsVersion()} parameter
     * also will update or create subObjects
     *
     * @param id      db id of the customer
     * @param request customer edit object
     * @return @return {@link CustomerResponse} object
     */
    @Transactional
    public CustomerResponse update(Long id, EditCustomerRequest request, Set<String> permissions) {
        log.debug("Editing customer: {}", request.toString());

        List<String> exceptionMessages = new ArrayList<>();
        CustomerResponse customerResponse = new CustomerResponse();
        Optional<Customer> customerOptional = customerRepository.findById(id);
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            if (customer.getStatus() != null && customer.getStatus().equals(CustomerStatus.DELETED)) {
                throw new ClientException("id-Can't edit deleted customer;", ErrorCode.APPLICATION_ERROR);
            }
            if (!customer.getIdentifier().equals(request.getCustomerIdentifier())) {
                exceptionMessages.add("customerIdentifier-Customer identifier should be matched previous with version identifier");
            }

            if (Boolean.TRUE.equals(customer.getIsHardCoded())) {
                log.error("id-Customer with ID %s is Hardcoded and can not be deleted;".formatted(id));
                throw new OperationNotAllowedException("id-Customer with ID %s is Hardcoded and can not be deleted;".formatted(id));
            }

            CustomerDetails updatedVersion;
            customer.setAdditionalInfo(request.getCustomerAdditionalInformation());
            if (request.getUpdateExistingVersion()) {
                // update existing version of the Customer Details
                updatedVersion = customerDetailsService.editCustomerDetails(customer, id, request, exceptionMessages);
                if (updatedVersion != null) {
                    customerRepository.save(customer);
                    customerCommunicationsService.editCustomerCommunicationsData(request.getCommunicationData(), updatedVersion, exceptionMessages);
                    // update existing versions of the sub objects
                    List<CustomerSegment> segments = customerSegmentService.editCustomerSegment(request, updatedVersion, permissions, exceptionMessages);
                    List<CustomerPreference> preferences = customerPreferenceService.editCustomerPreference(request, updatedVersion, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);
                    updatedVersion.setCustomerSegments(segments);
                    updatedVersion.setCustomerPreferences(preferences);
                    managerService.editManagers(request.getManagers(), updatedVersion, exceptionMessages);
                    relatedCustomerService.editRelatedCustomers(request.getRelatedCustomers(), customer, exceptionMessages);
                    customerOwnerService.editCustomerOwners(request.getOwner(), customer, exceptionMessages);
                    customerAccountManagerService.editCustomerAccountManagers(request.getAccountManagers(), updatedVersion, exceptionMessages);
                    throwExceptionIfRequired(exceptionMessages);
                    customerDetailsService.saveCustomerDetails(updatedVersion);
                    customerResponse = customerMapperService.createCustomerResponse(customer, updatedVersion);
                }
            } else {
                Optional<CustomerDetails> dbCustomerDetails = customerDetailsService.findByCustomerIdAndVersionId(customer.getId(), request.getCustomerDetailsVersion());
                if (dbCustomerDetails.isPresent()) {
                    CustomerDetails oldDetails = dbCustomerDetails.get();
                    CustomerDetails newVersion = customerDetailsService.createCustomerDetailsNewVersion(request, customer, oldDetails, exceptionMessages);
                    if (newVersion == null) {
                        throwExceptionIfRequired(exceptionMessages);
                        return customerResponse;
                    }
                    customer.setLastCustomerDetailId(newVersion.getId());
                    customerRepository.save(customer);

                    customerCommunicationsService.addCustomerCommunicationsToNewVersion(request.getCommunicationData(), newVersion, oldDetails, exceptionMessages);
                    //create new versions of sub objects for the new customerDetails
                    List<CustomerSegment> segments = customerSegmentService.createAndGetCustomerSegment(request, newVersion, oldDetails, permissions, exceptionMessages);
                    List<CustomerPreference> preferences = customerPreferenceService.createAndGetCustomerPreference(request.getBankingDetails().getPreferenceIds(), newVersion, oldDetails, exceptionMessages);
                    newVersion.setCustomerSegments(segments);
                    newVersion.setCustomerPreferences(preferences);
                    customerOwnerService.editCustomerOwners(request.getOwner(), customer, exceptionMessages);
                    managerService.addManagersToNewVersion(request.getManagers(), newVersion, exceptionMessages, oldDetails);
                    relatedCustomerService.editRelatedCustomers(request.getRelatedCustomers(), customer, exceptionMessages);
                    customerAccountManagerService.createCustomerAccountManagersForNewVersion(getAccountManagersRequests(request.getAccountManagers()), newVersion, oldDetails, exceptionMessages);
                    throwExceptionIfRequired(exceptionMessages);

                    if (request.getCustomerEditContractRequests() != null && !request.getCustomerEditContractRequests().isEmpty()) {
                        checkPermission(CUSTOMER_EDIT_AUTOMATIC_UPDATE);

                        List<Long> serviceContractsIds = request.getCustomerEditContractRequests().stream().filter(contract ->
                                contract.getContractType().equals(ContractType.SERVICE_CONTRACT)).map(CustomerEditContractRequest::getContractId).toList();
                        List<Long> productContractsIds = request.getCustomerEditContractRequests().stream().filter(contract ->
                                contract.getContractType().equals(ContractType.PRODUCT_CONTRACT)).map(CustomerEditContractRequest::getContractId).toList();

                        serviceContractDetailsRepository.getServiceContractsByCustomerDetailsId(serviceContractsIds, customer.getId()).forEach(serviceContractDetail -> serviceContractDetail.setCustomerDetailId(newVersion.getId()));
                        productContractDetailsRepository.getProductContractsByCustomerDetailsId(productContractsIds, customer.getId()).forEach(productContractDetail -> productContractDetail.setCustomerDetailId(newVersion.getId()));
                        LocalDate currentDate = LocalDate.now();

                        serviceContractDetailsRepository.findCurrentServiceContractDetailsByContractIds(serviceContractsIds, customer.getId()).forEach(currentServiceContractDetail -> {
                            if (!Objects.equals(currentServiceContractDetail.getStartDate(), currentDate)) {
                                ServiceContractResponse serviceContractResponse = serviceContractService.view(currentServiceContractDetail.getContractId(), currentServiceContractDetail.getVersionId());
                                ServiceContractEditRequest serviceContractEditRequest = customerMapperService.mapServiceContractEditRequest(serviceContractResponse, currentDate, newVersion.getId());
                                serviceContractService.update(serviceContractEditRequest, currentServiceContractDetail.getContractId(), currentServiceContractDetail.getVersionId(), false);
                            } else {
                                Optional<ServiceContractDetails> productContractDetails = serviceContractDetailsRepository.findById(currentServiceContractDetail.getContractDetailsId());
                                productContractDetails.ifPresent(contractDetails -> {
                                    contractDetails.setCustomerDetailId(newVersion.getId());
                                    serviceContractDetailsRepository.save(contractDetails);
                                });
                            }
                        });

                        productContractDetailsRepository.findCurrentProductContractDetailsByContractIds(productContractsIds, customer.getId()).forEach(currentProductContractDetail -> {
                            if (!Objects.equals(currentProductContractDetail.getStartDate(), currentDate)) {
                                ProductContractResponse productContractResponse = productContractService.get(currentProductContractDetail.getContractId(), currentProductContractDetail.getVersionId());
                                ProductContractUpdateRequest productContractUpdateRequest = customerMapperService.mapProductContractUpdateRequest(productContractResponse, currentDate, newVersion.getId());
                                productContractService.edit(
                                        currentProductContractDetail.getContractId(),
                                        currentProductContractDetail.getVersionId(),
                                        productContractUpdateRequest,
                                        false
                                );
                            } else {
                                Optional<ProductContractDetails> productContractDetails = productContractDetailsRepository.findById(currentProductContractDetail.getContractDetailsId());
                                productContractDetails.ifPresent(contractDetails -> {
                                    contractDetails.setCustomerDetailId(newVersion.getId());
                                    productContractDetailsRepository.save(contractDetails);
                                });
                            }
                        });
                    }
                    customerDetailsService.saveCustomerDetails(newVersion);
                    return customerMapperService.createCustomerResponse(customer, newVersion);
                } else {
                    log.error("CustomerDetails with version {} or customerId {} not found;", request.getCustomerDetailsVersion(), customer.getId());
                    exceptionMessages.add("CustomerDetails with version %s or customerId %s not found;".formatted(request.getCustomerDetailsVersion(), customer.getId()));
                }
            }
        } else {
            log.error("Customer with ID {} not found;", id);
            exceptionMessages.add("Customer with ID %s not found;".formatted(id));
        }

        throwExceptionIfRequired(exceptionMessages);

        return customerResponse;
    }

    /**
     * Retrieves customers optionally filtered by the search criteria.
     *
     * @param request request object containing the search criteria
     * @return page of {@link CustomerListingResponse} objects
     */
    public Page<CustomerListingResponse> list(GetCustomersListRequest request) {
        log.debug("Getting customers list: {}", request);

        List<CustomerType> customerTypes = getCustomerTypes(request);

        if (customerTypes.isEmpty()) {
            return new PageImpl<>(Collections.emptyList());
        } else {
            Page<CustomerListingResponse> returnList = customerRepository.filter(
                    EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                    getSearchField(request),
                    customerTypes,
                    getCustomerDetailStatuses(request),
                    request.getEconomicBranchCiIds(),
                    getUnwantedCustomerType(request),
                    request.getManagerIds(),
                    getFilterPopulatedPlace(request.getPopulatedPlace()),
                    checkDirectionForAccountManager(request),
                    getPermissionsOfCustomerStatuses(),
                    getManager(),
                    getCanEditCustomer(),
                    String.valueOf(request.isExcludePastVersion()),
                    PageRequest.of(
                            request.getPage(),
                            request.getSize(),
                            Sort.by(new Sort.Order(request.getColumnDirection(), checkSortField(request)))
                    )
            );
            return modifyCustomerListResponse(returnList);
        }
    }

    /**
     * Sets deleted status to customer if the following conditions are met:
     * <ul>
     *     <li>Customer is present and user has permission to proceed with the process</li>
     *     <li>Customer is not already deleted</li>
     *     <li>Customer is not connected to Discount</li>
     *     <li>Customer is not connected to Point of Delivery</li>
     * </ul>
     *
     * @param id Id of the customer to be deleted
     * @return {@link CustomerResponse} object
     */
    public CustomerResponse delete(Long id) {
        Customer customer = customerRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer not found by Id: %s;".formatted(id)));

        CustomerDetails customerDetails = customerDetailsService
                .findFirstByCustomerId(id, Sort.by(Sort.Direction.DESC, "createDate"))
                .orElseThrow(() -> new ClientException("id-Customer Details not found by customer Id: " + id + ";", ErrorCode.APPLICATION_ERROR));

        checkCustomerDeletePermission(customerDetails.getStatus());

        if (customer.getStatus().equals(CustomerStatus.DELETED)) {
            log.error("id-Customer with ID %s is already deleted;".formatted(id));
            throw new OperationNotAllowedException("id-Customer with ID %s is already deleted;".formatted(id));
        }

        if (Boolean.TRUE.equals(customer.getIsHardCoded())) {
            log.error("id-Customer with ID %s is Hardcoded and can not be deleted;".formatted(id));
            throw new OperationNotAllowedException("id-Customer with ID %s is Hardcoded and can not be deleted;".formatted(id));
        }

        if (customerRepository.isInGroupOfConnectedCustomers(id)) {
            log.error("id-You can't delete the Customer because it is in the group of connected Customers;");
            throw new OperationNotAllowedException("id-You can't delete the Customer because it is in the group of connected Customers;");
        }

        if (customerRepository.hasActiveConnectionToDiscount(id)) {
            log.error("id-You can’t delete the Customer with ID %s because it is connected to the Discount;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Customer because it is connected to the Discount;");
        }

        if (customerRepository.hasActiveConnectionToPointOfDelivery(id)) {
            log.error("id-You can’t delete the Customer with ID %s because it is connected to the Point of Delivery;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Customer because it is connected to the Point of Delivery;");
        }

        if (customerRepository.hasConnectionToActivity(id)) {
            log.error("You can't delete the Customer because it is connected to activity.");
            throw new OperationNotAllowedException("You can't delete the Customer because it is connected to activity.");
        }

        if (customerRepository.hasActiveConnectionToProductContract(id)) {
            log.error("You can't delete the Customer because it is connected to Product Contract.");
            throw new OperationNotAllowedException("You can't delete the Customer because it is connected to Product Contract.");
        }

        if (customerRepository.hasActiveConnectionToServiceContract(id)) {
            log.error("You can't delete the Customer because it is connected to Service Contract.");
            throw new OperationNotAllowedException("You can't delete the Customer because it is connected to Service Contract.");
        }

        if (customerRepository.hasActiveConnectionToServiceOrder(id)) {
            log.error("You can't delete the Customer because it is connected to Service Order.");
            throw new OperationNotAllowedException("You can't delete the Customer because it is connected to Service Order.");
        }

        if (customerRepository.hasActiveConnectionToGoodsOrder(id)) {
            log.error("You can't delete the Customer because it is connected to Goods Order.");
            throw new OperationNotAllowedException("You can't delete the Customer because it is connected to Goods Order.");
        }

        if (customerRepository.hasActiveConnectionToCustomerTask(id)) {
            log.error("You can't delete the Customer because it is connected to Task.");
            throw new OperationNotAllowedException("You can't delete the Customer because it is connected to Task.");
        }

        customer.setStatus(CustomerStatus.DELETED);
        customerRepository.save(customer);

        return new CustomerResponse(customer.getId());
    }

    private Customer createCustomer(
            CreateCustomerRequest request,
            List<String> exceptionMessages
    ) {
        Customer customer = new Customer();
        assignCustomerNumber(request, customer);
        String identifier = request.getCustomerIdentifier();
        checkCustomerExistence(identifier, exceptionMessages);
        checkCustomerExistenceInAPIS(request.getCustomerType(), request.getForeign(), identifier, exceptionMessages);
        customer.setIdentifier(identifier);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setCustomerType(request.getCustomerType());
        customer.setAdditionalInfo(request.getCustomerAdditionalInformation());
        if (exceptionMessages.isEmpty()) {
            return customerRepository.save(customer);
        }
        return null;
    }

    //TODO: change customer number assignation logic
    private void assignCustomerNumber(
            CreateCustomerRequest request,
            Customer customer
    ) {
        Random random = new Random();
        String generated = String.format("%08d", random.nextInt(100000000));

        if (request.getCustomerType() == CustomerType.PRIVATE_CUSTOMER && !request.getBusinessActivity()) {
            customer.setCustomerNumber(Long.parseLong("66" + generated));
        } else {
            customer.setCustomerNumber(Long.parseLong("60" + generated));
        }
    }

    private void checkCustomerExistence(
            String identifier,
            List<String> exceptionMessages
    ) {
        Optional<Customer> optionalCustomer = customerRepository
                .findFirstByIdentifierAndStatusIn(identifier, List.of(CustomerStatus.ACTIVE));
        if (optionalCustomer.isPresent()) {
            exceptionMessages.add("customerIdentifier-Customer with identifier already exists;");
        }
    }

    private void checkCustomerExistenceInAPIS(
            CustomerType customerType,
            boolean foreign,
            String identifier,
            List<String> exceptionMessages
    ) {
        if (customerType == LEGAL_ENTITY && !foreign) {
            CustomerCheckResponse customerCheckResponse = checkCustomer(new CustomerCheckRequest(List.of(identifier)));
            if (customerCheckResponse == null || customerCheckResponse.getCustomers() == null || customerCheckResponse.getCustomers().isEmpty()) {
                exceptionMessages.add("Customer does not exists in apis;");
            }
        }
    }

    private void throwExceptionIfRequired(List<String> exceptionMessages) {
        if (!exceptionMessages.isEmpty()) {
            log.error(StringUtils.join("; ", exceptionMessages));
            StringBuilder sb = new StringBuilder();

            for (String exceptionMessage : exceptionMessages) {
                if (!exceptionMessage.contains(EPBFinalFields.VALIDATION_MESSAGE_REMOVE_INDICATOR)) {
                    sb.append(exceptionMessage);
                }
            }

            if (sb.isEmpty()) {
                sb.append("Error: Process Failed");
            }

            throw new ClientException(sb.toString().trim(), ErrorCode.CONFLICT);
        }
    }

    /**
     * <h1>checkCustomer</h1>
     * function checks customer in apis api service and returns data about this customer
     *
     * @param customerCheckRequest {@link CustomerCheckRequest} object
     * @return {@link CustomerCheckResponse} object
     */
    public CustomerCheckResponse checkCustomer(CustomerCheckRequest customerCheckRequest) {
        CustomerCheckResponse customerCheckResponse = apisApiService.checkApisCustomersInfo(customerCheckRequest);
        List<ApisCustomer> customers = customerCheckResponse.getCustomers();
        return new CustomerCheckResponse(legalFormService.searchInDescriptions(customers));
    }

    /**
     * <h1>GetAccountManagerRequests</h1>
     * maps {@link EditCustomerAccountManagerRequest} to the {@link CreateCustomerAccountManagerRequest}
     *
     * @param accountManagerRequests List of {@link EditCustomerAccountManagerRequest}
     * @return list of {@link CreateCustomerAccountManagerRequest}
     */
    private List<CreateCustomerAccountManagerRequest> getAccountManagersRequests(List<EditCustomerAccountManagerRequest> accountManagerRequests) {
        return CreateCustomerAccountManagerRequest.getCreateCustomerAccountManagerRequests(accountManagerRequests);
    }

    private String getCanEditCustomer() {
        List<String> context = permissionService.getPermissionsFromContext(CUSTOMER);
        if (context.stream().anyMatch(x -> Objects.equals(CUSTOMER_EDIT_AM.getId(), x))) {
            return permissionService.getLoggedInUserId();
        }
        return null;
    }

    /**
     * <h1>Modify Customer List Response</h1>
     * function checks customer list array for GDPR permission and return masks or allows personal info to be seen by user
     *
     * @param responses list of {@link CustomerListingResponse} object
     * @return page list of {@link CustomerListingResponse} object
     */
    private Page<CustomerListingResponse> modifyCustomerListResponse(Page<CustomerListingResponse> responses) {
        if (customerMapperService.checkGdpr()) {
            return responses;
        }
        return responses.map(x -> {
            if (x.getCustomerType().equals(PRIVATE_CUSTOMER)) {
                x.setCustomerMiddleName(EPBFinalFields.GDPR);
                x.setCustomerLastName(EPBFinalFields.GDPR);
                x.setIdentifier(EPBFinalFields.GDPR);
                x.setCustomerName(EPBFinalFields.GDPR);
            }
            return x;
        });

    }

    /**
     * <h1>Customer Delete checkCustomerDeletePermission</h1>
     * function checks if authorized user has delete potential customer or customer delete basic permissions
     * delete process will continue or stop based on this permission
     *
     * @param status {@link CustomerDetailStatus} object
     */
    private void checkCustomerDeletePermission(CustomerDetailStatus status) {
        List<String> context = permissionService.getPermissionsFromContext(CUSTOMER);
        if (status.equals(CustomerDetailStatus.POTENTIAL) && !context.contains(CUSTOMER_DELETE_POTENTIAL.getId())) {
            throw new ClientException("You do not have enough permissions to delete potential customer;", ErrorCode.ACCESS_DENIED);
        } else if (context.contains(CUSTOMER_DELETE_BASIC.getId())) {
            return;
        }
        throw new ClientException("You can not perform this action", ErrorCode.ACCESS_DENIED);
    }

    /**
     * <h1>Get Permissions Of Customer Statuses</h1>
     * function returns customer statuses ,according to the authorized system user , for customer list filtration
     *
     * @return list of {@link CustomerStatus}
     */
    private List<CustomerStatus> getPermissionsOfCustomerStatuses() {
        List<CustomerStatus> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(CUSTOMER);
        Set<String> viewPermissions = Set.of(CUSTOMER_VIEW_BASIC.getId(), CUSTOMER_VIEW_BASIC_AM.getId(), CUSTOMER_VIEW_GDPR.getId(), CUSTOMER_VIEW_GDPR_AM.getId());
        if (context.contains(CUSTOMER_VIEW_DELETED.getId())) {
            statuses.add(CustomerStatus.DELETED);
        }
        if (context.stream().anyMatch(viewPermissions::contains)) {
            statuses.add(CustomerStatus.ACTIVE);
        }
        return statuses;
    }

    /**
     * <h1>Get Manager</h1>
     * function returns manager username if permissions are set
     *
     * @return string userName of a loggedIn user
     */
    private String getManager() {
        List<String> context = permissionService.getPermissionsFromContext(CUSTOMER);
        if (context.stream().anyMatch(x -> List.of(CUSTOMER_VIEW_GDPR_AM.getId(), CUSTOMER_VIEW_BASIC_AM.getId()).contains(x))) {
            return permissionService.getLoggedInUserId();
        }
        return null;
    }

    /**
     * <h1>Check Direction For Account Manager</h1>
     * if columns and directions is not null function returns value if direction
     * else return null
     *
     * @param request {@link GetCustomersListRequest} object
     * @return direction value
     */
    private String checkDirectionForAccountManager(GetCustomersListRequest request) {
        CustomerListColumns columns = request.getCustomerListColumns();
        String direction = null;
        if (columns != null) {
            if (columns.equals(CustomerListColumns.ACCOUNT_MANAGER)) {
                if (request.getColumnDirection() != null) {
                    direction = request.getColumnDirection().name();
                }
            }
        }
        return direction;
    }

    /**
     * <h1>Get Filter Populated Place</h1>
     * if populated place is not null or blank function transforms string into
     * lowercase value an returns it "%string%" format
     * else returns null
     *
     * @param populatedPlace string value
     * @return string value
     */
    private String getFilterPopulatedPlace(String populatedPlace) {
        StringBuilder prompt = new StringBuilder("%");
        if (StringUtils.isNotBlank(populatedPlace)) {
            prompt.append(populatedPlace.toLowerCase());
        } else return null;
        prompt.append("%");
        return prompt.toString();
    }

    /**
     * <h1>Get Unwanted Customer Type</h1>
     * if unwantedCustomerStatus is not set function returns ALL by default
     * else returns the value
     *
     * @param request {@link GetCustomersListRequest} object
     * @return String type
     */
    private String getUnwantedCustomerType(GetCustomersListRequest request) {
        if (request.getUnwantedCustomerStatus() != null) {
            return request.getUnwantedCustomerStatus().getValue();
        } else {
            return UnwantedCustomerListingStatus.ALL.getValue();
        }
    }

    /**
     * <h1>Get customer types</h1>
     * if request customer type object is not null and has value of ALL
     * function returns {@link CustomerType} all values
     * else returns the value type
     * function also checks filters:
     * PERSONAL NUMBER -> ADD FILTER  PRIVATE_CUSTOMER and PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY
     * ID -> ADD FILTER LEGAL_ENTITY
     * NAME_LEGAL_ENTITY -> ADD FILTER LEGAL_ENTITY
     * NAME -> ADD FILTER  PRIVATE_CUSTOMER and PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY
     *
     * @param request {@link GetCustomersListRequest} object
     * @return list of {@link CustomerType}
     */
    private List<CustomerType> getCustomerTypes(GetCustomersListRequest request) {
        List<CustomerType> types = null;
        if (request.getCustomerTypeFilter() == null) {
            request.setCustomerTypeFilter(List.of(CustomerFilterType.ALL));
        }
        if (request.getCustomerTypeFilter() != null) {
            types = new ArrayList<>();
            for (CustomerFilterType item : request.getCustomerTypeFilter()) {
                if (item.equals(CustomerFilterType.ALL)) {
                    types = List.of(LEGAL_ENTITY,
                            CustomerType.PRIVATE_CUSTOMER);
                    break;
                } else {
                    types.add(CustomerType.valueOf(item.name()));
                }
            }
            CustomerSearchFields searchFields = request.getSearchFields();
            if (searchFields != null) {
                if (CollectionUtils.isNotEmpty(types)) {
                    if (searchFields.equals(ID) || searchFields.equals(NAME_LEGAL_ENTITY)) {
                        if (types.size() == 2) { //means both of the options are in the list
                            //types=types.remove(PRIVATE_CUSTOMER);  remove private customer form types
                            types = new ArrayList<>();
                            types = List.of(LEGAL_ENTITY);
                            return types;

                        }
                        if (types.size() == 1) {
                            if (types.get(0).equals(LEGAL_ENTITY)) {
                                //Thats ok
                                return types;
                            }
                            if (types.get(0).equals(PRIVATE_CUSTOMER)) {
                                //should return empty list
                                types = new ArrayList<>();
                                return types;
                            }
                        }

                    }
                    if (searchFields.equals(PERSONAL_NUMBER) || searchFields.equals(NAME) || searchFields.equals(MIDDLE_NAME) || searchFields.equals(LAST_NAME)) {
                        if (types.size() == 2) { //means both of the options are here
                            //types=types.remove(LEGAL_ENTITY);  remove LEGALENTITRY customer form types
                            types = List.of(PRIVATE_CUSTOMER);
                            return types;

                        }
                        if (types.get(0).equals(LEGAL_ENTITY)) {
                            //should return empty list
                            types = new ArrayList<>();
                            return types;

                        }
                        if (types.get(0).equals(PRIVATE_CUSTOMER)) {
                            //Thats ok
                            return types;
                        }
                    }
                } else {
                    if (searchFields.equals(ID) || searchFields.equals(NAME_LEGAL_ENTITY)) {
                        //if typs is null
                        types = new ArrayList<>();
                        types.add(LEGAL_ENTITY);
                        return types;

                    }
                    if (searchFields.equals(PERSONAL_NUMBER) || searchFields.equals(NAME)) {
                        types = new ArrayList<>();
                        types.add(PRIVATE_CUSTOMER);
                        return types;

                    }
                }
            }
        }
        return types;
    }

    /**
     * <h1>Get customer statuses</h1>
     * if request getCustomerStatusFilter is not null and has value of All
     * function returns all the {@link CustomerDetailStatus} enum values
     * else function returns values from getCustomerStatusFilter()
     *
     * @param request {@link GetCustomersListRequest} object
     * @return list of {@link CustomerDetailStatus}
     */
    private List<CustomerDetailStatus> getCustomerDetailStatuses(GetCustomersListRequest request) {
        List<CustomerDetailStatus> statuses = null;
        if (request.getCustomerStatusFilter() != null) {
            statuses = new ArrayList<>();
            for (CustomerFilterStatus item : request.getCustomerStatusFilter()) {
                if (item.equals(CustomerFilterStatus.ALL)) {
                    statuses = new ArrayList<>();
                    statuses = List.of(CustomerDetailStatus.POTENTIAL,
                            CustomerDetailStatus.NEW,
                            CustomerDetailStatus.ACTIVE,
                            CustomerDetailStatus.LOST,
                            CustomerDetailStatus.ENDED);
                    break;
                } else {
                    statuses.add(CustomerDetailStatus.valueOf(item.name()));
                }
            }

        }
        return statuses;
    }

    /**
     * <h1>Get Search Field</h1>
     * if request getSerachFields() is not null function returns value of {@link CustomerSearchFields}
     * else return ALL value by default
     *
     * @param request {@link GetCustomersListRequest} object
     * @return string value
     */
    private String getSearchField(GetCustomersListRequest request) {
        if (request.getSearchFields() != null) {
            return request.getSearchFields().getValue();
        } else {
            return CustomerSearchFields.ALL.getValue();
        }
    }

    /**
     * <h1>Check Sort Field</h1>
     * if getCustomerListColumns is null function returns ID value as default
     * else it returns {@link CustomerListColumns} enum value
     *
     * @param getCustomersListRequest {@link GetCustomersListRequest} object
     * @return string value
     */
    private String checkSortField(GetCustomersListRequest getCustomersListRequest) {
        if (getCustomersListRequest.getCustomerListColumns() == null) {
            return CustomerListColumns.ID.getValue();
        } else return getCustomersListRequest.getCustomerListColumns().getValue();
    }

    /**
     * Checks if user is allowed to edit customer details based on permissions.
     *
     * @param customerDetailId customer detail id to check if user is allowed to edit
     * @return true if user is allowed to edit customer, false otherwise
     */
    private boolean assertIfAllowedToEditCustomer(Long customerDetailId) {
        // if user has CUSTOMER_EDIT permission, return true (it's a superior permission)
        if (permissionService.permissionContextContainsPermissions(CUSTOMER, List.of(CUSTOMER_EDIT))) {
            return true;
        }

        // if user has CUSTOMER_EDIT_AM permission, check if he is an account manager of the customer
        return customerDetailsRepository.isManagerInCustomerAccountManagers(permissionService.getLoggedInUserId(), customerDetailId);
    }

    /**
     * <h1>FindCustomerInfo</h1>
     * function that Checks customer in database with personal number and returns short details
     *
     * @param identifier personal number of customer
     * @param types      list of {@link CustomerType} objects
     * @return {@link CustomerShortResponse}
     */
    //
    public CustomerShortResponse findCustomerInfo(
            String identifier,
            List<CustomerType> types
    ) {
        CustomerShortResponse customerInfo = customerDetailsService.findCustomerInfo(identifier, types);
        customerMapperService.modifyCustomerShortResponse(customerInfo);
        return customerInfo;
    }

    /**
     * Returns a list of customer's communication data records filtered by the contact purpose type: billing or contract.
     * The communication should contain an email and a phone number.
     *
     * @param request {@link CommunicationDataListRequest} object
     * @return list of {@link CustomerCommunicationDataResponse} objects
     */
    public List<CustomerCommunicationDataResponse> customerCommunicationDataList(CommunicationDataListRequest request) {
        log.debug("Retrieving communication data for customer details id: {} and data type: {}", request.getCustomerDetailsId(), request.getCommunicationDataType());
        List<CustomerCommunicationDataResponse> communicationDataResponseList = customerRepository.customerCommunicationDataList(
                request.getCustomerDetailsId(),
                request.getCommunicationDataType().equals(CommunicationDataType.BILLING)
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

    public List<CustomerCommunicationMobileDataResponse> customerCommunicationMobileDataList(Long customerDetailId) {
        var communications = customerCommunicationsRepository.findMobileContactByCommunicationId(customerDetailId);
        Map<Long, String> contactPurposeMap = customerRepository.getConcatPurposeFromCustomerCommunicationData(communications.stream().map(CustomerCommunicationDataResponse::getId).toList())
                .stream().collect(Collectors.toMap(ContactPurposeMiddleResponse::getCommunicationId, ContactPurposeMiddleResponse::getPurposes));
        for (CustomerCommunicationDataResponse comm : communications) {
            String purposes = contactPurposeMap.get(comm.getId());
            comm.setConcatPurposes(purposes);
        }
        return communications;
    }

    public List<CustomerCommunicationEmailDataResponse> customerCommunicationEmailDataList(Long customerDetailId) {
        List<CustomerCommunicationEmailDataResponse> unmodified = customerCommunicationsRepository
                .findEmailContactByCommunicationId(customerDetailId)
                .stream()
                .toList();

        Map<Long, String> contactPurposeMap = customerRepository
                .getConcatPurposeFromCustomerCommunicationData(
                        EPBListUtils.transform(
                                unmodified,
                                CustomerCommunicationDataResponse::getId
                        )
                )
                .stream()
                .collect(
                        Collectors.toMap(
                                ContactPurposeMiddleResponse::getCommunicationId,
                                ContactPurposeMiddleResponse::getPurposes
                        )
                );

        List<CustomerCommunicationEmailDataResponse> modified = unmodified
                .stream()
                .collect(Collectors.groupingBy(
                                CustomerCommunicationEmailDataResponse::getId,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            if (list.isEmpty()) {
                                                return null;
                                            }
                                            Long id = list.stream().findFirst().get().getId();
                                            String name = list.stream().findFirst().get().getName();
                                            LocalDateTime createDate = list.stream().findFirst().get().getCreateDate();
                                            String concatenatedEmails = list
                                                    .stream()
                                                    .map(CustomerCommunicationEmailDataResponse::getEmailAddress)
                                                    .collect(Collectors.joining(";"));
                                            return new CustomerCommunicationEmailDataResponse(id, name, createDate, concatenatedEmails);
                                        }
                                )
                        )
                )
                .values()
                .stream()
                .peek(comm -> comm.setConcatPurposes(contactPurposeMap.get(comm.getId())))
                .toList();

        return modified;
    }

    /**
     * Fetches all customer versions for a given customer identifier.
     * It is used in contracts and orders creation process.
     *
     * @param identifier customer identifier
     * @return last customer version along with all possible versions
     */
    public CustomerVersionedResponse findCustomerVersions(String identifier) {
        log.debug("Fetching customer versions for identifier {}", identifier);

        CustomerShortResponse customer = customerDetailsRepository
                .findFirstByCustomerIdentifierAndStatus(
                        identifier,
                        List.of(CustomerStatus.ACTIVE),
                        List.of(PRIVATE_CUSTOMER, LEGAL_ENTITY),
                        PageRequest.of(0, 1)
                )
                .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Customer not found with given identifier!;"));

        return new CustomerVersionedResponse(
                customer,
                customerDetailsRepository.findCustomerDetailsForCustomer(customer.getId())
        );
    }

    /**
     * Retrieves all activities for a customer
     *
     * @param id id of the customer to retrieve activities for
     * @return list of activities
     */
    public List<SystemActivityShortResponse> getActivitiesById(Long id) {
        return customerActivityService.getActivitiesByConnectedObjectId(id);
    }

    public ExpressContractCustomerShortResponse getCustomerShortResponse(
            CustomerDetails details,
            Customer customer
    ) {
        ExpressContractCustomerShortResponse response = new ExpressContractCustomerShortResponse();
        response.setId(customer.getId());
        response.setCustomerDetailId(details.getId());
        response.setIdentifier(customer.getIdentifier());
        response.setVatNumber(details.getVatNumber());
        response.setName(details.getName());
        response.setNameTransl(details.getNameTransl());
        response.setDetailStatus(details.getStatus());
        response.setCustomerStatus(customer.getStatus());
        response.setBusinessActivity(details.getBusinessActivity());
        response.setPreferCommunicationInEnglish(Boolean.TRUE.equals(details.getPreferCommunicationInEnglish()));
        response.setConsentToMarketingCommunication(Boolean.TRUE.equals(details.getMarketingCommConsent()));
        response.setPublicProcurementLaw(Boolean.TRUE.equals(details.getPublicProcurementLaw()));
        CustomerType customerType = customer.getCustomerType();
        response.setCustomerType(customerType);
        if (customerType.equals(CustomerType.PRIVATE_CUSTOMER)) {
            response.setMiddleName(details.getMiddleName());
            response.setMiddleNameTransl(details.getMiddleNameTransl());
            response.setLastName(details.getLastName());
            response.setLastNameTransl(details.getLastNameTransl());
            response.setBusinessActivityName(details.getBusinessActivityName());
            response.setBusinessActivityNameTransl(details.getBusinessActivityNameTransl());
        }
        if (customerType.equals(LEGAL_ENTITY) || Boolean.TRUE.equals(details.getBusinessActivity())) {
            response.setBusinessActivityName(details.getBusinessActivityName());
            response.setBusinessActivityNameTransl(details.getBusinessActivityNameTransl());
            response.setEconomicBranchCiId(economicBranchCIService.view(details.getEconomicBranchCiId()));
            response.setOwnershipFormId(ownershipFormService.view(details.getOwnershipFormId()));
            response.setMainActivitySubject(details.getMainActivitySubject());
            response.setManagers(managerService.getManagersByCustomerDetailsId(details.getId()));
            if (details.getLegalFormId() != null) {
                response.setLegalForm(legalFormService.view(details.getLegalFormId()));
            }
            if (details.getLegalFormTranslId() != null) {
                response.setLegalFormTranslId(legalFormService.getLegalFormTransliterated(details.getLegalFormTranslId()));
            }
        }
        response.setCustomerSegments(customerSegmentService.findSegmentsForCustomer(details.getId()));
        response.setCommunications(customerCommunicationsService.getCustomerCommunicationForExpress(details.getId()));
        response.setAddresses(customerMapperService.createCustomerAddressData(details));
        return response;
    }

    public List<ManagerBasicInfo> getManagersByCustomerDetailId(Long customerDetailId) {
        return managerService.getManagersByCustomerDetailId(customerDetailId);
    }

    public List<TaskShortResponse> getTasksById(Long id) {
        return taskService.getTasksByCustomerId(id);
    }

    /**
     * @param request {@link GetCustomersListRequest} containing search and filter parameters
     * @return list of product and service contracts related to the customer
     */
    public Page<CustomerRelatedContractListResponse> getCustomerRelatedContracts(
            Long customerDetailId,
            CustomerRelatedContractListRequest request
    ) {
        log.debug("Retrieving related contracts for customer detail with id: {}", customerDetailId);

        CustomerRelatedContractsTableColumn sortBy = Objects.requireNonNullElse(request.getSortBy(), CustomerRelatedContractsTableColumn.CREATION_DATE);
        CustomerRelatedContractsSearchField searchBy = Objects.requireNonNullElse(request.getSearchBy(), CustomerRelatedContractsSearchField.ALL);
        Sort.Direction sortDirection = Objects.requireNonNullElse(request.getSortDirection(), Sort.Direction.DESC);

        return customerDetailsRepository
                .getCustomerRelatedContracts(
                        customerDetailId,
                        getAllowedStatusesForViewingRelatedContracts(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        searchBy.name(),
                        ListUtils.emptyIfNull(request.getContractTypes()),
                        ListUtils.emptyIfNull(request.getContractStatuses()),
                        ListUtils.emptyIfNull(request.getContractSubStatuses()),
                        request.getSigningDateFrom(),
                        request.getSigningDateTo(),
                        request.getActivationDateFrom(),
                        request.getActivationDateTo(),
                        request.getContractTermEndDateFrom(),
                        request.getContractTermEndDateTo(),
                        request.getEntryIntoForceDateFrom(),
                        request.getEntryIntoForceDateTo(),
                        request.getCreationDateFrom(),
                        request.getCreationDateTo(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(sortDirection, sortBy.getValue())
                        )
                );
    }

    /**
     * @return list of statuses that the user is allowed to view related contracts for
     */
    private List<String> getAllowedStatusesForViewingRelatedContracts() {
        List<String> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(CUSTOMER, List.of(CUSTOMER_VIEW_RELATED_CONTRACTS))) {
            statuses.add("ACTIVE");
        }

        if (permissionService.permissionContextContainsPermissions(CUSTOMER, List.of(CUSTOMER_VIEW_DELETED_RELATED_CONTRACTS))) {
            statuses.add("DELETED");
        }
        return statuses;
    }

    /**
     * @param request {@link GetCustomersListRequest} containing search and filter parameters
     * @return list of product and service orders related to the customer
     */
    public Page<CustomerRelatedOrderListResponse> getCustomerRelatedOrders(
            Long customerDetailId,
            CustomerRelatedOrderListRequest request
    ) {
        log.debug("Retrieving related orders for customer detail with id: {}", customerDetailId);

        CustomerRelatedOrdersTableColumn sortBy = Objects.requireNonNullElse(request.getSortBy(), CustomerRelatedOrdersTableColumn.CREATION_DATE);
        CustomerRelatedOrdersSearchField searchBy = Objects.requireNonNullElse(request.getSearchBy(), CustomerRelatedOrdersSearchField.ALL);
        Sort.Direction sortDirection = Objects.requireNonNullElse(request.getSortDirection(), Sort.Direction.DESC);

        return customerDetailsRepository
                .getCustomerRelatedOrders(
                        customerDetailId,
                        getAllowedStatusesForViewingRelatedOrders(),
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        searchBy.name(),
                        ListUtils.emptyIfNull(request.getOrderTypes()),
                        ListUtils.emptyIfNull(request.getOrderStatuses()),
                        request.getInvoiceMaturityDateFrom(),
                        request.getInvoiceMaturityDateTo(),
                        request.getCreationDateFrom(),
                        request.getCreationDateTo(),
                        InvoicedOptions.fromOptions(request.getInvoicePaid()),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(sortDirection, sortBy.getValue())
                        )
                );
    }

    /**
     * @return list of statuses that the user is allowed to view related orders for
     */
    private List<String> getAllowedStatusesForViewingRelatedOrders() {
        List<String> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(CUSTOMER, List.of(CUSTOMER_VIEW_RELATED_ORDERS))) {
            statuses.add("ACTIVE");
        }

        if (permissionService.permissionContextContainsPermissions(CUSTOMER, List.of(CUSTOMER_VIEW_DELETED_RELATED_ORDERS))) {
            statuses.add("DELETED");
        }
        return statuses;
    }

    private void checkPermission(PermissionEnum permission) {
        if (!permissionService.getPermissionsFromContext(CUSTOMER).contains(permission.getId()))
            throw new ClientException("Can't update service without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
    }

    public Page<CustomerActiveContractResponse> getCustomerActiveContracts(
            CustomerActiveContractRequest customerActiveContractRequest
    ) {
        Long customerId = customerRepository.findCustomerIdByCustomerDetailIdAndStatusIn(customerActiveContractRequest.getCustomersDetailsId(), List.of(CustomerStatus.ACTIVE, CustomerStatus.DELETED))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Customer with customerDetailId: %s;".formatted(customerActiveContractRequest.getCustomersDetailsId())));

        return productContractRepository.findByCustomerId(
                customerId,
                PageRequest.of(customerActiveContractRequest.getPage(), customerActiveContractRequest.getSize())
        );
    }

    public Page<CustomerInvoicesResponse> getCustomerInvoices(
            Long customerDetailId,
            CustomerInvoicesRequest request
    ) {
        return invoiceRepository
                .getCustomerInvoicesByCustomerDetailId(
                        EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                        customerDetailId,
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.documentTypes()),
                        ListUtils.emptyIfNull(request.accountingPeriodIds()),
                        EPBStringUtils.fromPromptToQueryParameter(request.billingRun()),
                        request.dateOfInvoiceFrom(),
                        request.dateOfInvoiceTo(),
                        request.totalAmountFrom(),
                        request.totalAmountTo(),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.invoiceStatuses()),
                        request.searchBy() == null ? null : request.searchBy().name(),
                        PageRequest.of(request.page(), request.size(), extractSorting(request.direction(), request.sortBy()))
                ).map(CustomerInvoicesResponse::new);
    }

    public Sort extractSorting(Sort.Direction direction, CustomerInvoicesRequest.SortBy sortBy) {
        return Sort.by(Objects.requireNonNullElse(direction, Sort.Direction.DESC), Objects.requireNonNullElse(sortBy, CustomerInvoicesRequest.SortBy.ID).getColumnName());
    }

    /**
     * Retrieves a page of customer receivables based on the provided request parameters.
     *
     * @param customerDetailId the ID of the customer detail
     * @param request          the request parameters for filtering and sorting the customer receivables
     * @return a page of customer receivable listing responses
     */
    public Page<CustomerReceivableListingResponse> getCustomerReceivable(
            Long customerDetailId,
            CustomerRelatedReceivableListRequest request
    ) {
        List<EntityStatus> entityStatuses = new ArrayList<>();
        if (permissionCheck(CUSTOMER_RECEIVABLE_VIEW_DELETE)) {
            entityStatuses.add(EntityStatus.DELETED);
        }
        if (permissionCheck(CUSTOMER_RECEIVABLE_VIEW)) {
            entityStatuses.add(EntityStatus.ACTIVE);
        }
        boolean sizeOneAndZero = request.getCurrencyIds() != null && request.getCurrencyIds().size() == 1 && request.getCurrencyIds().get(0).equals(0L);

        return sizeOneAndZero ? Page.empty() : customerReceivableRepository.getCustomerRelatedReceivable(
                customerDetailId,
                request.getInitialAmountFrom(),
                request.getInitialAmountTo(),
                request.getCurrentAmountFrom(),
                request.getCurrentAmountTo(),
                request.getBlockedForOffsetting(),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchByEnum(request.getCustomerReceivableSearchBy()),
                ListUtils.emptyIfNull(request.getCurrencyIds()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(entityStatuses),
                request.getBillingGroup(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(
                                new Sort.Order(
                                        request.getDirection(),
                                        getSortByEnum(request.getColumns())
                                )
                        )
                )
        ).map(CustomerReceivableListingResponse::from);

    }

    private boolean permissionCheck(PermissionEnum permissionEnum) {
        return permissionService.getPermissionsFromContext(CUSTOMER_RECEIVABLE).contains(permissionEnum.getId());
    }

    private String getSearchByEnum(CustomerRelatedReceivableSearchField searchFields) {
        return searchFields != null ? searchFields.getValue() : CustomerRelatedReceivableSearchField.ALL.getValue();
    }

    private String getSortByEnum(CustomerRelatedReceivableSortColumns sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : CustomerRelatedReceivableSortColumns.ID.getValue();
    }

    /**
     * Retrieves a paginated list of payments for a given customer detail ID, based on the provided payment listing request.
     *
     * @param customerId The ID of the customer for which to retrieve the payments.
     * @param request    The request object containing the search criteria for the payment listing.
     * @return A page of {@link PaymentListResponse} objects representing the matching payments.
     */
    public Page<PaymentListResponse> getCustomerPayments(
            Long customerId,
            CustomerRelatedPaymentsListRequest request
    ) {
        List<EntityStatus> entityStatuses = new ArrayList<>();
        if (request.getCollectionChannelIds() != null) {
            request.getCollectionChannelIds().removeIf(Objects::isNull);
        }
        if (hasPermissionPayment(PermissionEnum.RECEIVABLE_PAYMENT_VIEW_DELETE)) {
            entityStatuses.add(EntityStatus.DELETED);
        }

        if (hasPermissionPayment(PermissionEnum.RECEIVABLE_PAYMENT_VIEW)) {
            entityStatuses.add(EntityStatus.ACTIVE);
        }

        return paymentRepository.getCustomerRelatedPayment(
                        customerId,
                        getSearchByEnumPayment(request.getSearchFields()),
                        request.getPrompt(),
                        convertEnumListIntoStringListIfNotNull(entityStatuses),
                        request.getInitialAmountFrom(),
                        request.getInitialAmountTo(),
                        request.getCurrentAmountFrom(),
                        request.getCurrentAmountTo(),
                        ListUtils.emptyIfNull(request.getCollectionChannelIds()),
                        request.getBlockedForOffsetting(),
                        request.getPaymentDateFrom(),
                        request.getPaymentDateTo(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(
                                        new Sort.Order(request.getDirection(), getSorByEnum(request.getColumns()))
                                )
                        )
                )
                .map(PaymentListResponse::new);
    }

    private String getSorByEnum(PaymentListColumns paymentListColumns) {
        return paymentListColumns != null ? paymentListColumns.getValue() : PaymentListColumns.PAYMENT_NUMBER.getValue();
    }

    private boolean hasPermissionPayment(PermissionEnum permission) {
        return permissionService.permissionContextContainsPermissions(PermissionContextEnum.RECEIVABLE_PAYMENT, List.of(permission));
    }

    private String getSearchByEnumPayment(CustomerRelatedPaymentSearchField paymentSearchFields) {
        return paymentSearchFields != null ? paymentSearchFields.getValue() : CustomerRelatedPaymentSearchField.ALL.getValue();
    }

    public CustomerLiabilityAndReceivableListingResponse getCustomerRelatedLiabilitiesAndReceivables(
            Long customerId,
            CustomerRelatedLiabilitiesAndReceivablesListRequest request
    ) {
        Page<CustomerLiabilityAndReceivableListingMiddleResponse> middleResponses = customerLiabilityRepository.getCustomerLiabilityAndReceivableListingMiddleResponse(
                customerId,
                request.getBlockedForPayments(),
                request.getBlockedForOffsetting(),
                request.getBlockedForReminders(),
                request.getBlockedForInterest(),
                request.getShowDeposits(),
                request.getBlockedForDisconnection(),
                request.getShowLiabilitiesAndReceivables(),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchByEnumLiability(request.getSearchFields()),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(
                                new Sort.Order(request.getDirection(), getSorByEnumLiability(request.getColumns()))
                        )
                )
        );
        return mapCustomerLiabilityAndReceivableListingPage(middleResponses);
    }

    private CustomerLiabilityAndReceivableListingResponse mapCustomerLiabilityAndReceivableListingPage(
            Page<CustomerLiabilityAndReceivableListingMiddleResponse> middlePage
    ) {
        BigDecimal totalInitialAmount = middlePage
                .getContent()
                .stream()
                .map(CustomerLiabilityAndReceivableListingMiddleResponse::getInitialAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCurrentAmount = middlePage
                .getContent()
                .stream()
                .map(CustomerLiabilityAndReceivableListingMiddleResponse::getCurrentAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CustomerLiabilityAndReceivableListingResponse(
                middlePage,
                totalInitialAmount,
                totalCurrentAmount
        );
    }

    private String getSearchByEnumLiability(CustomerRelatedLiabilityAndReceivableSearchField searchFields) {
        return searchFields != null ? searchFields.getValue() : CustomerRelatedLiabilityAndReceivableSearchField.ALL.getValue();
    }

    private String getSorByEnumLiability(CustomerLiabilityAndReceivableListColumns sortByColumn) {
        return sortByColumn != null ? sortByColumn.getValue() : CustomerLiabilityAndReceivableListColumns.DUE_DATE.getValue();
    }

    private boolean hasLiabilityViewPermission() {
        return permissionService.permissionContextContainsPermissions(CUSTOMER_LIABILITY, List.of(PermissionEnum.CUSTOMER_LIABILITY_VIEW));
    }

    private boolean hasLiabilityDeletedPermission() {
        return permissionService.permissionContextContainsPermissions(CUSTOMER_LIABILITY, List.of(PermissionEnum.CUSTOMER_LIABILITY_VIEW_DELETE));
    }

    public Page<CustomerRelatedRelationshipResponse> getCustomerRelationship(
            Long customerDetailId,
            CustomerRelatedRelationshipListRequest request
    ) {
        List<EntityStatus> entityStatuses = new ArrayList<>();
        if (hasCustomerRelationshipViewPermission() && hasCustomerRelationshipViewDeletePermission()) {
            entityStatuses = Arrays.asList(EntityStatus.values());
        } else if (hasLiabilityDeletedPermission()) {
            entityStatuses = List.of(EntityStatus.DELETED);
        } else if (hasLiabilityViewPermission()) {
            entityStatuses = List.of(EntityStatus.ACTIVE);
        }

        List<KindOfCommunicationsForRelationship> relationships = new ArrayList<>();
        if (request.getKindOfCommunications() != null) {
            List<CustomerRelationshipKindOfCommunication> kindEnums = request.getKindOfCommunications();
            for (CustomerRelationshipKindOfCommunication kindEnum : kindEnums) {
                if (kindEnum.equals(CustomerRelationshipKindOfCommunication.MASS)) {
                    relationships.add(KindOfCommunicationsForRelationship.MASS_EMAIL);
                    relationships.add(KindOfCommunicationsForRelationship.MASS_SMS);
                }
                if (kindEnum.equals(CustomerRelationshipKindOfCommunication.INDIVIDUAL)) {
                    relationships.add(KindOfCommunicationsForRelationship.SMS);
                    relationships.add(KindOfCommunicationsForRelationship.EMAIL);
                }
                if (kindEnum.equals(CustomerRelationshipKindOfCommunication.ALL)) {
                    relationships.addAll(List.of(KindOfCommunicationsForRelationship.values()));
                }
            }
        }

        return customerRepository.getCustomerRelatedCommunication(
                customerDetailId,
                EPBListUtils.convertEnumListIntoStringListIfNotNull(entityStatuses),
                ListUtils.emptyIfNull(request.getCreatorEmployee()),
                ListUtils.emptyIfNull(request.getSenderEmployee()),
                request.getDateFrom(),
                request.getDateTo(),
                ListUtils.emptyIfNull(request.getContactPurposes()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getCommunicationTypes()),
                ListUtils.emptyIfNull(request.getActivities()),
                ListUtils.emptyIfNull(request.getTasks()),
                ListUtils.emptyIfNull(request.getTopicOfCommunications()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getCommunicationStatuses()),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                Objects.requireNonNullElse(request.getSearchBy(), CustomerRelatedRelationshipSearchField.ALL).name(),
                Objects.requireNonNullElse(request.getActivityDirection(), Sort.Direction.ASC).name(),
                Objects.requireNonNullElse(request.getContactPurposeDirection(), Sort.Direction.ASC).name().toUpperCase(),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(relationships),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getRelationshipType()),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(
                                new Sort.Order(request.getSortingDirection(), getSortByEnumForRelationship(request.getSortBy()))
                        )
                )
        ).map(CustomerRelatedRelationshipResponse::new);

    }

    private String getSortByEnumForRelationship(CustomerRelatedRelationshipSortField sortBy) {
        return sortBy != null ? sortBy.getValue() : CustomerRelatedRelationshipSortField.MASS_OR_IND_COMMUNICATION_ID.getValue();
    }

    private boolean hasCustomerRelationshipViewPermission() {
        return permissionService.permissionContextContainsPermissions(CUSTOMER_RELATIONSHIP, List.of(CUSTOMER_RELATIONSHIP_VIEW));
    }

    private boolean hasCustomerRelationshipViewDeletePermission() {
        return permissionService.permissionContextContainsPermissions(CUSTOMER_RELATIONSHIP, List.of(CUSTOMER_RELATIONSHIP_VIEW_DELETED));
    }

    public List<ShortResponse> customerNumberList(String prompt, Integer page, Integer size) {
        return customerRepository.getCustomerNumbers(
                EPBStringUtils.fromPromptToQueryParameter(prompt),
                PageRequest.of(page, size)
        );
    }
}

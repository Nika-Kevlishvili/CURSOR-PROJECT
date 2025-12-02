package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.contract.service.*;
import bg.energo.phoenix.model.entity.customer.*;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommContactPurposes;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.contract.ContractVersionType;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.entity.product.service.ServiceContractServiceListingMiddleResponse;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceSegment;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroupTerms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerDetailStatus;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSaleMethod;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import bg.energo.phoenix.model.request.contract.ProxyAddRequest;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractBasicParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import bg.energo.phoenix.model.request.contract.service.ServiceContractBasicParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractBasicParametersResponse;
import bg.energo.phoenix.model.request.contract.service.ServiceContractCreateRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractBasicParametersEditRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractEditRequest;
import bg.energo.phoenix.model.request.product.service.ServiceContractProductListingRequest;
import bg.energo.phoenix.model.response.contract.ContractFileResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractContractVersionTypesResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.proxy.ProxyFilesResponse;
import bg.energo.phoenix.model.response.proxy.ProxyManagersResponse;
import bg.energo.phoenix.model.response.proxy.ProxyResponse;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.repository.contract.product.ProductContractSignableDocumentRepository;
import bg.energo.phoenix.repository.contract.service.*;
import bg.energo.phoenix.repository.customer.*;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommContactPurposesRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ContractVersionTypesRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.repository.product.service.ServiceSegmentsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupTermsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.contract.relatedEntities.RelatedContractsAndOrdersService;
import bg.energo.phoenix.service.customer.UnwantedCustomerService;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.product.service.ServiceService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.ByteMultiPartFile;
import bg.energo.phoenix.util.contract.CommunicationContactPurposeProperties;
import bg.energo.phoenix.util.contract.product.ProductContractStatusChainUtil;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBDateUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import io.micrometer.core.instrument.util.StringUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static bg.energo.phoenix.util.communication.CommunicationDataUtils.checkCommunicationEmailAndNumber;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceContractBasicParametersService {
    private final TermsRepository termsRepository;
    private final TermsGroupTermsRepository termsGroupTermsRepository;
    private final ServiceContractAdditionalDocumentsRepository serviceContractAdditionalDocumentsRepository;

    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ServiceRepository serviceRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CurrencyRepository currencyRepository;
    private final CustomerCommunicationsRepository communicationsRepository;
    private final CustomerCommContactPurposesRepository commContactPurposesRepository;
    private final CommunicationContactPurposeProperties communicationContactPurposeProperties;
    private final UnwantedCustomerRepository unwantedCustomerRepository;
    private final ManagerRepository managerRepository;
    private final ServiceContractProxyFilesRepository serviceContractProxyFilesRepository;
    private final ServiceContractProxyRepository serviceContractProxyRepository;
    private final ServiceContractProxyManagersRepository serviceContractProxyManagersRepository;
    private final Validator validator;
    private final ServiceContractFilesRepository serviceContractFilesRepository;
    private final ContractVersionTypesRepository contractVersionTypesRepository;
    private final ServiceContractContractVersionTypesRepository serviceContractContractVersionTypesRepository;
    private final RelatedContractsAndOrdersService relatedContractsAndOrdersService;
    private final ServiceService serviceService;
    private final UnwantedCustomerService unwantedCustomerService;
    private final CustomerSegmentRepository customerSegmentRepository;
    private final ServiceSegmentsRepository serviceSegmentsRepository;
    private final CustomerCommunicationContactsRepository communicationContactsRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final PermissionService permissionService;
    private final ServiceContractFilesService serviceContractFilesService;
    private final ServiceContractDocumentService serviceContractDocumentService;
    private final EDMSFileArchivationService archivationService;
    private final DocumentsRepository documentsRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final SignerChainManager signerChainManager;
    private final ServiceContractSignableDocumentsRepository signableDocumentRepository;

    private static void checkEntryIntoForceDate(LocalDate requestEntryIntoForceDate) {
        if (requestEntryIntoForceDate == null) {
            throw new ClientException("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] " +
                    "should be assigned when term entry into force date contains MANUAL;", ErrorCode.CONFLICT);
        } else {
            if (!EPBDateUtils.isDateInRange(requestEntryIntoForceDate, LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                throw new ClientException("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] " +
                        "should be between 01/01/1990 and 31/12/2090;", ErrorCode.CONFLICT);
            }
        }
    }

    private static void checkEntryIntoForceDateRange(LocalDate requestEntryIntoForceDate) {
        if (requestEntryIntoForceDate != null) {
            if (!EPBDateUtils.isDateInRange(requestEntryIntoForceDate, LocalDate.of(1990, 1, 1), LocalDate.of(2090, 12, 31))) {
                throw new ClientException("serviceContractBasicParametersCreateRequest.entryIntoForceDate-[entryIntoForceDate] " +
                        "should be between 01/01/1990 and 31/12/2090;", ErrorCode.CONFLICT);
            }
        }
    }

    public static void findMissingProxyIds(List<ServiceContractProxy> dbProxies, List<ProxyEditRequest> proxyEditList, List<Long> shouldBeDeleted) {
        for (ServiceContractProxy proxy : dbProxies) {
            long proxyId = proxy.getId();
            boolean found = false;

            for (ProxyEditRequest editRequest : proxyEditList) {
                if (editRequest.getId() != null && editRequest.getId().equals(proxyId)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                shouldBeDeleted.add(proxyId);
            }
        }
    }

    public static boolean isStartDateValid(List<ServiceContractDetails> serviceContractDetails, LocalDate startDate, ServiceContractDetails contractDetails, ServiceContracts serviceContract, ServiceContractDetails contractDetailsUpdated) {
        LocalDate previousVersionStartDate = null;
        LocalDate futuredVersionStartDate = null;
        Optional<ServiceContractDetails> serviceContractWithSameStartDateOptional =
                serviceContractDetails.stream().filter(a -> a.getStartDate().equals(startDate)).findFirst();
        if (serviceContractWithSameStartDateOptional.isPresent()) {
            ServiceContractDetails serviceContractDetails1 = serviceContractWithSameStartDateOptional.get();
            if (!contractDetails.getId().equals(serviceContractDetails1.getServiceDetailId())) {
                throw new ClientException("Contract version with this date: %s already exists;".formatted(getFormattedString(startDate)), ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }
        boolean firstVersion = false;
        if (contractDetails.getVersionId() == 1
                && contractDetailsUpdated.getId() != null) {
            firstVersion = true;
            if (serviceContract.getSigningDate() != null &&
                    serviceContract.getSigningDate().isBefore(LocalDate.now()) &&
                    !serviceContract.getSigningDate().equals(startDate)) {
                throw new ClientException("When signing date is in the past, first contract version date can be changed only on signing date", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }
        for (ServiceContractDetails serviceContractDetail : serviceContractDetails) {
            if (serviceContractDetail.getStartDate().isBefore(startDate)) {
                previousVersionStartDate = serviceContractDetail.getStartDate();
            } else if (serviceContractDetail.getStartDate().isAfter(startDate)) {
                futuredVersionStartDate = serviceContractDetail.getStartDate();
                break;
            }
        }
        if (previousVersionStartDate != null && futuredVersionStartDate == null) {
            return true;
        } else if (previousVersionStartDate == null && futuredVersionStartDate != null && !firstVersion) {
            throw new ClientException("Can't update start date with this value: %s, because value in first version is: %s;".formatted(getFormattedString(startDate), getFormattedString(futuredVersionStartDate)), ErrorCode.OPERATION_NOT_ALLOWED);
        } else return true;
    }

    private static String getFormattedString(LocalDate startDate) {
        if (startDate != null) {
            String textStartDate = startDate.toString();
            if (!StringUtils.isEmpty(textStartDate)) {
                return textStartDate.replace('-', '.');
            }
        }
        return null;
    }

    private ServiceContractBasicParametersResponse mapToBasicParameters(ServiceContracts contract, ServiceContractDetails details) {
        ServiceContractBasicParametersResponse response = new ServiceContractBasicParametersResponse();
        response.setId(details.getContractId());
        response.setVersionId(details.getVersionId());
        response.setContractNumber(contract.getContractNumber());
        if (!details.getType().equals(ServiceContractContractType.CONTRACT)) {
            response.setAdditionalSuffix("#" + details.getAgreementSuffix());
            //response.setContractNumber(contract.getContractNumber()+"#"+details.getAgreementSuffix());
        }
        //response.setContractNumber(contract.getContractNumber());
        response.setCreationDate(contract.getCreateDate().toLocalDate());
        response.setContractStatus(contract.getContractStatus());
        response.setSubStatus(contract.getSubStatus());
        response.setStatusModifyDate(contract.getStatusModifyDate());
        response.setContractStatusModifyDate(contract.getStatusModifyDate());
        response.setType(details.getType());
        response.setHasUntilAmount(details.getContractTermUntilTheAmount());
        response.setContractTermUntilTheAmountValue(details.getContractTermUntilTheAmountValue());
        response.setSignInDate(contract.getSigningDate());
        response.setEntryIntoForceDate(contract.getEntryIntoForceDate());
        response.setTerminationDate(contract.getTerminationDate());
        response.setContractTermEndDate(contract.getContractTermEndDate());
        //response.setActivationDate(contract.getActivationDate());
        response.setPerpetuityDate(contract.getPerpetuityDate());
        response.setContractTermUntilAmountIsReached(details.getContractTermUntilTheAmountValue());
        response.setContractTermUntilAmountIsReachedCheckbox(details.getContractTermUntilTheAmount());
        //response.setCurrencyId(details.getCurrencyId());
        response.setCurrency(getCurrency(details.getCurrencyId()));
        response.setContractVersionStatus(details.getContractVersionStatus());
        response.setContractInitialTermStartDate(contract.getContractInitialTermStartDate());
        //response.setStartOfContractInitialTerm(contract.getStartOfContractInitialTerm());
        response.setVersionTypes(getVersionTypes(details.getId()));
        response.setCommunicationDataForBilling(details.getCustomerCommunicationIdForBilling());
        response.setCommunicationDataForContract(details.getCustomerCommunicationIdForContract());
        return response;
    }

    private CurrencyResponse getCurrency(Long currencyId) {
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
        return currencyOptional.map(CurrencyResponse::new).orElse(null);
    }

    private List<ServiceContractContractVersionTypesResponse> getVersionTypes(Long id) {
        List<ServiceContractContractVersionTypes> contractVersionTypes =
                serviceContractContractVersionTypesRepository.findByContractDetailIdAndStatusIn(id, List.of(ContractSubObjectStatus.ACTIVE));
        List<ServiceContractContractVersionTypesResponse> returnList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(contractVersionTypes)) {
            for (ServiceContractContractVersionTypes item : contractVersionTypes) {
                ServiceContractContractVersionTypesResponse serviceContractContractVersionTypesResponse = new ServiceContractContractVersionTypesResponse();
                serviceContractContractVersionTypesResponse.setServiceContractVersionId(item.getId());
                ContractVersionType contractVersionType = getContractVersionType(item.getContractVersionTypeId());
                if (contractVersionType != null) {
                    serviceContractContractVersionTypesResponse.setId(contractVersionType.getId());
                    serviceContractContractVersionTypesResponse.setName(contractVersionType.getName());
                }
                returnList.add(serviceContractContractVersionTypesResponse);
            }
        }
        return returnList;
    }

    private ContractVersionType getContractVersionType(Long id) {
        Optional<ContractVersionType> contractVersionTypeOptional = contractVersionTypesRepository.findById(id);
        return contractVersionTypeOptional.orElse(null);
    }

    public void create(EPService service, ServiceDetails serviceDetails, ServiceContractCreateRequest request, ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceDetails dbServiceDetails, List<String> errorMessages) {
        ServiceContractBasicParametersCreateRequest basicRequest = request.getBasicParameters();
        checkUnwantedCustomer(basicRequest.getCustomerId(), errorMessages);

        if (!serviceRepository.existsByIdAndStatusIn(basicRequest.getServiceId(), List.of(ServiceStatus.ACTIVE))) {
            errorMessages.add("serviceContractBasicParametersCreateRequest.serviceId -[serviceId] Active Service not found;");
        }

        if (!customerRepository.existsByIdAndStatusIn(basicRequest.getCustomerId(), List.of(CustomerStatus.ACTIVE))) {
            errorMessages.add("serviceContractBasicParametersCreateRequest.customerId -[customerId] Active customer not found;");
        }

        serviceContractDetails.setContractVersionStatus(checkContractVersionStatus(request.getBasicParameters().getContractVersionStatus(), errorMessages));


        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.
                findByCustomerIdAndVersionId(basicRequest.getCustomerId(), basicRequest.getCustomerVersionId());
        if (customerDetailsOptional.isEmpty()) {
            errorMessages.add("serviceContractBasicParametersCreateRequest.customerVersionId -[customerVersionId] customer details with version id not found;");
        } else {
            CustomerDetails customerDetails = customerDetailsOptional.get();
            if (!List.of(CustomerDetailStatus.NEW, CustomerDetailStatus.ACTIVE, CustomerDetailStatus.LOST).contains(customerDetails.getStatus())) {
                errorMessages.add("basicParameters.customerVersionId-Can not conclude contract because customer has wrong status!;");
            }
        }

        CustomerDetails customerDetails = null;
        if (serviceDetails != null && customerDetailsOptional.isPresent()) {
            customerDetails = customerDetailsOptional.get();
        }
        if (customerDetails != null) {
            serviceContractDetails.setCustomerDetailId(customerDetails.getId());

        }
        if (serviceDetails != null) {
            serviceContractDetails.setServiceDetailId(serviceDetails.getId());
        }
        createServiceContractDetails(serviceContract, request, serviceDetails, serviceContractDetails, basicRequest, errorMessages);
        serviceContractDetails.setContractId(serviceContract.getId());
        //serviceContractDetails.setContractId(serviceContract.getId());
        createServiceContractCommunicationData(customerDetails.getId(), basicRequest.getCommunicationDataForContract(), serviceContractDetails, errorMessages);
        createBillingContractCommunicationData(customerDetails.getId(), basicRequest.getCommunicationDataForBilling(), serviceContractDetails, errorMessages);
        serviceContractDetails.setVersionId(1L);
    }

    private ContractVersionStatus checkContractVersionStatus(ContractVersionStatus contractVersionStatus, List<String> errorMessages) {
        if (contractVersionStatus.equals(ContractVersionStatus.DRAFT) || contractVersionStatus.equals(ContractVersionStatus.READY) || contractVersionStatus.equals(ContractVersionStatus.SIGNED)) {
            return contractVersionStatus;
        } else {
            errorMessages.add("basicParameters.contractVersionStatus-[contractVersionStatus] should be only DRAFT ,READY or SIGNED;");
            return null;
        }
    }

    private LocalDate getContractTermEndDate(LocalDate contractInitialTermStartDate, ServiceDetails dbServiceDetails) {
        if (contractInitialTermStartDate != null) {
        }
        return null;
    }

    public void validateContractVersionTypesAndCreate(ServiceContractDetails serviceContract, List<Long> contractVersionTypes) {
        List<ContractVersionType> contractVersions = new ArrayList<>();
        List<ServiceContractContractVersionTypes> serviceContractContractVersionTypesToSave = new ArrayList<>();
        for (Long id : contractVersionTypes) {
            ContractVersionType contractVersionType = contractVersionTypesRepository.findByIdAndStatusIn(id, List.of(NomenclatureItemStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Active nomenclature with id:%s;".formatted(id)));
            contractVersions.add(contractVersionType);
        }
        if (!CollectionUtils.isEmpty(contractVersionTypes)) {
            for (ContractVersionType item : contractVersions) {
                ServiceContractContractVersionTypes serviceContractContractVersionTypes = new ServiceContractContractVersionTypes();
                serviceContractContractVersionTypes.setContractDetailId(serviceContract.getId());
                serviceContractContractVersionTypes.setContractVersionTypeId(item.getId());
                serviceContractContractVersionTypes.setStatus(ContractSubObjectStatus.ACTIVE);
                serviceContractContractVersionTypesRepository.save(serviceContractContractVersionTypes);
                serviceContractContractVersionTypesToSave.add(serviceContractContractVersionTypes);
            }
            // serviceContractContractVersionTypesRepository.saveAllAndFlush(serviceContractContractVersionTypesToSave); //TODO ERROR IN DATABASE
        }
    }

    private LocalDate getInitialTermDate(ServiceContractCreateRequest request, ServiceDetails dbServiceDetails, List<String> errorMessages) {
        LocalDate initialTermDate = request.getBasicParameters().getStartOfTheInitialTermOfTheContract();
        LocalDate signInDate = request.getBasicParameters().getSignInDate();
        StartOfContractInitialTerm startOfContractInitialTerm = request.getServiceParameters().getStartOfContractInitialTerm();
        if (startOfContractInitialTerm != null) {
            if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.MANUAL)) {
                return request.getBasicParameters().getStartOfTheInitialTermOfTheContract();
            }
            if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.EXACT_DATE)) {
                return !request.getBasicParameters().getContractStatus().equals(ServiceContractDetailStatus.DRAFT) ?
                        request.getServiceParameters().getStartOfContractInitialTermDate() : null;
            }
            if (startOfContractInitialTerm.equals(StartOfContractInitialTerm.SIGNING)) {
                return signInDate;
            }
        }
        Terms targetTerm = dbServiceDetails.getTerms();
        TermsGroups targetTermGroup = dbServiceDetails.getTermsGroups();
        List<StartOfContractInitialTerm> supplyActivations = new ArrayList<>();
        if (Objects.nonNull(targetTerm)) {
            supplyActivations.addAll(targetTerm.getStartsOfContractInitialTerms());
        } else if (Objects.nonNull(targetTermGroup)) {
            Optional<TermsGroupTerms> termsGroupTermsOptional = termsGroupTermsRepository
                    .findByTermGroupDetailIdAndTermGroupStatusIn(targetTermGroup.getLastGroupDetailsId(), List.of(TermGroupStatus.ACTIVE));

            if (termsGroupTermsOptional.isEmpty()) {
                errorMessages.add("Term Group with active term not found");
            } else {
                TermsGroupTerms termsGroupTerms = termsGroupTermsOptional.get();

                Optional<Terms> termsOptional = termsRepository
                        .findByIdAndStatusIn(termsGroupTerms.getTermId(), List.of(TermStatus.ACTIVE));

                if (termsOptional.isEmpty()) {
                    errorMessages.add("Service Term Group does not has active Term");
                } else {
                    List<StartOfContractInitialTerm> startsOfContractInitialTerms = termsOptional.get().getStartsOfContractInitialTerms();
                    if (CollectionUtils.isNotEmpty(startsOfContractInitialTerms)) {
                        supplyActivations.addAll(startsOfContractInitialTerms);
                    }
                }
            }
        } else {
            errorMessages.add("Service does not has any Term or Term Group");
        }

        for (StartOfContractInitialTerm item : supplyActivations) {
            if (item.equals(StartOfContractInitialTerm.MANUAL)) {
                return signInDate;
            } else if (item.equals(StartOfContractInitialTerm.EXACT_DATE)) {
                return initialTermDate;
            }
            if (item.equals(StartOfContractInitialTerm.SIGNING)) {
                return signInDate; //TODO ASK TINIKO
            }
            if (item.equals(StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG) || item.equals(StartOfContractInitialTerm.FIRST_DELIVERY)) {
                //DON'T HAVE DATE OF ACTIVATION SO DO NOTHING
            }
        }
        return null;
    }

    private Boolean getExactDate(List<StartOfContractInitialTerm> contractEntryIntoForces) {
        for (StartOfContractInitialTerm item : contractEntryIntoForces) {
            if (item.equals(StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG) || item.equals(StartOfContractInitialTerm.FIRST_DELIVERY)) {
                return true;
            }
        }
        return false;
    }

    public void createServiceContractProxy(Customer customer, Long customerVersionId, List<ProxyEditRequest> proxyAddRequestList, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        if (!CollectionUtils.isEmpty(proxyAddRequestList)) {
            log.debug("Creating serviceContractProxy object: {}", proxyAddRequestList);
            checkProxyRequest(proxyAddRequestList, serviceContractDetails, errorMessages);
            for (ProxyEditRequest request : proxyAddRequestList) {
                EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
                Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(customer.getId(), customerVersionId);
                if (customerDetailsOptional.isPresent()) {
                    CustomerDetails customerDetails = customerDetailsOptional.get();
                    List<Manager> managers = getManagers(customer, customerDetails.getId(), request, errorMessages);
                    List<ServiceContractProxyFiles> proxyFile = getProxyFiles(request.getFileIds(), errorMessages);
                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
                    ServiceContractProxy proxy = mapProxyRequestToProxyEntity(request, serviceContractDetails.getId());
                    ServiceContractProxy dbProxy = serviceContractProxyRepository.saveAndFlush(proxy);
                    if (CollectionUtils.isNotEmpty(proxyFile)) {
                        updateProxyFiles(dbProxy, proxyFile, errorMessages);
                    }
                    if (CollectionUtils.isNotEmpty(managers)) {
                        updateProxyManagers(dbProxy, managers, errorMessages);
                    }
                    EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
                }
            }
        }
    }

    @Transactional
    public void updateProxy(Long id, Long customerVersionId, List<ProxyEditRequest> proxyEditList, Long contractDetailId, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(proxyEditList)) {
            log.debug("Update proxy object: {}", proxyEditList);
            List<Long> updatedProxyIds = new ArrayList<>();
            Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(id, List.of(CustomerStatus.ACTIVE));
            if (customerOptional.isPresent()) {
                Customer customer = customerOptional.get();
                if (!customer.getCustomerType().equals(CustomerType.LEGAL_ENTITY) && proxyEditList.size() > 1) {
                    exceptionMessages.add("proxy-[proxy] you cant add multiple proxy when customer type is not legal entity;");
                }
                List<ServiceContractProxy> dbProxies = serviceContractProxyRepository.findByContractDetailIdAndStatusIn(contractDetailId, List.of(ContractSubObjectStatus.ACTIVE));
                if (dbProxies.isEmpty()) {
                    List<ProxyEditRequest> proxiesToCreate = proxyEditList;
                    for (ProxyEditRequest request : proxiesToCreate) {
                        updatedProxyIds.add(create(customer, customerVersionId, request, contractDetailId, exceptionMessages));
                    }
                } else {
                    //List<ProxyEditRequest> shouldBeDeleted = new ArrayList<>();
                    List<Long> shouldBeDeleted = new ArrayList<>();
                    List<ProxyEditRequest> shouldBeCreated = new ArrayList<>();
                    List<ProxyEditRequest> shouldBeEdited = getProxies(proxyEditList, dbProxies, shouldBeDeleted, shouldBeCreated);
                    if (CollectionUtils.isNotEmpty(shouldBeEdited)) {
                        for (ProxyEditRequest request : shouldBeEdited) {
                            updatedProxyIds.add(editProxy(customer, customerVersionId, request, request.getId(), exceptionMessages));
                        }
                    }
                    if (CollectionUtils.isNotEmpty(shouldBeDeleted)) {
                        for (Long proxyId : shouldBeDeleted) {
                            Optional<ServiceContractProxy> proxyOptional = serviceContractProxyRepository.findByIdAndStatus(proxyId, ContractSubObjectStatus.ACTIVE);
                            if (proxyOptional.isPresent()) {
                                ServiceContractProxy proxy = proxyOptional.get();
                                proxy.setStatus(ContractSubObjectStatus.DELETED);
                                serviceContractProxyRepository.save(proxy);
                            } else {
                                exceptionMessages.add("Can't find active proxy with id: %s;".formatted(proxyId));
                            }
                        }
                    }
                    if (CollectionUtils.isNotEmpty(shouldBeCreated)) {
                        for (ProxyEditRequest request : shouldBeCreated) {
                            updatedProxyIds.add(create(customer, customerVersionId, request, contractDetailId, exceptionMessages));
                        }
                    }
                }
            }

        } else {
            List<ServiceContractProxy> proxies = serviceContractProxyRepository.getProxiesByContractDetailIdAndStatus(contractDetailId, ContractSubObjectStatus.ACTIVE);
            if (CollectionUtils.isNotEmpty(proxies)) {
                for (ServiceContractProxy item : proxies) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    serviceContractProxyRepository.save(item);
                }
            }
            // proxyRepository.deleteAllByContractDetailsId(contractDetailId, ContractSubObjectStatus.ACTIVE);
        }
    }

    @Transactional
    public Long editProxy(Customer customer, Long customerVersionId, @Valid ProxyAddRequest request, Long proxyId, List<String> exceptionMessages) {
        log.debug("Update proxy object: {}", request.toString());
        Set<ConstraintViolation<ProxyAddRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<ProxyAddRequest> constraintViolation : violations) {
                sb.append(constraintViolation.getMessage());
            }
            exceptionMessages.add(sb.toString());
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        Optional<ServiceContractProxy> proxyOptional = serviceContractProxyRepository.findByIdAndStatus(proxyId, ContractSubObjectStatus.ACTIVE);
        if (proxyOptional.isPresent()) {
            ServiceContractProxy proxy = proxyOptional.get();
            ServiceContractProxy updatedProxy = mapProxyRequestToProxyEntity(request, proxy.getContractDetailId());
            List<ServiceContractProxyFiles> proxyFiles = serviceContractProxyFilesRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
            List<ServiceContractProxyManagers> proxyManagers = serviceContractProxyManagersRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
            editProxyFiles(proxy, proxyFiles, request.getFileIds());
            Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(customer.getId(), customerVersionId);
            if (customerDetailsOptional.isPresent()) {
                CustomerDetails customerDetails = customerDetailsOptional.get();
                List<Manager> managers = getManagers(customer, customerDetails.getId(), request, exceptionMessages);
                editManagers(proxy, proxyManagers, request.getManagerIds(), managers);
            }
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        } else {
            exceptionMessages.add("id-can't find proxy with id: %s;".formatted(proxyId));
        }
        return proxyId;
    }

    private void editManagers(ServiceContractProxy proxy, List<ServiceContractProxyManagers> proxyManagers, Set<Long> managerIds, List<Manager> managers) {
        Set<Long> notInManagerIds = new HashSet<>(managerIds);
        Set<Long> shouldBeUpdated = new HashSet<>();
        if (CollectionUtils.isEmpty(managerIds)) {
            if (CollectionUtils.isNotEmpty(proxyManagers)) {
                List<ServiceContractProxyManagers> proxyManagersToDelete = serviceContractProxyManagersRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
                if (CollectionUtils.isNotEmpty(proxyManagersToDelete)) {
                    for (ServiceContractProxyManagers item : proxyManagersToDelete) {
                        item.setStatus(ContractSubObjectStatus.DELETED);
                        serviceContractProxyManagersRepository.save(item);
                    }
                }
            }
        }
        for (ServiceContractProxyManagers proxyManager : proxyManagers) {
            Long fileId = proxyManager.getCustomerManagerId();
            Long actualId = proxyManager.getId();
            if (managerIds.contains(fileId)) {
                shouldBeUpdated.add(actualId);
                notInManagerIds.remove(fileId);
            }
        }
        if (CollectionUtils.isNotEmpty(shouldBeUpdated)) {
            List<ServiceContractProxyManagers> proxyManagersShouldBeDeleted = serviceContractProxyManagersRepository.findByContractProxyIdAndIdNotInAndStatus(proxy.getId(), shouldBeUpdated, ContractSubObjectStatus.ACTIVE);
            if (CollectionUtils.isNotEmpty(proxyManagersShouldBeDeleted)) {
                for (ServiceContractProxyManagers item : proxyManagersShouldBeDeleted) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    serviceContractProxyManagersRepository.save(item);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(notInManagerIds)) {
            for (Long id : notInManagerIds) {
                ServiceContractProxyManagers proxyManagers1 = new ServiceContractProxyManagers();
                proxyManagers1.setContractProxyId(proxy.getId());
                proxyManagers1.setCustomerManagerId(id);
                proxyManagers1.setStatus(ContractSubObjectStatus.ACTIVE);
                serviceContractProxyManagersRepository.save(proxyManagers1);
            }
        }

    }

    private void editProxyFiles(ServiceContractProxy proxy, List<ServiceContractProxyFiles> proxyFiles, Set<Long> fileIds) {
        Set<Long> notInProxyFiles = new HashSet<>(fileIds);
        Set<Long> shouldBeUpdated = new HashSet<>();
        if (CollectionUtils.isEmpty(fileIds)) {
            if (CollectionUtils.isNotEmpty(proxyFiles)) {
                List<ServiceContractProxyFiles> proxyFilesToDelete = serviceContractProxyFilesRepository.findByContractProxyIdAndStatus(proxy.getId(), ContractSubObjectStatus.ACTIVE);
                if (CollectionUtils.isNotEmpty(proxyFilesToDelete)) {
                    for (ServiceContractProxyFiles item : proxyFilesToDelete) {
                        item.setStatus(ContractSubObjectStatus.DELETED);
                        serviceContractProxyFilesRepository.save(item);
                    }
                }
            }
        }
        for (ServiceContractProxyFiles proxyFile : proxyFiles) {
            Long fileId = proxyFile.getId();
            if (fileIds.contains(fileId)) {
                shouldBeUpdated.add(fileId);
                notInProxyFiles.remove(fileId);
            }
        }

        if (CollectionUtils.isNotEmpty(shouldBeUpdated)) {
            List<ServiceContractProxyFiles> proxiesShouldBeDeleted = serviceContractProxyFilesRepository.findByContractProxyIdAndIdNotInAndStatus(proxy.getId(), shouldBeUpdated, ContractSubObjectStatus.ACTIVE);
            if (CollectionUtils.isNotEmpty(proxiesShouldBeDeleted)) {
                for (ServiceContractProxyFiles item : proxiesShouldBeDeleted) {
                    item.setStatus(ContractSubObjectStatus.DELETED);
                    serviceContractProxyFilesRepository.save(item);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(notInProxyFiles)) {
            for (Long id : notInProxyFiles) {
                ServiceContractProxyFiles proxyFile = serviceContractProxyFilesRepository.findByIdAndStatus(id, ContractSubObjectStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("id-can't find Active proxyFiles with id: %s;".formatted(id)));
                proxyFile.setContractProxyId(proxy.getId());
                serviceContractProxyFilesRepository.save(proxyFile);
            }
        }
    }

    private List<ProxyEditRequest> getProxies(List<ProxyEditRequest> proxyEditList, List<ServiceContractProxy> dbProxies, List<Long> shouldBeDeleted, List<ProxyEditRequest> shouldBeCreated) {
        List<ProxyEditRequest> shouldBeEdited = new ArrayList<>();
        List<ProxyEditRequest> copyList = new ArrayList<>(proxyEditList);
        findMissingProxyIds(dbProxies, proxyEditList, shouldBeDeleted);

        for (ProxyEditRequest proxyEditRequest : copyList) {
            Long proxyEditRequestId = proxyEditRequest.getId();
            if (proxyEditRequest.getId() != null) {
                boolean hasSameId = dbProxies.stream()
                        .anyMatch(proxy -> proxy.getId().equals(proxyEditRequestId));
                if (hasSameId) {
                    shouldBeEdited.add(proxyEditRequest);
                }
            } else {
                shouldBeCreated.add(proxyEditRequest);
            }
        }
        return shouldBeEdited;
    }

    @Transactional
    public Long create(Customer customer, Long customerVersionId, ProxyAddRequest request, Long contractDetailId, List<String> exceptionMessages) {
        if (request != null) {
            log.debug("Creating proxy object: {}", request);
            Set<ConstraintViolation<ProxyAddRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ConstraintViolation<ProxyAddRequest> constraintViolation : violations) {
                    sb.append(constraintViolation.getMessage());
                }
                exceptionMessages.add(sb.toString());
            }
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
            Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(customer.getId(), customerVersionId);
            if (customerDetailsOptional.isPresent()) {
                CustomerDetails customerDetails = customerDetailsOptional.get();
                List<Manager> managers = getManagers(customer, customerDetails.getId(), request, exceptionMessages);
                List<ServiceContractProxyFiles> proxyFile = getProxyFiles(request.getFileIds(), exceptionMessages);
                EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
                ServiceContractProxy proxy = mapProxyRequestToProxyEntity(request, contractDetailId);
                ServiceContractProxy dbProxy = serviceContractProxyRepository.saveAndFlush(proxy);
                if (CollectionUtils.isNotEmpty(proxyFile)) {
                    updateProxyFiles(dbProxy, proxyFile, exceptionMessages);
                }
                if (CollectionUtils.isNotEmpty(managers)) {
                    updateProxyManagers(dbProxy, managers, exceptionMessages);
                }
                EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
                return dbProxy.getId();
            } else {
                exceptionMessages.add("can't find customer details;");
                return null;
            }
        } else return null;
    }

    private void checkProxyRequest(List<ProxyEditRequest> request, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        Customer customer = getCustomer(serviceContractDetails.getCustomerDetailId(), errorMessages);
        if (customer != null) {
            if (!customer.getCustomerType().equals(CustomerType.LEGAL_ENTITY)) {
                if (request.size() > 1) {
                    errorMessages.add("serviceContractBasicParametersCreateRequest.proxy-[proxy] can't have more then one proxy items when customer type is LEGAL_ENTITY;");
                }
            }
        }
    }

    private Customer getCustomer(Long customerDetailId, List<String> errorMessages) {
        Optional<Customer> customerOptional = customerRepository.findByCustomerDetailIdAndStatusIn(customerDetailId, List.of(CustomerStatus.ACTIVE));
        if (customerOptional.isPresent()) {
            return customerOptional.get();
        } else {
            errorMessages.add("Can't find active customer with details id: %s;".formatted(customerDetailId));
            return null;
        }
    }

    public void createContractFiles(LinkedHashSet<Long> request, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(request)) {
            serviceContractFilesRepository.saveAll(getContractFiles(request, serviceContractDetails, errorMessages));
        }
    }

    public void copyContractFiles(LinkedHashSet<Long> request, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(request)) {
            serviceContractFilesRepository.saveAll(getFilesToCopyContractFiles(request, serviceContractDetails, errorMessages));
        }
    }

    public void createContractDocuments(LinkedHashSet<Long> documents, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(documents)) {
            serviceContractAdditionalDocumentsRepository.saveAll(getDocuments(documents, serviceContractDetails, errorMessages));
        }
    }

    public void copyContractDocuments(LinkedHashSet<Long> documents, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(documents)) {
            serviceContractAdditionalDocumentsRepository.saveAll(getDocumentsToCopy(documents, serviceContractDetails, errorMessages));
        }
    }


    private List<ServiceContractAdditionalDocuments> getDocuments(LinkedHashSet<Long> documents, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        List<ServiceContractAdditionalDocuments> serviceContractFiles = new ArrayList<>();
        for (Long id : documents) {
            Optional<ServiceContractAdditionalDocuments> serviceContractFilesOptional =
                    serviceContractAdditionalDocumentsRepository.findByIdAndStatus(id, EntityStatus.ACTIVE);
            if (serviceContractFilesOptional.isPresent()) {
                ServiceContractAdditionalDocuments serviceContractDocument = serviceContractFilesOptional.get();
                serviceContractDocument.setContractDetailId(serviceContractDetails.getId());
                serviceContractFiles.add(serviceContractDocument);
            } else {
                errorMessages.add("serviceContractBasicParametersCreateRequest.documents-[documents] can't find active file wit id: %s;".formatted(id));
            }
        }
        return serviceContractFiles;
    }

    private List<ServiceContractAdditionalDocuments> getDocumentsToCopy(LinkedHashSet<Long> documents, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        List<ServiceContractAdditionalDocuments> serviceContractFiles = new ArrayList<>();
        for (Long id : documents) {
            Optional<ServiceContractAdditionalDocuments> serviceContractFilesOptional =
                    serviceContractAdditionalDocumentsRepository.findByIdAndStatus(id, EntityStatus.ACTIVE);
            if (serviceContractFilesOptional.isPresent()) {
                ServiceContractAdditionalDocuments serviceContractDocument = serviceContractFilesOptional.get();
                if (Boolean.TRUE.equals(serviceContractDocument.getIsArchived())) {
                    try {
                        ByteArrayResource archivedFile = archivationService.downloadArchivedFile(serviceContractDocument.getDocumentId(), serviceContractDocument.getFileId());

                        FileWithStatusesResponse localFile = serviceContractDocumentService.uploadContractFile(new ByteMultiPartFile(serviceContractDocument.getName(), archivedFile.getContentAsByteArray()), serviceContractDocument.getFileStatuses());

                        ServiceContractAdditionalDocuments document = serviceContractAdditionalDocumentsRepository
                                .findById(localFile.getId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Service Contract Document with id: [%s] not found;".formatted(localFile.getId())));

                        document.setContractDetailId(serviceContractDetails.getId());

                        serviceContractFiles.add(document);
                    } catch (Exception e) {
                        log.error("Exception handled while trying to archive file in new version: %s".formatted(serviceContractDocument.getName()), ErrorCode.APPLICATION_ERROR);
                        throw new ClientException("Exception handled while trying to archive file in new version: %s".formatted(serviceContractDocument.getName()), ErrorCode.APPLICATION_ERROR);
                    }
                } else {
                    ServiceContractAdditionalDocuments newServiceContractDocument = new ServiceContractAdditionalDocuments();
                    newServiceContractDocument.setName(serviceContractDocument.getName());
                    newServiceContractDocument.setLocalFileUrl(serviceContractDocument.getLocalFileUrl());
                    newServiceContractDocument.setContractDetailId(serviceContractDetails.getId());
                    newServiceContractDocument.setStatus(EntityStatus.ACTIVE);
                    serviceContractFiles.add(newServiceContractDocument);
                }
            } else {
                errorMessages.add("serviceContractBasicParametersCreateRequest.documents-[documents] can't find active file wit id: %s;".formatted(id));
            }
        }
        return serviceContractFiles;
    }

    private List<ServiceContractFiles> getContractFiles(LinkedHashSet<Long> list, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        List<ServiceContractFiles> serviceContractFiles = new ArrayList<>();
        for (Long id : list) {
            Optional<ServiceContractFiles> serviceContractFilesOptional =
                    serviceContractFilesRepository.findByIdAndStatus(id, EntityStatus.ACTIVE);
            if (serviceContractFilesOptional.isPresent()) {
                ServiceContractFiles serviceContractFile = serviceContractFilesOptional.get();
                serviceContractFile.setContractDetailId(serviceContractDetails.getId());
                serviceContractFiles.add(serviceContractFile);
            } else {
                errorMessages.add("serviceContractBasicParametersCreateRequest.files-[files] can't find active file wit id: %s;".formatted(id));
            }
        }
        return serviceContractFiles;
    }

    private List<ServiceContractFiles> getFilesToCopyContractFiles(LinkedHashSet<Long> list,
                                                                   ServiceContractDetails serviceContractDetails,
                                                                   List<String> errorMessages) {
        List<ServiceContractFiles> serviceContractFiles = new ArrayList<>();
        for (Long id : list) {
            Optional<ServiceContractFiles> serviceContractFilesOptional =
                    serviceContractFilesRepository.findByIdAndStatus(id, EntityStatus.ACTIVE);
            if (serviceContractFilesOptional.isPresent()) {
                ServiceContractFiles serviceContractFile = serviceContractFilesOptional.get();

                if (Boolean.TRUE.equals(serviceContractFile.getIsArchived())) {
                    try {
                        ByteArrayResource archivedFile = archivationService.downloadArchivedFile(serviceContractFile.getDocumentId(), serviceContractFile.getFileId());

                        FileWithStatusesResponse localFile = serviceContractFilesService.uploadContractFile(new ByteMultiPartFile(serviceContractFile.getName(), archivedFile.getContentAsByteArray()), serviceContractFile.getFileStatuses());

                        ServiceContractFiles file = serviceContractFilesRepository
                                .findById(localFile.getId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Service Contract File with id: [%s] not found;".formatted(localFile.getId())));

                        file.setContractDetailId(serviceContractDetails.getId());

                        serviceContractFiles.add(file);
                    } catch (Exception e) {
                        log.error("Exception handled while trying to archive file in new version: %s".formatted(serviceContractFile.getName()), e);
                        throw new ClientException("Exception handled while trying to archive file in new version: %s".formatted(serviceContractFile.getName()), ErrorCode.APPLICATION_ERROR);
                    }
                } else {
                    ServiceContractFiles newServiceContractFile = new ServiceContractFiles();
                    newServiceContractFile.setName(serviceContractFile.getName());
                    newServiceContractFile.setLocalFileUrl(serviceContractFile.getLocalFileUrl());
                    newServiceContractFile.setContractDetailId(serviceContractDetails.getId());
                    newServiceContractFile.setStatus(EntityStatus.ACTIVE);
                    serviceContractFiles.add(newServiceContractFile);
                }
            } else {
                errorMessages.add("serviceContractBasicParametersCreateRequest.files-[files] can't find active file wit id: %s;".formatted(id));
            }
        }
        return serviceContractFiles;
    }

    private void updateProxyManagers(ServiceContractProxy proxy, List<Manager> managers, List<String> errorMessages) {
        Long proxyId = proxy.getId();
        List<ServiceContractProxyManagers> proxyManagers = new ArrayList<>();
        for (Manager item : managers) {
            ServiceContractProxyManagers proxyManager = new ServiceContractProxyManagers();
            proxyManager.setContractProxyId(proxyId);
            proxyManager.setCustomerManagerId(item.getId());
            proxyManager.setStatus(ContractSubObjectStatus.ACTIVE);
            proxyManagers.add(proxyManager);
        }
        if (CollectionUtils.isNotEmpty(proxyManagers)) {
            serviceContractProxyManagersRepository.saveAll(proxyManagers);
        } else {
            errorMessages.add("serviceContractBasicParametersCreateRequest.proxy.managerIds-[managerIds] is empty;");
        }
    }

    private void updateProxyFiles(ServiceContractProxy proxy, List<ServiceContractProxyFiles> proxyFile, List<String> errorMessages) {
        Long proxyId = proxy.getId();
        for (ServiceContractProxyFiles item : proxyFile) {
            item.setContractProxyId(proxyId);
            serviceContractProxyFilesRepository.save(item);
        }
    }

    private ServiceContractProxy mapProxyRequestToProxyEntity(ProxyAddRequest request, Long id) {
        return ServiceContractProxy.builder()
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
                .status(ContractSubObjectStatus.ACTIVE)
                .contractDetailId(id)
                .build();
    }

    private List<ServiceContractProxyFiles> getProxyFiles(Set<Long> fileIds, List<String> exceptionMessages) {
        List<ServiceContractProxyFiles> proxyFiles = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(fileIds)) {
            for (Long id : fileIds) {
                Optional<ServiceContractProxyFiles> proxyFile =
                        serviceContractProxyFilesRepository.findByIdAndStatus(id, ContractSubObjectStatus.ACTIVE);
                if (proxyFile.isPresent()) {
                    ServiceContractProxyFiles dbProxyFile = proxyFile.get();
                    if (proxyFile.get().getContractProxyId() != null) {
                        ServiceContractProxyFiles serviceContractProxyFiles = ServiceContractProxyFiles.builder().
                                name(dbProxyFile.getName())
                                .fileUrl(dbProxyFile.getFileUrl())
                                .contractProxyId(null)
                                .status(ContractSubObjectStatus.ACTIVE)
                                .build();
                        ServiceContractProxyFiles savedProxyFile = serviceContractProxyFilesRepository.save(serviceContractProxyFiles);
                        proxyFiles.add(savedProxyFile);
                    } else proxyFiles.add(proxyFile.get());
                } else {
                    exceptionMessages.add("serviceContractBasicParametersCreateRequest.proxy.fileIds-[FileIds] can't find active file with id: %s;".formatted(id));
                }
            }
            return proxyFiles;
        }
        return null;
    }

    private List<Manager> getManagers(Customer customer, Long customerDetailsId, ProxyAddRequest request, List<String> exceptionMessages) {
        List<Manager> managers = new ArrayList<>();
        Set<Long> managerIds = request.getManagerIds();
        if (!customer.getCustomerType().equals(CustomerType.PRIVATE_CUSTOMER)) {
            if (!CollectionUtils.isEmpty(managerIds)) {
                for (Long id : managerIds) {
                    Optional<Manager> manager = managerRepository.findByIdAndStatus(id, Status.ACTIVE);
                    if (manager.isEmpty()) {
                        exceptionMessages.add("managerIds-[managerIds] can't find account manager with id: %s;".formatted(id));
                    } else {
                        if (manager.get().getCustomerDetailId().equals(customerDetailsId)) {
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

    public ServiceContractBasicParametersResponse getBasicParameters(ServiceContracts serviceContracts, ServiceContractDetails details) {
        ServiceContractBasicParametersResponse basicParametersResponse = mapToBasicParameters(serviceContracts, details);//new ServiceContractBasicParametersResponse();
        setCustomerData(details, basicParametersResponse);
        setServiceData(details, basicParametersResponse);
        setProxyDate(details, basicParametersResponse);
        setContractFiles(details, basicParametersResponse);
        setAdditionalFiles(details, basicParametersResponse);
        basicParametersResponse.setRelatedEntities(relatedContractsAndOrdersService.getRelatedEntities(serviceContracts.getId(), RelatedEntityType.SERVICE_CONTRACT));
        return basicParametersResponse;
    }

    private void setAdditionalFiles(ServiceContractDetails details, ServiceContractBasicParametersResponse basicParametersResponse) {
        List<ServiceContractAdditionalDocuments> contractFiles =
                serviceContractAdditionalDocumentsRepository.findServiceContractFilesByContractDetailIdAndStatusIn(details.getId(), List.of(EntityStatus.ACTIVE));
        List<FileWithStatusesResponse> additionalDocumentsResponse = new ArrayList<>();
        for (ServiceContractAdditionalDocuments item : contractFiles) {
            FileWithStatusesResponse additionalDocument = new FileWithStatusesResponse(item, accountManagerRepository.findByUserName(item.getSystemUserId())
                    .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
            additionalDocumentsResponse.add(additionalDocument);
        }
        basicParametersResponse.setAdditionalDocuments(additionalDocumentsResponse);
    }

    private void setContractFiles(ServiceContractDetails details, ServiceContractBasicParametersResponse basicParametersResponse) {
        List<ServiceContractFiles> contractFiles =
                serviceContractFilesRepository.findServiceContractFilesByContractDetailIdAndStatusIn(details.getId(), List.of(EntityStatus.ACTIVE));
        List<ContractFileResponse> contractFilesResponse = new ArrayList<>();
        for (ServiceContractFiles item : contractFiles) {
            ContractFileResponse contractFile = new ContractFileResponse(item, accountManagerRepository.findByUserName(item.getSystemUserId())
                    .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
            contractFilesResponse.add(contractFile);
        }
        List<ContractFileResponse> signableDocs = documentsRepository.findSignedDocumentsForServiceContractDetail(details.getId())
                .stream()
                .flatMap(x -> {
                    Stream.Builder<Pair<Document, ContractFileResponse>> builder = Stream.builder();
                    if (x.getDocumentStatus().equals(DocumentStatus.SIGNED)) {
                        boolean notSigned = !x.getFileFormat().equals(FileFormat.PDF) || (ListUtils.emptyIfNull(x.getSigners()).contains(DocumentSigners.NO));
                        builder.add(Pair.of(x, new ContractFileResponse(DocumentStatus.SIGNED, x, notSigned)));
                        if (!(x.getSigners().contains(DocumentSigners.NO) || x.getSigners().isEmpty())) {
                            builder.add(Pair.of(x, new ContractFileResponse(DocumentStatus.UNSIGNED, x, false)));
                        }
                    } else {
                        builder.add(Pair.of(x, new ContractFileResponse(DocumentStatus.UNSIGNED, x, false)));
                    }
                    return builder.build();
                })
                .peek(x -> x.getSecond().updateFileInfo(x.getFirst(), accountManagerRepository.findByUserName(x.getFirst().getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .map(Pair::getSecond)
                .toList();
        contractFilesResponse.addAll(signableDocs);

        contractFilesResponse.addAll(documentsRepository.findActionDocumentsForServiceContractDetail(details.getId()).stream()
                .map(f -> new ContractFileResponse(f, accountManagerRepository.findByUserName(f.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                .toList());
        basicParametersResponse.setContractFiles(contractFilesResponse);
    }

    private void setProxyDate(ServiceContractDetails details, ServiceContractBasicParametersResponse basicParametersResponse) {
        List<ServiceContractProxy> serviceContractProxyOptional =
                serviceContractProxyRepository.findByContractDetailIdAndStatusIn(details.getId(), List.of(ContractSubObjectStatus.ACTIVE));
        if (!CollectionUtils.isEmpty(serviceContractProxyOptional)) {
            List<ProxyResponse> proxyResponses = new ArrayList<>();
            for (ServiceContractProxy item : serviceContractProxyOptional) {
                List<ServiceContractProxyFiles> proxyFiles =
                        serviceContractProxyFilesRepository.findServiceContractProxyFilesByContractProxyIdAndStatusIn(item.getId(), List.of(ContractSubObjectStatus.ACTIVE));
                List<ServiceContractProxyManagers> proxyManagers =
                        serviceContractProxyManagersRepository.findByContractProxyIdAndStatusIn(item.getId(), List.of(ContractSubObjectStatus.ACTIVE));
                ProxyResponse proxyResponse = new ProxyResponse();
                proxyResponse = mapProxyToResponse(item, proxyFiles, proxyManagers);
                proxyResponses.add(proxyResponse);
            }
            basicParametersResponse.setProxyResponse(proxyResponses);
        }
    }

    private ProxyResponse mapProxyToResponse(ServiceContractProxy proxy, List<ServiceContractProxyFiles> proxyFiles, List<ServiceContractProxyManagers> proxyManagers) {
        return ProxyResponse.builder()
                .id(proxy.getId())
                .proxyForeignEntityPerson(proxy.getProxyForeignEntityPerson())
                .proxyName(proxy.getProxyName())
                .proxyCustomerIdentifier(proxy.getProxyPersonalIdentifier())
                .proxyEmail(proxy.getProxyEmail())
                .proxyPhone(proxy.getProxyMobilePhone())
                .proxyPowerOfAttorneyNumber(proxy.getProxyAttorneyPowerNumber())
                .proxyData(proxy.getProxyDate())
                .proxyValidTill(proxy.getProxyValidTill())
                .notaryPublic(proxy.getProxyNotaryPublic())
                .registrationNumber(proxy.getProxyRegistrationNumber())
                .areaOfOperation(proxy.getProxyOperationArea())
                .authorizedProxyForeignEntityPerson(proxy.getProxyByProxyForeignEntityPerson())
                .proxyAuthorizedByProxy(proxy.getProxyByProxyName())
                .authorizedProxyCustomerIdentifier(proxy.getProxyByProxyPersonalIdentifier())
                .authorizedProxyEmail(proxy.getProxyByProxyEmail())
                .authorizedProxyPhone(proxy.getProxyByProxyMobilePhone())
                .authorizedProxyPowerOfAttorneyNumber(proxy.getProxyByProxyAttorneyPowerNumber())
                .authorizedProxyData(proxy.getProxyByProxyDate())
                .authorizedProxyValidTill(proxy.getProxyByProxyValidTill())
                .authorizedProxyNotaryPublic(proxy.getProxyByProxyNotaryPublic())
                .authorizedProxyRegistrationNumber(proxy.getProxyByProxyRegistrationNumber())
                .authorizedProxyAreaOfOperation(proxy.getProxyByProxyOperationArea())
                .status(proxy.getStatus())
                .proxyFiles(mapProxyFiles(proxyFiles))
                .proxyManagers(mapProxyManagers(proxyManagers))
                .build();
    }

    private List<ProxyManagersResponse> mapProxyManagers(List<ServiceContractProxyManagers> proxyManagers) {
        List<ProxyManagersResponse> responses = new ArrayList<>();
        for (ServiceContractProxyManagers item : proxyManagers) {
            ProxyManagersResponse proxyManagersResponse = new ProxyManagersResponse();
            Manager manager = getManager(item.getCustomerManagerId());
            if (manager != null) {
                proxyManagersResponse.setManagerName(manager.getName());
                proxyManagersResponse.setManagerMiddleName(manager.getMiddleName());
                proxyManagersResponse.setManagerSurName(manager.getSurname());
            }
            proxyManagersResponse.setId(item.getId());
            proxyManagersResponse.setContractProxyId(item.getContractProxyId());
            proxyManagersResponse.setCustomerManagerId(item.getCustomerManagerId());
            proxyManagersResponse.setStatus(item.getStatus());
            responses.add(proxyManagersResponse);
        }
        return responses;
    }

    private Manager getManager(Long customerManagerId) {
        Optional<Manager> accountManagerOptional = managerRepository.findById(customerManagerId);
        if (accountManagerOptional.isPresent()) {
            Manager manager = accountManagerOptional.get();
            return manager;
        }
        return null;
    }

    private List<ProxyFilesResponse> mapProxyFiles(List<ServiceContractProxyFiles> proxyFiles) {
        List<ProxyFilesResponse> responses = new ArrayList<>();
        for (ServiceContractProxyFiles item : proxyFiles) {
            ProxyFilesResponse proxyFileResponse = new ProxyFilesResponse();
            proxyFileResponse.setId(item.getId());
            proxyFileResponse.setName(item.getName());
            proxyFileResponse.setFileUrl(item.getFileUrl());
            proxyFileResponse.setContractProxyId(item.getContractProxyId());
            proxyFileResponse.setStatus(item.getStatus());
            responses.add(proxyFileResponse);
        }
        return responses;
    }

    private void setCustomerData(ServiceContractDetails details, ServiceContractBasicParametersResponse basicParametersResponse) {
        List<String> errorMessages = new ArrayList<>();
        CustomerDetails customerDetails = customerDetailsRepository.findById(details.getCustomerDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Attached customer do not exists!;"));
        basicParametersResponse.setCustomerId(customerDetails.getCustomerId());
        basicParametersResponse.setCustomerVersionId(customerDetails.getVersionId());
        basicParametersResponse.setCustomerDetailId(customerDetails.getId());
        basicParametersResponse.setBussinessActivity(customerDetails.getBusinessActivity());
        Customer customer = getCustomer(customerDetails.getId(), errorMessages);
        if (customer != null) {
            basicParametersResponse.setCustomerType(customer.getCustomerType());

        }

        StringBuilder stringBuilder = new StringBuilder();
        String identifier = customer.getIdentifier();
        if (!StringUtils.isEmpty(identifier))
            stringBuilder.append(identifier);
        stringBuilder.append(" (");
        stringBuilder.append(customerDetails.getName());
        stringBuilder.append(" ");
        if (customerDetails.getMiddleName() != null) {
            stringBuilder.append(customerDetails.getMiddleName());
            stringBuilder.append(" ");
        }
        if (customerDetails.getLastName() != null) {
            stringBuilder.append(customerDetails.getLastName());
            stringBuilder.append(" ");
        }
        String legalFormName = customerDetailsRepository.getLegalFormName(customerDetails.getId());
        if (StringUtils.isNotEmpty(legalFormName)) {
            stringBuilder.append(legalFormName);
        }
        stringBuilder.append(")");

        Optional<CustomerCommunications> billingComsOptional = communicationsRepository.findByIdAndStatuses(details.getCustomerCommunicationIdForBilling(), List.of(Status.ACTIVE));
        //.orElseThrow(() -> new DomainEntityNotFoundException("id-Attached communication for billing do not exist!;"));
        Optional<CustomerCommunications> contractComsOptional = communicationsRepository.findByIdAndStatuses(details.getCustomerCommunicationIdForContract(), List.of(Status.ACTIVE));
        //.orElseThrow(() -> new DomainEntityNotFoundException("id-Attached communication for contract do not exist!;"));
        if (billingComsOptional.isPresent()) {
            CustomerCommunications billingComs = billingComsOptional.get();
            basicParametersResponse.setBillingCommunicationData(new CustomerCommunicationDataResponse(billingComs.getId(), billingComs.getContactTypeName(), billingComs.getCreateDate()));
            basicParametersResponse.getBillingCommunicationData().setConcatPurposes(customerRepository.getConcatPurposeFromCustomerCommunicationData(billingComs.getId()));
        }
        if (contractComsOptional.isPresent()) {
            CustomerCommunications contractComs = contractComsOptional.get();
            basicParametersResponse.setContractCommunicationData(new CustomerCommunicationDataResponse(contractComs.getId(), contractComs.getContactTypeName(), contractComs.getCreateDate()));
            basicParametersResponse.getContractCommunicationData().setConcatPurposes(customerRepository.getConcatPurposeFromCustomerCommunicationData(contractComs.getId()));
        }

        basicParametersResponse.setCustomerName(stringBuilder.toString());
    }

    private void setServiceData(ServiceContractDetails details, ServiceContractBasicParametersResponse response) {
        ServiceDetails serviceDetails = serviceDetailsRepository.findById(details.getServiceDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Attached service do not exist;"));
        response.setServiceId(serviceDetails.getService().getId());
        response.setServiceDetailId(serviceDetails.getId());
        response.setServiceVersionId(serviceDetails.getVersion());
        response.setServiceName(getServiceFormattedName(serviceDetails.getName(), serviceDetails.getVersion()));
        response.setVersionId(details.getVersionId());
    }

    private String getServiceFormattedName(String name, Long version) {
        return name + " (version %s)".formatted(version);
    }

    private void checkUnwantedCustomer(Long customerId, List<String> errorMessages) {
        Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(customerId, List.of(CustomerStatus.ACTIVE));
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            Optional<UnwantedCustomer> unwantedCustomerOptional =
                    unwantedCustomerRepository.findByIdentifierAndStatusIn(customer.getIdentifier(), List.of(UnwantedCustomerStatus.ACTIVE));
            if (unwantedCustomerOptional.isPresent()) {
                UnwantedCustomer unwantedCustomer = unwantedCustomerOptional.get();
                if (unwantedCustomer.getCreateContractRestriction()) {
                    errorMessages.add("serviceContractBasicParametersCreateRequest.customerId -[customerId] is unwanted customer and has restricted to create contract field checked;");
                }
            }
        }
    }

    private void createBillingContractCommunicationData(Long customerDetailsId, Long id, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        Optional<CustomerCommunications> billingCommunicationsOptional = communicationsRepository.findByIdAndStatuses(id, List.of(Status.ACTIVE));
        if (billingCommunicationsOptional.isEmpty()) {
            errorMessages.add("communicationDataContractId-Communication data for contract not found!;");
            return;
        }
        CustomerCommunications billingCommunications = billingCommunicationsOptional.get();
        if (!customerDetailsId.equals(billingCommunications.getCustomerDetailsId())) {
            errorMessages.add("communicationDataBillingId-Billing communications is invalid;");
            return;
        }
        List<CustomerCommContactPurposes> contractPurposes = commContactPurposesRepository.findByCustomerCommId(billingCommunications.getId(), List.of(Status.ACTIVE));
        Long contractCommunicationId = communicationContactPurposeProperties.getBillingCommunicationId();
        boolean contains = false;
        for (CustomerCommContactPurposes contractPurpose : contractPurposes) {
            if (contractPurpose.getContactPurposeId().equals(contractCommunicationId)) {
                contains = true;
                serviceContractDetails.setCustomerCommunicationIdForBilling(billingCommunications.getId());
            }
        }
        if (!contains) {
            errorMessages.add("communicationDataBillingId-Billing communications is invalid;");
        }
        checkForEmailAndNumber(id, errorMessages, "basicParameters.communicationDataBillingId-Billing communication should have Email and Mobile number contact types!;");
    }

    private void createServiceContractCommunicationData(Long customerDetailsId, Long id, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        Optional<CustomerCommunications> contractCommunicationsOptional = communicationsRepository.findByIdAndStatuses(id, List.of(Status.ACTIVE));
        if (contractCommunicationsOptional.isEmpty()) {
            errorMessages.add("communicationDataContractId-Communication data for contract not found!;");
            return;
        }
        CustomerCommunications contractCommunications = contractCommunicationsOptional.get();
        if (!customerDetailsId.equals(contractCommunications.getCustomerDetailsId())) {
            errorMessages.add("communicationDataContractId-Contract communications is invalid;");
            return;
        }
        List<CustomerCommContactPurposes> contractPurposes = commContactPurposesRepository.findByCustomerCommId(contractCommunications.getId(), List.of(Status.ACTIVE));
        Long contractCommunicationId = communicationContactPurposeProperties.getContractCommunicationId();
        boolean contains = false;
        for (CustomerCommContactPurposes contractPurpose : contractPurposes) {
            if (contractPurpose.getContactPurposeId().equals(contractCommunicationId)) {
                contains = true;
                serviceContractDetails.setCustomerCommunicationIdForContract(contractCommunications.getId());
            }
        }
        if (!contains) {
            errorMessages.add("communicationDataContractId-Contract communications is invalid;");
        }
        checkForEmailAndNumber(id, errorMessages, "basicParameters.communicationDataContractId-Contract communication should have Email and Mobile number contact types!;");
    }

    private void checkForEmailAndNumber(Long id, List<String> errorMessages, String message) {
        checkCommunicationEmailAndNumber(id, errorMessages, message, communicationContactsRepository);
    }

    private Long checkCurrencyId(Long currencyId, List<String> errorMessages) {
        Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
        if (currencyOptional.isEmpty()) {
            errorMessages.add("serviceContractBasicParametersCreateRequest.currencyId-[currencyId] can't find Active currency");
            return null;
        } else return currencyOptional.get().getId();
    }

    private ServiceContractDetails createServiceContractDetails(ServiceContracts serviceContract, ServiceContractCreateRequest serviceContractCreateRequest, ServiceDetails serviceDetails, ServiceContractDetails details, ServiceContractBasicParametersCreateRequest request, List<String> errorMessages) {
        details.setType(request.getContractType());
        serviceContract.setSigningDate(request.getSignInDate());
        if (request.getContractTermUntilAmountIsReachedCheckbox()) {
            details.setContractTermUntilTheAmountValue(request.getContractTermUntilAmountIsReached());
            details.setContractTermUntilTheAmount(true);
            details.setCurrencyId(getCurrencyId(request.getCurrencyId(), errorMessages));
        } else {
            details.setContractTermUntilTheAmount(false);
        }
        return details;
    }

    private Long getCurrencyId(Long currencyId, List<String> errorMessages) {
        if (currencyId != null) {
            Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE));
            if (currencyOptional.isPresent()) {
                return currencyOptional.get().getId();
            } else {
                errorMessages.add("basicParameters.currencyId- [currencyId] Not found currency with id:%s;".formatted(currencyId));
            }
        }
        return null;
    }


    @Transactional
    public ServiceContractDetails edit(ServiceContracts serviceContract, ServiceContractEditRequest request, ServiceContractDetails serviceContractDetails, List<String> errorMessages) {
        ServiceContractBasicParametersEditRequest basicParameters = request.getBasicParameters();
        checkUnwantedCustomer(basicParameters.getCustomerId(), errorMessages);
        ServiceContractDetails contractDetailsUpdated = null;
        ServiceDetails serviceContractUpdated = null;
        boolean updatingNewVersion = request.isSavingAsNewVersion();
        CustomerDetails customerDetails = null;
        if (updatingNewVersion) {
            contractDetailsUpdated = new ServiceContractDetails();
            contractDetailsUpdated.setContractId(serviceContract.getId());
            serviceContractUpdated = getContractForUpdate(serviceContractDetails, basicParameters);
        } else {
            contractDetailsUpdated = serviceContractDetails;
            if (serviceContractDetails.getVersionId() == 1 && (request.getBasicParameters().getSignInDate() == null || request.getBasicParameters().getSignInDate().equals(LocalDate.now()))
                    && !serviceContractDetails.getStartDate().equals(request.getBasicParameters().getStartDate())) {
                errorMessages.add("startDate-Start date must not be changed for this version;");
            }
            serviceContractUpdated = serviceDetailsRepository.findByServiceIdAndVersion(basicParameters.getServiceId(), basicParameters.getServiceVersionId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-service details do not exists on selected contract!"));
            EPService service = serviceContractUpdated.getService();
            if (!service.getStatus().equals(ServiceStatus.ACTIVE)) {
                throw new DomainEntityNotFoundException("id-service do not exists on selected contract!");
            }
            individualServiceCheck(service, serviceContractDetails.getId(), errorMessages);
        }
        serviceDetailsOrderCheck(serviceContractUpdated, errorMessages);
        updateContractDetails(request, contractDetailsUpdated, serviceContract, serviceContractDetails, basicParameters, updatingNewVersion ? serviceContractDetailsRepository.findMaxVersionId(serviceContract.getId()) + 1 : contractDetailsUpdated.getVersionId(), errorMessages);

        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository.findByCustomerIdAndVersionId(basicParameters.getCustomerId(), basicParameters.getCustomerVersionId());
        if (customerDetailsOptional.isEmpty()) {
            errorMessages.add("serviceContractBasicParametersCreateRequest.customerId-Customer version not found");
        } else {
            customerDetails = customerDetailsOptional.get();
            if (!List.of(CustomerDetailStatus.NEW, CustomerDetailStatus.ACTIVE, CustomerDetailStatus.LOST).contains(customerDetails.getStatus())) {
                errorMessages.add("basicParameters.customerVersionId-Can not conclude contract because customer has wrong status!;");
            }
            if (!Objects.equals(serviceContractDetails.getCustomerDetailId(), customerDetails.getId())) {
                Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(basicParameters.getCustomerId(), List.of(CustomerStatus.ACTIVE));
                if (customerOptional.isEmpty()) {
                    errorMessages.add("serviceContractBasicParametersCreateRequest.customerId-Customer can not be found;");
                } else {
                    Customer customer = customerOptional.get();
                    UnwantedCustomer unwantedCustomer = unwantedCustomerService.checkUnwantedCustomer(customer.getIdentifier());
                    if (unwantedCustomer != null) {
                        if (Boolean.TRUE.equals(unwantedCustomer.getCreateContractRestriction()) && unwantedCustomer.getStatus().equals(UnwantedCustomerStatus.ACTIVE)) {
                            errorMessages.add("serviceContractBasicParametersCreateRequest.customerId-Customer is unwanted and restricted to create contract;");
                        }
                    }
                }
                if (serviceContractUpdated.getService().getCustomerIdentifier() == null) {
                    checkSegments(serviceContractUpdated, customerDetails, errorMessages);
                }
            }

            contractDetailsUpdated.setCustomerDetailId(request.getBasicParameters().getCustomerNewDetailsId() != null ?
                    request.getBasicParameters().getCustomerNewDetailsId() :
                    customerDetails.getId());

        }
        contractDetailsUpdated.setServiceDetailId(serviceContractUpdated.getId());
        createBillingContractCommunicationData(customerDetails.getId(), basicParameters.getCommunicationDataForBilling(), contractDetailsUpdated, errorMessages);
        createServiceContractCommunicationData(customerDetails.getId(), basicParameters.getCommunicationDataForContract(), contractDetailsUpdated, errorMessages);
        //updateContractVersionTypes(updatingNewVersion, basicParameters.getContractVersionTypes(), serviceContractDetails, contractDetailsUpdated, errorMessages);

        return contractDetailsUpdated;
    }

    private void serviceDetailsOrderCheck(ServiceDetails serviceContractUpdated, List<String> errorMessages) {
        if (!serviceContractUpdated.getSaleMethods().contains(ServiceSaleMethod.CONTRACT)) {
            errorMessages.add("basicParameters.serviceId-[serviceId] service should have method of sale CONTRACT!;");
        }
    }

    public void updateContractVersionTypes(boolean updateAsNewVersion, List<Long> contractVersionTypeIds, ServiceContractDetails currentServiceContractDetails, ServiceContractDetails updatedServiceContractDetails, List<String> errorMessages) {
        if (updateAsNewVersion) {
            validateContractVersionTypesAndCreate(updatedServiceContractDetails, contractVersionTypeIds);
        } else {
            List<ServiceContractContractVersionTypes> currentActiveServiceContractContractTypes = serviceContractContractVersionTypesRepository
                    .findByContractDetailIdAndStatusIn(currentServiceContractDetails.getId(), List.of(ContractSubObjectStatus.ACTIVE));

            currentActiveServiceContractContractTypes
                    .stream()
                    .filter(ct -> !contractVersionTypeIds.contains(ct.getContractVersionTypeId()))
                    .forEach(ct -> ct.setStatus(ContractSubObjectStatus.DELETED));

            List<Long> currentActiveServiceContractContractTypeIds = currentActiveServiceContractContractTypes
                    .stream()
                    .map(ServiceContractContractVersionTypes::getContractVersionTypeId)
                    .toList();

            List<ServiceContractContractVersionTypes> uncommittedNewlyAddedContractVersionTypes = new ArrayList<>();
            for (int i = 0; i < contractVersionTypeIds.size(); i++) {
                Long contractVersionTypeId = contractVersionTypeIds.get(i);

                if (currentActiveServiceContractContractTypeIds.contains(contractVersionTypeId)) {
                    Optional<ContractVersionType> contractVersionTypeOptional = contractVersionTypesRepository
                            .findByIdAndStatusIn(contractVersionTypeId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));

                    if (contractVersionTypeOptional.isEmpty()) {
                        errorMessages.add("basicParameters.contractVersionTypes[%s]-Contract Version Type with presented ID: [%s] not found;".formatted(i, contractVersionTypeId));
                    }
                } else {
                    Optional<ContractVersionType> contractVersionTypeOptional = contractVersionTypesRepository
                            .findByIdAndStatusIn(contractVersionTypeId, List.of(NomenclatureItemStatus.ACTIVE));

                    if (contractVersionTypeOptional.isPresent()) {
                        uncommittedNewlyAddedContractVersionTypes
                                .add(new ServiceContractContractVersionTypes(
                                        null,
                                        currentServiceContractDetails.getId(),
                                        contractVersionTypeId,
                                        ContractSubObjectStatus.ACTIVE
                                ));
                    } else {
                        errorMessages.add("basicParameters.contractVersionTypes[%s]-Contract Version Type with presented ID: [%s] not found;".formatted(i, contractVersionTypeId));
                    }
                }
            }

            serviceContractContractVersionTypesRepository.saveAll(uncommittedNewlyAddedContractVersionTypes);
        }
    }

    public void sendContractFilesForSignIfApplicable(ServiceContractDetailStatus oldStatus, ServiceContractDetailStatus newStatus, ServiceContractDetails detailsUpdated) {
        List<ServiceContractDetailStatus> validStatusesForSigning = List.of(
                ServiceContractDetailStatus.READY,
                ServiceContractDetailStatus.SIGNED,
                ServiceContractDetailStatus.ENTERED_INTO_FORCE,
                ServiceContractDetailStatus.ACTIVE_IN_TERM,
                ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY,
                ServiceContractDetailStatus.CHANGED_WITH_AGREEMENT
        );

        if (oldStatus.equals(ServiceContractDetailStatus.DRAFT) && validStatusesForSigning.contains(newStatus)) {
            List<Document> documentList = signableDocumentRepository.getDocumentsForServiceContractByContractDetailId(detailsUpdated.getId());
            signerChainManager.startSign(documentList);
        }
    }

    private void checkSegments(ServiceDetails serviceContractUpdated, CustomerDetails customerDetails, List<String> errorMessages) {
        List<CustomerSegment> customerSegments = customerSegmentRepository.findAllByCustomerDetailIdAndStatus(customerDetails.getId(), Status.ACTIVE);
        List<ServiceSegment> serviceSegments = serviceSegmentsRepository.findAllByServiceDetailsIdAndStatus(serviceContractUpdated.getId(), ServiceSubobjectStatus.ACTIVE);
        Set<Long> customerSegmentIds = new HashSet<>();
        boolean contains = false;
        for (CustomerSegment customerSegment : customerSegments) {
            customerSegmentIds.add(customerSegment.getSegment().getId());
        }
        for (ServiceSegment serviceSegment : serviceSegments) {
            if (customerSegmentIds.contains(serviceSegment.getSegment().getId())) {
                contains = true;
                break;
            }
        }
        if (serviceContractUpdated.getGlobalSegment() != null && serviceContractUpdated.getGlobalSegment() && !customerSegmentIds.isEmpty()) {
            contains = true;
        }
        if (!contains) {
            errorMessages.add("serviceContractBasicParametersCreateRequest.customerId-Customer and service segments do not overlap;");
        }
    }

    private ServiceContractDetails updateContractDetails(ServiceContractEditRequest serviceContractEditRequest, ServiceContractDetails contractDetailsUpdated, ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails, ServiceContractBasicParametersEditRequest request, Long versionId, List<String> errorMessages) {
        contractDetailsUpdated.setType(request.getContractType());
        contractDetailsUpdated.setVersionId(versionId);
        Boolean untilAmountIsReachedCheckbox = request.getContractTermUntilAmountIsReachedCheckbox();
        contractDetailsUpdated.setContractTermUntilTheAmount(untilAmountIsReachedCheckbox);
        if (untilAmountIsReachedCheckbox) {
            contractDetailsUpdated.setContractTermUntilTheAmountValue(request.getContractTermUntilAmountIsReached());
            contractDetailsUpdated.setCurrencyId(request.getCurrencyId());
        } else {
            contractDetailsUpdated.setContractTermUntilTheAmountValue(null);
            contractDetailsUpdated.setCurrencyId(null);
        }
        if (!serviceContractEditRequest.savingAsNewVersion) {
            if (!ProductContractStatusChainUtil.serviceContractVersionStatusCanBeChanged(serviceContractDetails.getContractVersionStatus(), request.getContractVersionStatus())) {
                errorMessages.add("serviceContractBasicParametersCreateRequest.contractVersionStatus-Version status can not be changed to provided status!;");
            } else {
                contractDetailsUpdated.setContractVersionStatus(request.getContractVersionStatus());
            }
        } else {
            contractDetailsUpdated.setContractVersionStatus(request.getContractVersionStatus());
        }
        serviceContract.setSigningDate(request.getSignInDate());
        contractDetailsUpdated.setStartDate(checkAndGetStartDate(serviceContract.getId(), request.getStartDate(), serviceContractDetails.getStartDate(), serviceContractDetails, contractDetailsUpdated, serviceContract));
        return contractDetailsUpdated;
    }

    private LocalDate checkAndGetStartDate(Long id, LocalDate startDate, LocalDate currentStartDate, ServiceContractDetails serviceContractDetails, ServiceContractDetails contractDetailsUpdated, ServiceContracts serviceContract) {
        if (serviceContractDetails.getId().equals(contractDetailsUpdated.getId()) && startDate.equals(serviceContractDetails.getStartDate())) {
            return startDate;
        }
        List<ServiceContractDetails> serviceContractDetailsList = serviceContractDetailsRepository.findByContractIdOrderByStartDate(id);
        if (isStartDateValid(serviceContractDetailsList, startDate, serviceContractDetails, serviceContract, contractDetailsUpdated)) {
            return startDate;
        } else {
            throw new ClientException("Contract version with this date: %s already exists;".formatted(startDate), ErrorCode.OPERATION_NOT_ALLOWED);
        }
    }

    private void individualServiceCheck(EPService service, Long serviceContractDetailsId, List<String> errorMessages) {
        if (service.getCustomerIdentifier() == null) {
            return;
        }
        if (serviceContractsRepository.existsByServiceIdAndContractIdNotEquals(service.getId(), serviceContractDetailsId)) {
            errorMessages.add("basicParameters.serviceId-Individual service with id %s is Already used!;".formatted(service.getId()));
        }
    }

    private ServiceDetails getContractForUpdate(ServiceContractDetails serviceContractDetails, ServiceContractBasicParametersEditRequest basicParameters) {
        ServiceDetails serviceContractsDetails = serviceDetailsRepository.findByServiceIdAndVersion(basicParameters.getServiceId(), basicParameters.getServiceVersionId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id- service details do not exists on selected contract!"));
        EPService service = serviceContractsDetails.getService();
        if (!service.getStatus().equals(ServiceStatus.ACTIVE)) {
            throw new DomainEntityNotFoundException("id-Service do not exists on selected contract!");
        }
        if (service.getCustomerIdentifier() != null && serviceContractsDetails.getId().equals(serviceContractDetails.getServiceDetailId())) {
            serviceContractsRepository.existsByServiceId(service.getId());
        } else if (service.getCustomerIdentifier() != null) {
            serviceService.copy(serviceContractsDetails.getService().getId(), serviceContractDetails.getVersionId());   //TODO find out where's clone service functionality
        }
        return serviceContractsDetails;
    }

    public void updateFiles(ServiceContractDetails serviceContractDetails, LinkedHashSet<Long> files, Long id, List<String> errorMessages) {
        List<ServiceContractFiles> dbFiles = serviceContractFilesRepository.findByContractDetailIdAndStatus(id, EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(files)) {
            List<ServiceContractFiles> filesToUpdate = new ArrayList<>();
            List<ServiceContractFiles> filesToDelete = new ArrayList<>();
            LinkedHashSet<Long> filesToCreate = new LinkedHashSet<>();

            for (ServiceContractFiles dbFile : dbFiles) {
                if (files.contains(dbFile.getId())) {
                    filesToUpdate.add(dbFile);
                    files.remove(dbFile.getId());
                } else {
                    dbFile.setStatus(EntityStatus.DELETED);
                    filesToDelete.add(dbFile);
                }
            }
            for (Long fileId : files) {
                ServiceContractFiles newFile = new ServiceContractFiles();
                newFile.setId(fileId);
                filesToCreate.add(newFile.getId());
            }
            if (CollectionUtils.isNotEmpty(filesToCreate)) {
                createContractFiles(filesToCreate, serviceContractDetails, errorMessages);
            }
            if (CollectionUtils.isNotEmpty(filesToDelete)) {
                serviceContractFilesRepository.saveAll(filesToDelete);
            }
        } else {
            if (CollectionUtils.isNotEmpty(dbFiles)) {
                for (ServiceContractFiles item : dbFiles) {
                    item.setStatus(EntityStatus.DELETED);
                    serviceContractFilesRepository.save(item);
                }
            }
        }
    }

    public void updateDocuments(ServiceContractDetails detailsUpdated, LinkedHashSet<Long> documents, Long id, List<String> errorMessages) {
        List<ServiceContractAdditionalDocuments> dbDocuments = serviceContractAdditionalDocumentsRepository.findByContractDetailIdAndStatus(id, EntityStatus.ACTIVE);
        if (CollectionUtils.isNotEmpty(documents)) {
            List<ServiceContractAdditionalDocuments> documentsToUpdate = new ArrayList<>();
            List<ServiceContractAdditionalDocuments> documentsToDelete = new ArrayList<>();
            LinkedHashSet<Long> filesToCreate = new LinkedHashSet<>();

            for (ServiceContractAdditionalDocuments dbFile : dbDocuments) {
                if (documents.contains(dbFile.getId())) {
                    documentsToUpdate.add(dbFile);
                    documents.remove(dbFile.getId());
                } else {
                    dbFile.setStatus(EntityStatus.DELETED);
                    documentsToDelete.add(dbFile);
                }
            }
            for (Long fileId : documents) {
                ServiceContractFiles newFile = new ServiceContractFiles();
                newFile.setId(fileId);
                filesToCreate.add(newFile.getId());
            }
            if (CollectionUtils.isNotEmpty(filesToCreate)) {
                createContractDocuments(filesToCreate, detailsUpdated, errorMessages);
            }
            if (CollectionUtils.isNotEmpty(documentsToDelete)) {
                serviceContractAdditionalDocumentsRepository.saveAll(documentsToDelete);
            }
        } else {
            if (CollectionUtils.isNotEmpty(dbDocuments)) {
                for (ServiceContractAdditionalDocuments item : dbDocuments) {
                    item.setStatus(EntityStatus.DELETED);
                    serviceContractAdditionalDocumentsRepository.save(item);
                }
            }
        }
    }

    public Page<ServiceContractServiceListingMiddleResponse> getServices(ServiceContractProductListingRequest request) {
        return serviceRepository.searchForContract(
                request.getCustomerDetailId(),
                permissionService.getLoggedInUserId(),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt() == null ? "" : request.getPrompt()),
                request.getServiceContractId(),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "customer_identifier", "sd.name")));
    }

    public Page<ServiceContractServiceListingMiddleResponse> getServicesForExpressContract(ServiceContractProductListingRequest request) {
        return serviceRepository.searchForContractForExpressContract(
                request.getCustomerDetailId(),
                permissionService.getLoggedInUserId(),
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt() == null ? "" : request.getPrompt()),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "customer_identifier", "sd.name")));
    }
}

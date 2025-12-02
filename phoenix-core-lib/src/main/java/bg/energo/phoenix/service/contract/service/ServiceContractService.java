package bg.energo.phoenix.service.contract.service;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.customAnotations.contract.service.ServiceContractThirdPageFields;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractAdditionalDocuments;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractDetails;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractFiles;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.entity.product.service.ServiceContractServiceListingResponse;
import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.contract.service.*;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.product.ExcludeVersions;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;
import bg.energo.phoenix.model.enums.product.service.ServiceSaleMethod;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import bg.energo.phoenix.model.request.contract.service.ServiceContractBasicParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractCreateRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractListingRequest;
import bg.energo.phoenix.model.request.contract.service.ServiceContractServiceParametersCreateRequest;
import bg.energo.phoenix.model.request.contract.service.edit.*;
import bg.energo.phoenix.model.request.product.service.ServiceContractProductListingRequest;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractListingResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractResponse;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractVersions;
import bg.energo.phoenix.model.response.service.ServiceContractTermShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractAdditionalDocumentsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractDetailsRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractFilesRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.contract.activity.ServiceContractActivityService;
import bg.energo.phoenix.service.contract.relatedEntities.RelatedContractsAndOrdersService;
import bg.energo.phoenix.service.customer.statusChangeEvent.CustomerStatusChangeEventPublisher;
import bg.energo.phoenix.service.product.service.ServiceRelatedEntitiesService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.contract.ContractUtils;
import bg.energo.phoenix.util.contract.product.ProductContractStatusChainUtil;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.versionDates.CalculateVersionDates;
import bg.energo.phoenix.util.versionDates.VersionWithDatesModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionContextEnum.SERVICE_CONTRACTS;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceContractService {
    private final ServiceContractAdditionalDocumentsRepository serviceContractAdditionalDocumentsRepository;
    private final ServiceContractFilesRepository serviceContractFilesRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerRepository customerRepository;
    private final ServiceContractBasicParametersService serviceContractBasicParametersService;
    private final ServiceContractAdditionalParametersService serviceContractAdditionalParametersService;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractDetailsRepository serviceContractDetailsRepository;
    private final InvoiceRepository invoiceRepository;
    private final ServiceContractActivityService serviceContractActivityService;
    private final ServiceContractServiceParametersService serviceParametersService;
    private final ServiceRepository serviceRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final RelatedContractsAndOrdersService relatedContractsAndOrdersService;
    private final PermissionService permissionService;
    private final ServiceRelatedEntitiesService serviceRelatedEntitiesService;
    private final TaskService taskService;
    private final ContractUtils contractUtils;
    private final CustomerStatusChangeEventPublisher customerStatusChangeEventPublisher;
    private final ServiceContractDateService contractDateService;
    private final EDMSAttributeProperties attributeProperties;
    private final FileArchivationService fileArchivationService;
    @Value("${contract.without_term.value}")
    private String maxDate;

    @Transactional
    public Long create(ServiceContractCreateRequest request) {
        log.debug("Creating service contract with id: {}", request);
        List<String> errorMessages = new ArrayList<>();
        checkStatusesForCreateContract(request, errorMessages);
        EPService service = getService(request.getBasicParameters().getServiceId());
        ServiceDetails serviceDetails = getServiceDetailsWithVersionId(service, request.getBasicParameters().getServiceVersionId());
        checkRequestValidity(request, service, serviceDetails, errorMessages);
        CustomerDetails customerDetails = validateCustomer(request.getBasicParameters().getCustomerId(), request.getBasicParameters().getCustomerVersionId(), errorMessages);


        ServiceContracts serviceContract = new ServiceContracts();
        serviceContract.setStatus(EntityStatus.ACTIVE);
        serviceContract.setContractNumber(contractUtils.getNextContractNumber());
        serviceContract.setSubStatus(request.getBasicParameters().getDetailsSubStatus());
        serviceContract.setContractStatus(request.getBasicParameters().getContractStatus());
        serviceContract.setStatusModifyDate(request.getBasicParameters().getContractStatusModifyDate());

        serviceContract.setCreateDate(LocalDateTime.now());
        serviceContractsRepository.saveAndFlush(serviceContract);
        ServiceContractDetails serviceContractDetails = new ServiceContractDetails();
        ServiceContractThirdPageFields sourceView = serviceParametersService.thirdPageFields(serviceDetails);
        validateDatesForCreate(sourceView, serviceContract, serviceContractDetails, request, errorMessages);
        if (serviceContract.getSigningDate() != null && serviceContract.getSigningDate().isBefore(LocalDate.now())) {
            serviceContractDetails.setStartDate(serviceContract.getSigningDate());
        } else {
            serviceContractDetails.setStartDate(LocalDate.now());
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        serviceContractBasicParametersService.create(service, serviceDetails, request, serviceContract, serviceContractDetails, serviceDetails, errorMessages);
        serviceContractAdditionalParametersService.create(request.getAdditionalParameters(), serviceContractDetails, errorMessages);
        serviceParametersService.create(serviceContract, request, serviceDetails, serviceContractDetails, sourceView, errorMessages);
        calculateAdditionalAgreementSuffix(serviceContractDetails, serviceContract);

        if (Objects.nonNull(customerDetails)) {
            if (!serviceRepository.validateContractServiceAndCustomerOnCreation(customerDetails.getId(), serviceDetails.getId())) {
                log.error("You are not allowed to create a contract because service is not valid");
                errorMessages.add("You are not allowed to create a contract because service is not valid;");
            }
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        serviceContractsRepository.saveAndFlush(serviceContract);
        serviceContractDetails.setCreateDate(LocalDateTime.now());
        serviceContractDetailsRepository.saveAndFlush(serviceContractDetails);
        serviceParametersService.fillSubActivityDetails(request.getServiceParameters(), serviceContractDetails, errorMessages);
        serviceContractBasicParametersService.validateContractVersionTypesAndCreate(serviceContractDetails, request.getBasicParameters().getContractVersionTypes());
        serviceContractAdditionalParametersService.createSubObjectRelations(serviceContractDetails, errorMessages, request.getAdditionalParameters());
        serviceParametersService.createSubObjects(serviceContract, request.getServiceParameters(), serviceContractDetails, errorMessages);
        serviceContractBasicParametersService.createServiceContractProxy(getCustomer(request.getBasicParameters().getCustomerId()), request.getBasicParameters().getCustomerVersionId(), request.getBasicParameters().getProxy(), serviceContractDetails, errorMessages);
        serviceContractBasicParametersService.createContractFiles(request.getBasicParameters().getFiles(), serviceContractDetails, errorMessages);
        archiveFiles(serviceContractDetails);
        serviceContractBasicParametersService.createContractDocuments(request.getBasicParameters().getDocuments(), serviceContractDetails, errorMessages);
        archiveDocuments(serviceContractDetails);
        serviceParametersService.fillServiceAdditionalParams(request.getServiceParameters(), false, serviceDetails, serviceContractDetails, errorMessages);
        relatedContractsAndOrdersService.createEntityRelations(
                serviceContract.getId(),
                RelatedEntityType.SERVICE_CONTRACT,
                request.getBasicParameters().getRelatedEntities(),
                errorMessages
        );

        if (!serviceRelatedEntitiesService.canCreateServiceContractWithServiceAndCustomer(
                request.getBasicParameters().getServiceId(),
                request.getBasicParameters().getServiceVersionId(),
                request.getBasicParameters().getCustomerId(),
                errorMessages
        )) {
            log.error("You are not allowed to create a contract because the service has related dependencies.");
            errorMessages.add("You are not allowed to create a contract because the service has related dependencies.");
        }


        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (serviceContractDetails.getCustomerDetailId() != null)
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(List.of(serviceContractDetails.getCustomerDetailId()));
        return serviceContract.getId();
    }


    private void archiveFiles(ServiceContractDetails contractDetails) {
        Optional<ServiceContracts> serviceContractsOptional = serviceContractsRepository
                .findById(contractDetails.getContractId());

        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository
                .findById(contractDetails.getCustomerDetailId());

        Optional<Customer> customerOptional = Optional.empty();
        if (customerDetailsOptional.isPresent()) {
            customerOptional = customerRepository.findById(customerDetailsOptional.get().getCustomerId());
        }

        if (serviceContractsOptional.isPresent()) {
            List<ServiceContractFiles> serviceContractFiles = serviceContractFilesRepository.findServiceContractFilesByContractDetailIdAndStatusIn(contractDetails.getId(), List.of(EntityStatus.ACTIVE));
            if (CollectionUtils.isNotEmpty(serviceContractFiles)) {
                for (ServiceContractFiles serviceContractFile : serviceContractFiles) {
                    try {
                        serviceContractFile.setNeedArchive(true);
                        serviceContractFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_FILE);
                        serviceContractFile.setAttributes(
                                List.of(
                                        new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_FILE),
                                        new Attribute(attributeProperties.getDocumentNumberGuid(), serviceContractsOptional.map(ServiceContracts::getContractNumber).orElse("")),
                                        new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                        new Attribute(attributeProperties.getCustomerIdentifierGuid(), customerOptional.map(Customer::getIdentifier).orElse("")),
                                        new Attribute(attributeProperties.getCustomerNumberGuid(), customerOptional.isPresent() ? customerOptional.get().getCustomerNumber() : ""),
                                        new Attribute(attributeProperties.getSignedGuid(), false)
                                )
                        );

                        fileArchivationService.archive(serviceContractFile);
                    } catch (Exception e) {
                        log.error("Cannot archive file: ", e);
                    }
                }
            }
        }
    }

    private void archiveDocuments(ServiceContractDetails contractDetails) {
        Optional<ServiceContracts> serviceContractsOptional = serviceContractsRepository
                .findById(contractDetails.getContractId());

        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository
                .findById(contractDetails.getCustomerDetailId());

        Optional<Customer> customerOptional = Optional.empty();
        if (customerDetailsOptional.isPresent()) {
            customerOptional = customerRepository.findById(customerDetailsOptional.get().getCustomerId());
        }

        if (serviceContractsOptional.isPresent()) {
            List<ServiceContractAdditionalDocuments> serviceContractAdditionalDocuments = serviceContractAdditionalDocumentsRepository.findByContractDetailIdAndStatus(contractDetails.getId(), EntityStatus.ACTIVE);
            if (CollectionUtils.isNotEmpty(serviceContractAdditionalDocuments)) {
                for (ServiceContractAdditionalDocuments serviceContractAdditionalDocument : serviceContractAdditionalDocuments) {
                    try {
                        serviceContractAdditionalDocument.setNeedArchive(true);
                        serviceContractAdditionalDocument.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_DOCUMENT);
                        serviceContractAdditionalDocument.setAttributes(
                                List.of(
                                        new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_CONTRACT_DOCUMENT),
                                        new Attribute(attributeProperties.getDocumentNumberGuid(), serviceContractsOptional.map(ServiceContracts::getContractNumber).orElse("")),
                                        new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                        new Attribute(attributeProperties.getCustomerIdentifierGuid(), customerOptional.map(Customer::getIdentifier).orElse("")),
                                        new Attribute(attributeProperties.getCustomerNumberGuid(), customerOptional.isPresent() ? customerOptional.get().getCustomerNumber() : ""),
                                        new Attribute(attributeProperties.getSignedGuid(), false)
                                )
                        );

                        fileArchivationService.archive(serviceContractAdditionalDocument);
                    } catch (Exception e) {
                        log.error("Cannot archive file: ", e);
                    }
                }
            }
        }
    }

    private CustomerDetails validateCustomer(Long customerId, Long customerVersionId, List<String> errorMessages) {
        Optional<CustomerDetails> customerDetailsOptional = customerDetailsRepository
                .findByCustomerIdAndVersionIdAndStatusIn(customerId, customerVersionId, List.of(CustomerStatus.ACTIVE));

        if (customerDetailsOptional.isEmpty()) {
            errorMessages.add("basicParameters.customerId-Customer with presented id and version not found;");
            return null;
        }

        return customerDetailsOptional.get();
    }

    private void checkStatusesForCreateContract(ServiceContractCreateRequest request, List<String> errorMessages) {
        ServiceContractDetailStatus contractDetailStatus = request.getBasicParameters().getContractStatus();
        ServiceContractDetailsSubStatus detailsSubStatus = request.getBasicParameters().getDetailsSubStatus();
        if (contractDetailStatus.equals(ServiceContractDetailStatus.DRAFT)) {
            if (!detailsSubStatus.equals(ServiceContractDetailsSubStatus.DRAFT)) {
                errorMessages.add("serviceContractBasicParametersCreateRequest.detailsSubStatus-[detailsSubStatus] can't be different from DRAFT");
            }
        } else if (contractDetailStatus.equals(ServiceContractDetailStatus.READY)) {
            if (!detailsSubStatus.equals(ServiceContractDetailsSubStatus.READY)) {
                errorMessages.add("serviceContractBasicParametersCreateRequest.detailsSubStatus-[detailsSubStatus] can't be different from READY");
            }
        } else if (contractDetailStatus.equals(ServiceContractDetailStatus.SIGNED) ||
                contractDetailStatus.equals(ServiceContractDetailStatus.ENTERED_INTO_FORCE)) {
            if (!contractDetailStatus.isCorrectSubStatus(detailsSubStatus)) {
                errorMessages.add("serviceContractBasicParametersCreateRequest.detailsSubStatus-[detailsSubStatus] statuses are incorrect;");
            }
        } else {
            errorMessages.add("serviceContractBasicParametersCreateRequest.detailsSubStatus-[detailsSubStatus] statuses are incorrect;serviceContractBasicParametersCreateRequest.contractStatus-[contractStatus] statuses are incorrect;");
        }
    }

    private void calculateAdditionalAgreementSuffix(ServiceContractDetails serviceContractDetails, ServiceContracts serviceContract) {
        Optional<Integer> contractAgreementSuffixValue = serviceContractDetailsRepository.findContractAgreementSuffixValue(serviceContract.getId());
        if ((serviceContractDetails.getType().equals(ServiceContractContractType.ADDITIONAL_AGREEMENT) || serviceContractDetails.getType().equals(ServiceContractContractType.EX_OFFICIO_AGREEMENT)) && serviceContractDetails.getAgreementSuffix() == null) {
            if (contractAgreementSuffixValue.isEmpty()) {
                serviceContractDetails.setAgreementSuffix(1);
            } else {
                Integer suffix = contractAgreementSuffixValue.get();
                serviceContractDetails.setAgreementSuffix(suffix + 1);
            }
        }
    }

    private Customer getCustomer(Long id) {
        Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(id, List.of(CustomerStatus.ACTIVE));
        return customerOptional.orElse(null);
    }

    private void checkRequestValidity(ServiceContractCreateRequest request, EPService service, ServiceDetails serviceDetails, List<String> errorMessages) {
        ServiceContractServiceParametersCreateRequest serviceParameters = request.getServiceParameters();
        if (serviceDetails.getExecutionLevel().equals(ServiceExecutionLevel.CONTRACT)) {
            if (!CollectionUtils.isEmpty(serviceParameters.getPodIds()) || !CollectionUtils.isEmpty(serviceParameters.getUnrecognizedPods())) {
                errorMessages.add("serviceParameters.podIds-[podIds] should be empty because service execution leve is CONTRACT;serviceParameters.unrecognizedPods-[unrecognizedPods] should be empty because service execution leve is CONTRACT;");
            }
            if (CollectionUtils.isEmpty(serviceParameters.getContractNumbers())) {
                errorMessages.add("serviceParameters.contractNumbers-[contractNumbers] shouldn't be empty because service execution leve is CONTRACT;");
            }
        }
        if (serviceDetails.getExecutionLevel().equals(ServiceExecutionLevel.POINT_OF_DELIVERY)) {
            if (!CollectionUtils.isEmpty(serviceParameters.getContractNumbers())) {
                errorMessages.add("serviceParameters.contractNumbers-[contractNumbers] should be empty because service execution leve is POINT_OF_DELIVERY;");
            }
            if (CollectionUtils.isEmpty(serviceParameters.getPodIds()) && CollectionUtils.isEmpty(serviceParameters.getUnrecognizedPods())) {
                errorMessages.add("serviceParameters.podIds-[podIds] shouldn't be empty because service execution leve is POINT_OF_DELIVERY;serviceParameters.unrecognizedPods-[unrecognizedPods] shouldn't be empty because service execution leve is POINT_OF_DELIVERY;");
            }
        }
        if (!serviceDetails.getSaleMethods().contains(ServiceSaleMethod.CONTRACT)) {
            errorMessages.add("Cannot create Service Contract because Service sale methods does not contains CONTRACT option;");
        }

    }

    private ContractEntryIntoForce getEnumValueForForce(ContractEntryIntoForce entryIntoForce, List<ContractEntryIntoForce> contractEntryIntoForces) {
        for (ContractEntryIntoForce item : contractEntryIntoForces) {
            if (item.equals(ContractEntryIntoForce.SIGNING) && entryIntoForce.equals(ContractEntryIntoForce.SIGNING)) {
                return ContractEntryIntoForce.SIGNING;
            }
            if (item.equals(ContractEntryIntoForce.EXACT_DAY) && entryIntoForce.equals(ContractEntryIntoForce.EXACT_DAY)) {
                return ContractEntryIntoForce.EXACT_DAY;
            }
            if (item.equals(ContractEntryIntoForce.DATE_CHANGE_OF_CBG) && entryIntoForce.equals(ContractEntryIntoForce.DATE_CHANGE_OF_CBG)) {
                return ContractEntryIntoForce.DATE_CHANGE_OF_CBG;
            }
            if (item.equals(ContractEntryIntoForce.FIRST_DELIVERY) && entryIntoForce.equals(ContractEntryIntoForce.FIRST_DELIVERY)) {
                return ContractEntryIntoForce.FIRST_DELIVERY;
            }
            if (item.equals(ContractEntryIntoForce.MANUAL) && entryIntoForce.equals(ContractEntryIntoForce.MANUAL)) {
                return ContractEntryIntoForce.MANUAL;
            }
        }
        return null;
    }

    private StartOfContractInitialTerm getEnumValue(StartOfContractInitialTerm contractInitialTerm, List<ContractEntryIntoForce> contractEntryIntoForces) {
        for (ContractEntryIntoForce item : contractEntryIntoForces) {
            if (item.equals(ContractEntryIntoForce.SIGNING) && contractInitialTerm.equals(StartOfContractInitialTerm.SIGNING)) {
                return StartOfContractInitialTerm.SIGNING;
            }
            if (item.equals(ContractEntryIntoForce.EXACT_DAY) && contractInitialTerm.equals(StartOfContractInitialTerm.EXACT_DATE)) {
                return StartOfContractInitialTerm.EXACT_DATE;
            }
            if (item.equals(ContractEntryIntoForce.DATE_CHANGE_OF_CBG) && contractInitialTerm.equals(StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG)) {
                return StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG;
            }
            if (item.equals(ContractEntryIntoForce.FIRST_DELIVERY) && contractInitialTerm.equals(StartOfContractInitialTerm.FIRST_DELIVERY)) {
                return StartOfContractInitialTerm.FIRST_DELIVERY;
            }
            if (item.equals(ContractEntryIntoForce.MANUAL) && contractInitialTerm.equals(StartOfContractInitialTerm.MANUAL)) {
                return StartOfContractInitialTerm.MANUAL;
            }
        }
        return null;
    }

    private void checkEditRequestRequestValidity(ServiceContractEditRequest request, ServiceDetails serviceDetails, List<String> errorMessages) {
        ServiceContractServiceParametersEditRequest serviceParameters = request.getServiceParameters();
        List<ServiceContractUnrecognizedPodsEditRequest> unrecognizedPodIds = serviceParameters.getUnrecognizedPodsEditList();
        List<ServiceContractPodsEditRequest> podIds = serviceParameters.getPodsEditList();
        List<ServiceContractContractNumbersEditRequest> contractNumbersEditList = serviceParameters.getContractNumbersEditList();
        if (serviceDetails.getExecutionLevel().equals(ServiceExecutionLevel.CONTRACT)) {
            if (!CollectionUtils.isEmpty(podIds) || !CollectionUtils.isEmpty(unrecognizedPodIds)) {
                errorMessages.add("serviceParameters.podsEditList-[podsEditList] should be empty because service execution leve is CONTRACT;serviceParameters.unrecognizedPods-[unrecognizedPods] should be empty because service execution leve is CONTRACT;");
            }
            if (CollectionUtils.isEmpty(contractNumbersEditList)) {
                errorMessages.add("serviceParameters.contractNumbers-[contractNumbers] shouldn't be empty because service execution leve is CONTRACT;");
            }
        }
        if (serviceDetails.getExecutionLevel().equals(ServiceExecutionLevel.POINT_OF_DELIVERY)) {
            if (!CollectionUtils.isEmpty(contractNumbersEditList)) {
                errorMessages.add("serviceParameters.contractNumbers-[contractNumbers] should be empty because service execution leve is POINT_OF_DELIVERY;");
            }
            if (CollectionUtils.isEmpty(podIds) && CollectionUtils.isEmpty(unrecognizedPodIds)) {
                errorMessages.add("serviceParameters.podIds-[podIds] shouldn't be empty because service execution leve is POINT_OF_DELIVERY;serviceParameters.unrecognizedPods-[unrecognizedPods] shouldn't be empty because service execution leve is POINT_OF_DELIVERY;");
            }
        }

    }

    private ServiceDetails getServiceDetails(Long lastServiceDetailId) {
        if (lastServiceDetailId != null) {
            return serviceDetailsRepository.findById(lastServiceDetailId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("serviceContractBasicParametersCreateRequest.serviceId- [serviceId] can't find service details;"));

        } else
            throw new DomainEntityNotFoundException("serviceContractBasicParametersCreateRequest.serviceId- [serviceId] can't find service details;");
    }

    private ServiceDetails getServiceDetailsWithVersionId(EPService service, Long versionId) {
        if (versionId != null) {
            return serviceDetailsRepository.findByServiceAndVersion(service, versionId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("serviceContractBasicParametersCreateRequest.serviceId- [serviceId] can't find service details;"));
        } else
            throw new DomainEntityNotFoundException("serviceContractBasicParametersCreateRequest.serviceId- [serviceId] can't find service details;");
    }

    private EPService getService(Long id) {
        return serviceRepository.findByIdAndStatusIn(id, List.of(ServiceStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("serviceContractBasicParametersCreateRequest.serviceId- [serviceId] can't find active service;"));
    }

    @Transactional
    public Long update(ServiceContractEditRequest request, Long id, Long versionId, boolean fromMassImport) {
        log.debug("Editing service contract with id: {}, version id: {}", id, versionId);
        List<String> errorMessages = new ArrayList<>();

        ServiceContracts serviceContract = serviceContractsRepository
                .findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Contract can't be found;"));

        ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository
                .findByContractIdAndVersionId(id, versionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("versionId- Contract version not found;"));

        if (isLockedByInvoice(id)) {
            if (!permissionService.permissionContextContainsPermissions(SERVICE_CONTRACTS, List.of(SERVICE_CONTRACT_EDIT_LOCKED))) {
                throw new ClientException("Contract is locked by invoice!;", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

        Long oldCustomerDetailId = serviceContractDetails.getCustomerDetailId();
        checkServiceContractDates(serviceContract, serviceContractDetails, request.getBasicParameters(), errorMessages);
        EPService service = getService(request.getBasicParameters().getServiceId());
        ServiceDetails serviceDetails = getServiceDetailsWithVersionId(service, request.getBasicParameters().getServiceVersionId());
        checkEditRequestRequestValidity(request, serviceDetails, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        checkIfUserHasPermission(serviceContract, fromMassImport);

        ServiceContractThirdPageFields thirdPageFields = thirdTabFields(serviceDetails.getId());
        validateContractStatus(request, serviceContract, thirdPageFields, errorMessages);
        ServiceContractDetailStatus oldStatus = serviceContract.getContractStatus();
        updateContractStatus(serviceContract, request, errorMessages);
        validateDatesForUpdate(thirdPageFields, serviceContract, serviceContractDetails, request, errorMessages);

        serviceContractsRepository.saveAndFlush(serviceContract);

        ServiceContractDetails detailsUpdated = serviceContractBasicParametersService.edit(serviceContract, request, serviceContractDetails, errorMessages);
        serviceContractAdditionalParametersService.update(serviceContractDetails, detailsUpdated, errorMessages, request);
        detailsUpdated.setGuaranteeContract(request.getServiceParameters().isGuaranteeContract());
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        serviceContractDetailsRepository.saveAndFlush(detailsUpdated);
        serviceContractBasicParametersService.updateContractVersionTypes(request.isSavingAsNewVersion(), request.getBasicParameters().getContractVersionTypes(), serviceContractDetails, detailsUpdated, errorMessages);
        serviceContractAdditionalParametersService.updateSubObjects(detailsUpdated, errorMessages, request, request.getAdditionalParameters());
        serviceParametersService.update(thirdPageFields, request, detailsUpdated, serviceContract, serviceDetails, errorMessages);
        serviceParametersService.updateSubObjects(serviceContract, request, detailsUpdated, serviceContractDetails, errorMessages);
        serviceContractDetailsRepository.saveAndFlush(detailsUpdated);
        serviceContractsRepository.saveAndFlush(serviceContract);
        calculateAdditionalAgreementSuffix(detailsUpdated, serviceContract);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        if (request.isSavingAsNewVersion()) {
            serviceContractBasicParametersService.createServiceContractProxy(getCustomer(request.getBasicParameters().getCustomerId()), request.getBasicParameters().getCustomerVersionId(), request.getBasicParameters().getProxy(), detailsUpdated, errorMessages);
            serviceContractBasicParametersService.copyContractFiles(request.getBasicParameters().getFiles(), detailsUpdated, errorMessages);
            serviceContractBasicParametersService.copyContractDocuments(request.getBasicParameters().getDocuments(), detailsUpdated, errorMessages);
        } else {
            serviceContractBasicParametersService.updateProxy(request.getBasicParameters().getCustomerId(), request.getBasicParameters().getCustomerVersionId(), request.getBasicParameters().getProxy(), serviceContractDetails.getId(), errorMessages);
            serviceContractBasicParametersService.updateFiles(detailsUpdated, request.getBasicParameters().getFiles(), serviceContractDetails.getId(), errorMessages);
            serviceContractBasicParametersService.updateDocuments(detailsUpdated, request.getBasicParameters().getDocuments(), serviceContractDetails.getId(), errorMessages);
        }

        updateVersionStartAndEndDates(serviceContract, detailsUpdated);

        relatedContractsAndOrdersService.updateEntityRelations(
                serviceContract.getId(),
                RelatedEntityType.SERVICE_CONTRACT,
                request.getBasicParameters().getRelatedEntities(),
                errorMessages
        );
        serviceParametersService.fillServiceAdditionalParams(request.getServiceParameters(), request.isSavingAsNewVersion(), serviceDetails, detailsUpdated, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        serviceContractBasicParametersService.sendContractFilesForSignIfApplicable(oldStatus, serviceContract.getContractStatus(), detailsUpdated);
        archiveFiles(detailsUpdated);
        archiveDocuments(detailsUpdated);

        Set<Long> customerDetailIDsTOCheck = new HashSet<>();
        if (oldCustomerDetailId != null)
            customerDetailIDsTOCheck.add(oldCustomerDetailId);
        if (detailsUpdated.getCustomerDetailId() != null)
            customerDetailIDsTOCheck.add(detailsUpdated.getCustomerDetailId());

        if (!customerDetailIDsTOCheck.isEmpty())
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerDetailIDsTOCheck.stream().toList());
        return serviceContract.getId();
    }

    private void updateVersionStartAndEndDates(ServiceContracts serviceContract, ServiceContractDetails serviceContractDetails) {
        List<ServiceContractDetails> serviceContractVersions = serviceContractsRepository.findServiceContractDetailsByContractId(serviceContract.getId());
        List<VersionWithDatesModel> versionWithDatesModels = serviceContractVersions.stream().map(VersionWithDatesModel::new).collect(Collectors.toList());

        List<VersionWithDatesModel> updatedVersionWithDatesModels = CalculateVersionDates.calculateVersionEndDates(versionWithDatesModels, serviceContractDetails.getStartDate(), Math.toIntExact(serviceContractDetails.getVersionId()));

        serviceContractVersions
                .forEach(pcv -> updatedVersionWithDatesModels.stream()
                        .filter(v -> Objects.equals(v.getVersionId(), Math.toIntExact(pcv.getVersionId())))
                        .findFirst()
                        .ifPresent(model -> {
                            pcv.setEndDate(model.getEndDate());
                            pcv.setStartDate(model.getStartDate());
                        })
                );
    }

    private void validateDatesForUpdate(ServiceContractThirdPageFields sourceView,
                                        ServiceContracts serviceContract,
                                        ServiceContractDetails serviceContractDetails,
                                        ServiceContractEditRequest request,
                                        List<String> errorMessages) {
        ServiceContractBasicParametersEditRequest basicParameters = request.getBasicParameters();
        ServiceContractServiceParametersEditRequest serviceParameters = request.getServiceParameters();
        contractDateService.validateDates(
                serviceContract,
                serviceContractDetails,
                basicParameters.getSignInDate(),
                basicParameters.getEntryIntoForceDate(),
                basicParameters.getStartOfTheInitialTermOfTheContract(),
                basicParameters.getContractStatus(),
                basicParameters.getDetailsSubStatus(),
                serviceParameters,
                sourceView,
                errorMessages
        );
        contractDateService.validateSourceView(sourceView, serviceParameters, errorMessages);
        LocalDate termEndDate = contractDateService.validateContractTerm(
                serviceContract,
                serviceContractDetails,
                sourceView,
                serviceParameters,
                errorMessages
        );
        contractDateService.setContractTermEndDate(serviceContract, basicParameters.getContractStatus(), basicParameters.getContractTermEndDate(), termEndDate);
        contractDateService.validatePerpetuity(
                serviceContract,
                sourceView,
                basicParameters.getContractStatus(),
                basicParameters.getTerminationDate(),
                basicParameters.getPerpetuityDate(),
                serviceParameters,
                errorMessages
        );
    }

    private void validateDatesForCreate(ServiceContractThirdPageFields sourceView,
                                        ServiceContracts serviceContract,
                                        ServiceContractDetails serviceContractDetails,
                                        ServiceContractCreateRequest request,
                                        List<String> errorMessages) {
        ServiceContractBasicParametersCreateRequest basicParameters = request.getBasicParameters();
        ServiceContractServiceParametersCreateRequest serviceParameters = request.getServiceParameters();
        contractDateService.validateDates(
                serviceContract,
                serviceContractDetails,
                basicParameters.getSignInDate(),
                basicParameters.getEntryIntoForceDate(),
                basicParameters.getStartOfTheInitialTermOfTheContract(),
                basicParameters.getContractStatus(),
                basicParameters.getDetailsSubStatus(),
                serviceParameters,
                sourceView,
                errorMessages
        );
        contractDateService.validateSourceView(sourceView, serviceParameters, errorMessages);
        LocalDate termEndDate = contractDateService.validateContractTerm(
                serviceContract,
                serviceContractDetails,
                sourceView,
                serviceParameters,
                errorMessages
        );
        contractDateService.setContractTermEndDate(
                serviceContract,
                basicParameters.getContractStatus(),
                basicParameters.getContractTermEndDate(),
                termEndDate
        );
        contractDateService.updateStatusesBasedOnDates(serviceContract, serviceContractDetails);
//        contractDateService.validatePerpetuity(
//                serviceContract,
//                sourceView,
//                basicParameters.getContractStatus(),
//        );
        //check tommorow

    }

    private void validateContractStatus(ServiceContractEditRequest request, ServiceContracts serviceContracts, ServiceContractThirdPageFields sourceView, List<String> errorMessages) {
        ServiceContractBasicParametersEditRequest basicParameters = request.getBasicParameters();
        ServiceContractServiceParametersEditRequest serviceParameters = request.getServiceParameters();
        if (!serviceContracts.getContractStatus().equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY) && basicParameters.getContractStatus().equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY)) {
            Map<Long, ServiceContractTermShortResponse> collect = sourceView.getServiceContractTerms().stream().collect(Collectors.toMap(ServiceContractTermShortResponse::getId, j -> j));
            ServiceContractTermShortResponse serviceContractTerm = collect.get(serviceParameters.getContractTermId());
            if (serviceContractTerm == null) {
                return;
            }
            if (!Boolean.TRUE.equals(serviceContractTerm.getPerpetuityCause())) {
                errorMessages.add("basicParameters.contractStatus-status can not be changed to active in perpetuity because term is not perpetuity clause!;");
            }
        }
    }

    private void checkServiceContractDates(ServiceContracts serviceContract, ServiceContractDetails sourceDetails, ServiceContractBasicParametersEditRequest basicParameters, List<String> errorMessages) {
        if (basicParameters.getTerminationDate() == null && !basicParameters.getContractStatus().equals(ServiceContractDetailStatus.TERMINATED)) {
            Optional<ServiceContractTerm> serviceContractTerm = serviceContractsRepository.getServiceContractTermByContractIdAndDetailId(serviceContract.getId(), sourceDetails.getId());
            if (serviceContract.getPerpetuityDate() != null) {
                checkBasicParametersPerpetuityDate(serviceContract, basicParameters, errorMessages);
            } else {
                if (serviceContractTerm.isPresent()) {
                    checkBasicParametersPerpetuityDate(serviceContract, basicParameters, errorMessages);
                } else {
                    if (basicParameters.getContractStatus().equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY) && basicParameters.getPerpetuityDate() == null) {
                        errorMessages.add("basicParameters.perpetuityDate-Active in perpetuity date should not be empty;");
                    } else {
                        checkDateEntryIntoForceDate(serviceContract, basicParameters, errorMessages);
                    }
                }
            }
        }
    }

    private void checkBasicParametersPerpetuityDate(ServiceContracts serviceContract, ServiceContractBasicParametersEditRequest basicParameters, List<String> errorMessages) {
        LocalDate basicParametersPerpetuityDate = basicParameters.getPerpetuityDate();
        ServiceContractDetailStatus basicParametersContractStatus = basicParameters.getContractStatus();
        if (basicParametersPerpetuityDate != null && basicParametersPerpetuityDate.isAfter(LocalDate.now())) {
            errorMessages.add("basicParameters.perpetuityDate-Perpetuity date should be less or equal than current day;");
            return;
        }
        if (basicParametersPerpetuityDate != null && (basicParametersPerpetuityDate.isBefore(LocalDate.now())
                || basicParametersPerpetuityDate.isEqual(LocalDate.now()))) {
            if (!basicParametersContractStatus.equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY)) {
                errorMessages.add("basicParameters.contractStatus-Status should be Active in perpetuity;");
            }
            return;
        }

        if (basicParametersPerpetuityDate == null) {
            checkDateEntryIntoForceDate(serviceContract, basicParameters, errorMessages);
        }
    }

    private void checkDateEntryIntoForceDate(ServiceContracts serviceContract, ServiceContractBasicParametersEditRequest basicParameters, List<String> errorMessages) {
        LocalDate nowDate = LocalDate.now();
        ServiceContractDetailStatus statusToCheckFromBasicParameters = basicParameters.getContractStatus();
        ServiceContractDetailsSubStatus subStatusToCheckFromBasicParameters = basicParameters.getDetailsSubStatus();

        LocalDate entryIntoForceDateToCheckFromBasicParameters = basicParameters.getEntryIntoForceDate();
        if (entryIntoForceDateToCheckFromBasicParameters == null) {
            if (statusToCheckFromBasicParameters.equals(ServiceContractDetailStatus.ENTERED_INTO_FORCE)) {
                errorMessages.add("basicParameters.entryIntoForceDate-Entering into force date should not be empty;");
            } else {
                if (!statusToCheckFromBasicParameters.equals(ServiceContractDetailStatus.DRAFT)
                        && !statusToCheckFromBasicParameters.equals(ServiceContractDetailStatus.READY)
                        && !statusToCheckFromBasicParameters.equals(ServiceContractDetailStatus.CANCELLED)
                        && !statusToCheckFromBasicParameters.equals(ServiceContractDetailStatus.SIGNED)
                        && !statusToCheckFromBasicParameters.equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY)) {
                    errorMessages.add("basicParameters.contractStatus-Contract status is not correct;");
                }
            }
        } else {
            /*if (entryIntoForceDateToCheckFromBasicParameters.isAfter(nowDate)) {
                if (!(statusToCheckFromBasicParameters.equals(ServiceContractDetailStatus.SIGNED)
                      && (subStatusToCheckFromBasicParameters.equals(ServiceContractDetailsSubStatus.SIGNED_BY_BOTH_SIDES)
                          || subStatusToCheckFromBasicParameters.equals(ServiceContractDetailsSubStatus.SPECIAL_PROCESSES)))) {
                    errorMessages.add("basicParameters.entryIntoForceDate-Entered into forces date Should be empty;");
                }
                return;
            }*/
            if (entryIntoForceDateToCheckFromBasicParameters.isBefore(nowDate) || entryIntoForceDateToCheckFromBasicParameters.equals(nowDate)) {
                if (!List.of(ServiceContractDetailStatus.ACTIVE_IN_TERM, ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY, ServiceContractDetailStatus.ENTERED_INTO_FORCE)
                        .contains(statusToCheckFromBasicParameters)) {
                    errorMessages.add("basicParameters.contractStatus-Contract status is not correct;");
                }
                if (!ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY.equals(statusToCheckFromBasicParameters)) {
                    serviceContract.setContractStatus(ServiceContractDetailStatus.ACTIVE_IN_TERM);
                    serviceContract.setSubStatus(ServiceContractDetailsSubStatus.DELIVERY);
                    serviceContractsRepository.saveAndFlush(serviceContract);
                    basicParameters.setContractStatus(ServiceContractDetailStatus.ACTIVE_IN_TERM);
                    basicParameters.setDetailsSubStatus(ServiceContractDetailsSubStatus.DELIVERY);
                }
            }
        }
    }

    @Transactional
    public Long updateStatus(ServiceContractEditStatusRequest request, Long id, Long versionId) {
        log.debug("Editing Status service contract with id: {};", id);
        ServiceContracts serviceContract = serviceContractsRepository.findByIdAndStatusIn(id, List.of(EntityStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Contract can't be found;"));
        List<String> errorMessages = new ArrayList<>();
        updateOnlyContractStatus(serviceContract, request, errorMessages);
        ServiceContractDetails serviceContractDetails = serviceContractDetailsRepository.findByContractIdAndVersionId(id, versionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("versionId-version not found!;"));
        serviceContractDetails.setContractVersionStatus(request.getContractVersionStatus());
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        serviceContractsRepository.save(serviceContract);
        serviceContractDetailsRepository.save(serviceContractDetails);
        return serviceContract.getId();
    }

    private void updateServiceContractDates(ServiceContracts serviceContract, ServiceContractThirdPageFields sourceView,
                                            ServiceContractEditRequest request, List<String> errorMessages) {
        ServiceContractBasicParametersEditRequest basicParametersEditRequest = request.getBasicParameters();
        if (request.getBasicParameters().getContractStatus().equals(ServiceContractDetailStatus.TERMINATED)) {
            serviceContract.setTerminationDate(basicParametersEditRequest.getTerminationDate());
            if (request.getBasicParameters().getPerpetuityDate() == null) {
                serviceContract.setPerpetuityDate(null);
            } else {
                errorMessages.add("basicParameters-[perpetuityDate] should be null;");
            }
        }
        if (serviceContract.getContractTermEndDate() == null) {
            serviceContract.setContractTermEndDate(basicParametersEditRequest.getContractTermEndDate());
        }
        if (serviceContract.getContractTermEndDate() != null && basicParametersEditRequest.getContractTermEndDate() != null) {
            if (serviceContract.getContractTermEndDate() != basicParametersEditRequest.getContractTermEndDate()) {
                serviceContract.setContractTermEndDate(basicParametersEditRequest.getContractTermEndDate());
            }
        }
        if (request.getBasicParameters().getContractStatus().equals(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY)) {
            serviceContract.setPerpetuityDate(basicParametersEditRequest.getPerpetuityDate());
            if (request.getBasicParameters().getTerminationDate() == null) {
                serviceContract.setTerminationDate(null);
            } else {
                errorMessages.add("basicParameters-[terminationDate] should be null;");
            }

        }
        //fillServiceContractTermEndDate(request, serviceContract, sourceView, errorMessages);
    }

    private void fillServiceContractTermEndDate(ServiceContractEditRequest request, ServiceContracts serviceContract, ServiceContractThirdPageFields sourceView, List<String> errorMessages) {
        Map<Long, ServiceContractTermShortResponse> collect = sourceView.getServiceContractTerms().stream().collect(Collectors.toMap(ServiceContractTermShortResponse::getId, j -> j));
        ServiceContractTermShortResponse termShortResponse = collect.get(request.getServiceParameters().getContractTermId());
        if (termShortResponse.getPeriodType().equals(ServiceContractTermPeriodType.PERIOD) /*&& serviceContract.getContractTermEndDate() == null*/) {
            if (serviceContract.getContractInitialTermStartDate() != null) {
                LocalDate termEndDate = serviceContract.getContractInitialTermStartDate().plus(termShortResponse.getValue(), termShortResponse.getTermType().getUnit());
                LocalDate maximumDate = LocalDate.parse(maxDate);
                serviceContract.setContractTermEndDate(termEndDate.isBefore(maximumDate) ? termEndDate : maximumDate);
            }
        } else if (termShortResponse.getPeriodType().equals(ServiceContractTermPeriodType.CERTAIN_DATE)) {
            if (serviceContract.getContractTermEndDate() == null) {
                serviceContract.setContractTermEndDate(request.getServiceParameters().getContractTermEndDate());
            }
            serviceContract.setContractTermEndDate(request.getServiceParameters().getContractTermEndDate());
        }
    }

    private void updateContractStatus(ServiceContracts serviceContract, ServiceContractEditRequest request, List<String> errorMessages) {
        ServiceContractBasicParametersEditRequest basicParameters = request.getBasicParameters();
        ServiceContractDetailStatus oldStatus = serviceContract.getContractStatus();
        ServiceContractDetailStatus newStatus = basicParameters.getContractStatus();
        if (!ProductContractStatusChainUtil.canServiceContractChanged(oldStatus, newStatus)) {
            errorMessages.add("basicParameters.status-Status can not be changed to %s".formatted(basicParameters.getContractStatus()));
        }
        serviceContract.setContractStatus(basicParameters.getContractStatus());
        ServiceContractDetailsSubStatus newSubStatus = basicParameters.getDetailsSubStatus();
        if (!serviceContract.getContractStatus().isCorrectSubStatus(newSubStatus)) {
            errorMessages.add("basicParameters.subStatus-Sub status is incorrect for current status: %s".formatted(basicParameters.getContractStatus()));
        }
        serviceContract.setStatusModifyDate(basicParameters.getContractStatusModifyDate());
        serviceContract.setSubStatus(basicParameters.getDetailsSubStatus());
    }

    private void updateOnlyContractStatus(ServiceContracts serviceContract, ServiceContractEditStatusRequest request, List<String> errorMessages) {
        ServiceContractDetailStatus oldStatus = serviceContract.getContractStatus();
        ServiceContractDetailStatus newStatus = request.getContractStatus();
        ServiceContractDetailsSubStatus contractSubStatus = request.getContractSubStatus();
        if (!ProductContractStatusChainUtil.canServiceContractChanged(oldStatus, newStatus)) {
            errorMessages.add("basicParameters.status-Status can not be changed to %s".formatted(newStatus));
        }
        serviceContract.setContractStatus(newStatus);
        if (!newStatus.isCorrectSubStatus(contractSubStatus)) {
            errorMessages.add("basicParameters.contractSubStatus-Sub status is incorrect for current status: %s".formatted(newStatus));
        }
        if (!oldStatus.equals(newStatus) || !contractSubStatus.equals(serviceContract.getSubStatus())) {
            serviceContract.setStatusModifyDate(LocalDate.now());
        }
        serviceContract.setSubStatus(contractSubStatus);
        if (oldStatus.equals(ServiceContractDetailStatus.TERMINATED) && !oldStatus.equals(newStatus)) {
            serviceContract.setTerminationDate(null);
        }
        serviceContract.setTerminationDate(null);
    }

    private void checkIfUserHasPermission(ServiceContracts detailsSubStatus, boolean fromMassImport) {
        if (fromMassImport) {
            return;
        }
        List<String> permissions = permissionService.getPermissionsFromContext(SERVICE_CONTRACTS);
        if (!permissions.contains(SERVICE_CONTRACT_EDIT.getId())) {
            switch (detailsSubStatus.getContractStatus()) {
                case READY -> checkPermission(SERVICE_CONTRACT_EDIT_READY);
                case DRAFT -> checkPermission(SERVICE_CONTRACT_EDIT_DRAFT);
                //case SERVICE_CONTRACT_EDIT_LOCKED -> checkPermission(SERVICE_CONTRACT_EDIT_LOCKED);//TODO ADD WHEN LOCKED FUNCTIONALITY WILL BE ADDED
                default ->
                        throw new ClientException("Can't edit contract without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }
    }

    private boolean checkPermission(PermissionEnum permission) {
        if (permissionService.getPermissionsFromContext(SERVICE_CONTRACTS).contains(permission.getId())) {
            return true;
        } else throw new ClientException("Can't edit contract without permission;", ErrorCode.OPERATION_NOT_ALLOWED);
    }


    /**
     * Returns all activities for a service contract
     *
     * @param id service contract id
     * @return list of activities
     */
    public List<SystemActivityShortResponse> getActivitiesById(Long id) {
        return serviceContractActivityService.getActivitiesByConnectedObjectId(id);
    }


    /**
     * Returns a service contract by id
     *
     * @param id        service contract id
     * @param versionId service contract version id
     * @return service contract response populated with data
     */
    public ServiceContractResponse view(Long id, Long versionId) {
        log.debug("View service contract with id: {}", id);

        ServiceContracts serviceContracts = serviceContractsRepository
                .findByIdAndStatusIn(id, getStatuses()).
                orElseThrow(() -> new DomainEntityNotFoundException("id-Can't find Contract with id: %s".formatted(id)));

        ServiceContractDetails details;
        if (versionId != null) {
            details = serviceContractDetailsRepository
                    .findByContractIdAndVersionId(id, versionId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't Find Service ContractDetails with id: %s and version: %s;".formatted(id, versionId)));
        } else {
            details = serviceContractDetailsRepository
                    .findFirstByContractIdOrderByVersionIdDesc(id)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Service Contract details with contract id: %s;".formatted(id)));
        }

        ServiceContractResponse response = new ServiceContractResponse();

        response.setBasicParameters(serviceContractBasicParametersService.getBasicParameters(serviceContracts, details));
        response.setAdditionalParameters(serviceContractAdditionalParametersService.getAdditionalParameters(details));
        response.setThirdPageTabs(serviceParametersService.thirdPageFields(getServiceDetails(details.getServiceDetailId())));
        response.setServiceParameters(serviceParametersService.thirdPagePreview(serviceContracts, details, getServiceInfo(details), serviceContracts.getContractNumber()));
        response.setVersions(getContractVersions(id));
        response.setStatus(serviceContracts.getStatus());
        response.setLockedByInvoice(isLockedByInvoice(id));
        return response;
    }

    private boolean isLockedByInvoice(Long contractId) {
        return invoiceRepository.existsInvoiceByInvoiceStatusAndServiceContractId(InvoiceStatus.REAL, contractId);
    }

    private List<ServiceContractVersions> getContractVersions(Long id) {
        List<ServiceContractDetails> version = serviceContractDetailsRepository.findAllByContractIdOrderByStartDateDesc(id);
        List<ServiceContractVersions> returnList = new ArrayList<>();
        for (ServiceContractDetails item : version) {
            returnList.add(ServiceContractVersions.builder()
                    .id(item.getId())
                    .serviceId(item.getContractId())
                    .versionId(item.getVersionId())
                    .startDate(item.getStartDate())
                    .endDate(item.getEndDate())
                    .build());
        }
        return returnList;
    }

    private ServiceDetails getServiceInfo(ServiceContractDetails details) {
        return serviceDetailsRepository.findById(details.getServiceDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("Attached service do not exist;"));
    }


    /**
     * Deletes a service contract by id if all conditions are met.
     *
     * @param id service contract id
     * @return id of the deleted service contract
     */
    public Long delete(Long id) {
        log.debug("Delete service contract with id: {}", id);

        ServiceContracts serviceContracts = serviceContractsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Service contract not found with id: %s;".formatted(id)));

        if (!(serviceContracts.getContractStatus().equals(ServiceContractDetailStatus.READY) ||
                serviceContracts.getContractStatus().equals(ServiceContractDetailStatus.DRAFT))) {
            log.error("Service contract with id: {} can't be deleted,because status is different from READY or SIGNED.", id);
            throw new OperationNotAllowedException("Service contract with id: {%s} can't be deleted,because status is different from READY or DRAFT.".formatted(id));
        }

        if (serviceContracts.getStatus().equals(EntityStatus.DELETED)) {
            log.error("Service contract with id: {} is already deleted.", id);
            throw new OperationNotAllowedException("Service contract with id: %s is already deleted.".formatted(id));
        }

        if (isLockedByInvoice(id)) {
            throw new OperationNotAllowedException("Can't delete because it is connected to invoice");
        }

        if (serviceContractsRepository.hasConnectionToActivity(id)) {
            log.error("Service contract with id: {} has connection to activity.", id);
            throw new OperationNotAllowedException("You cannot delete service contract because it is connected to an activity.");
        }

        if (serviceContractsRepository.hasConnectionToProductContract(id)) {
            log.debug("Service contract with id: {} has connection to Product Contract", id);
            throw new OperationNotAllowedException("You cannot delete service contract because it is connected to Product Contract.");
        }

        if (serviceContractsRepository.hasConnectionToServiceContract(id) || serviceContractsRepository.isLinkedToServiceContract(id)) {
            log.debug("Service contract with id: {} has connection to Service Contract", id);
            throw new OperationNotAllowedException("You cannot delete service contract because it is connected to another Service Contract.");
        }

        if (serviceContractsRepository.hasConnectionToServiceOrder(id)) {
            log.debug("Service contract with id: {} has connection to Service Order", id);
            throw new OperationNotAllowedException("You cannot delete service contract because it is connected to Service Order.");
        }
        if (serviceContractsRepository.isLinkedToServiceOrder(id)) {
            log.debug("Service contract with id: {} is linked to Service Order", id);
            throw new OperationNotAllowedException("You cannot delete service contract because it is linked to Service Order.");

        }
        if (serviceContractsRepository.hasConnectionToGoodsOrder(id)) {
            log.debug("Service contract with id: {} has connection to Goods Order", id);
            throw new OperationNotAllowedException("You cannot delete service contract because it is connected to Goods Order.");
        }

        if (serviceContractsRepository.hasConnectionToTask(id)) {
            log.debug("Service contract with id: {} has connection to Task", id);
            throw new OperationNotAllowedException("You cannot delete service contract because it is connected to Task.");
        }

        if (serviceContractsRepository.hasConnectionToAction(id)) {
            log.debug("Service contract with id: {} has connection to Action", id);
            throw new OperationNotAllowedException("You cannot delete service contract because it is connected to Action.");
        }

        serviceContracts.setStatus(EntityStatus.DELETED);
        serviceContractsRepository.save(serviceContracts);
        return id;
    }

    public Page<ServiceContractListingResponse> list(ServiceContractListingRequest request) {
        return serviceContractsRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        extractSearchBy(request.getSearchBy()),
                        request.getDateOfTerminationFrom(),
                        request.getDateOfTerminationTo(),
                        request.getDateOfEntryIntoPerpetuityFrom(),
                        request.getDateOfEntryIntoPerpetuityTo(),
                        CollectionUtils.isEmpty(request.getContractDetailsStatuses()) ? null : request.getContractDetailsStatuses(),
                        CollectionUtils.isEmpty(request.getContractDetailsSubStatuses()) ? null : request.getContractDetailsSubStatuses(),
                        CollectionUtils.isEmpty(request.getTypes()) ? null : request.getTypes(),
                        CollectionUtils.isEmpty(request.getServiceIds()) ? null : request.getServiceIds(),
                        CollectionUtils.isEmpty(request.getAccountManagerIds()) ? null : request.getAccountManagerIds(),
                        getStatuses(),
                        ExcludeVersions.getExcludeVersionFromCheckBoxes(request.isExcludeOldVersions(), request.isExcludeFutureVersions()).getValue(),
                        PageRequest.of(request.getPage(), request.getSize(), extractSorting(request))
                );
    }

    private List<EntityStatus> getStatuses() {
        List<EntityStatus> statuses = new ArrayList<>();

        if (permissionService.permissionContextContainsPermissions(SERVICE_CONTRACTS, List.of(PermissionEnum.SERVICE_CONTRACT_VIEW))) {
            statuses.add(EntityStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.SERVICE_CONTRACTS, List.of(PermissionEnum.SERVICE_CONTRACT_VIEW_DELETED))) {
            statuses.add(EntityStatus.DELETED);
        }

        return statuses;
    }

    private String extractSearchBy(ServiceContractListingSearchFields searchFields) {
        return Objects.requireNonNullElse(searchFields, ServiceContractListingSearchFields.ALL).name();
    }

    private Sort extractSorting(ServiceContractListingRequest request) {
        return Sort.by(
                Objects.requireNonNullElse(request.getDirection(), Sort.Direction.ASC),
                Objects.requireNonNullElse(request.getSortBy(), ServiceContractListingSortFields.ID).getValue()
        );
    }

    public ServiceContractThirdPageFields thirdTabFields(Long serviceDetailId) {
        return serviceParametersService.thirdPageFields(getServiceDetails(serviceDetailId));
    }

    public Page<ServiceContractServiceListingResponse> serviceListingForContract(ServiceContractProductListingRequest request) {
        return serviceContractBasicParametersService
                .getServices(request)
                .map(ServiceContractServiceListingResponse::new);
    }

    public Page<ServiceContractServiceListingResponse> serviceListingForContractForExpressContract(ServiceContractProductListingRequest request) {
        return serviceContractBasicParametersService
                .getServicesForExpressContract(request)
                .map(ServiceContractServiceListingResponse::new);
    }

    public List<TaskShortResponse> getTasksById(Long id) {
        return taskService.getTasksByServiceContractId(id);
    }

    @ExecutionTimeLogger
    @Transactional
    public List<ServiceContracts> updateServiceContractsFromSchedulerJob() {
        LocalDate nowDate = LocalDate.now();
        List<ServiceContracts> productContracts = serviceContractsRepository.getServiceContractsForStatusUpdateFromJob(nowDate);
        List<ServiceContracts> contractsToUpdate = new ArrayList<>();
        productContracts.forEach(sc -> {
            try {
                LocalDate entryIntoForceDate = sc.getEntryIntoForceDate();
                if (entryIntoForceDate != null && (entryIntoForceDate.isBefore(nowDate) || entryIntoForceDate.equals(nowDate))) {
                    sc.setContractStatus(ServiceContractDetailStatus.ACTIVE_IN_TERM);
                    sc.setSubStatus(ServiceContractDetailsSubStatus.DELIVERY);
                    contractsToUpdate.add(sc);
                }
            } catch (Exception e) {
                log.error("Some error happened when working to update service contract status  with serviceContractId: %s, skipping current service contract".formatted(sc.getId()), e);

            }
        });
        serviceContractsRepository.saveAllAndFlush(contractsToUpdate);
        contractsToUpdate.forEach(c -> {
            List<Long> customerIdsToChangeStatusWithContractId = serviceContractDetailsRepository.getCustomerIdsToChangeStatusWithContractId(c.getId());
            customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(customerIdsToChangeStatusWithContractId);
            //        customerStatusChangeEventPublisher.publishCustomerStatusChangeEvent(List.of(1L, 2L, 3L, 174521094L));
        });
        return contractsToUpdate;
    }
}

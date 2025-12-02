package bg.energo.phoenix.service.product.service;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.*;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceType;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceUnit;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroup;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetails;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.service.*;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermGroupDetails;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import bg.energo.phoenix.model.entity.receivable.collectionChannel.CollectionChannel;
import bg.energo.phoenix.model.entity.template.ServiceTemplate;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.product.ProductDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceConsumptionPurpose;
import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.service.list.IndividualServiceOption;
import bg.energo.phoenix.model.enums.product.service.list.ServiceSearchField;
import bg.energo.phoenix.model.enums.product.service.list.ServiceTableColumn;
import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateType;
import bg.energo.phoenix.model.enums.template.ProductServiceTemplateType;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.request.product.product.TemplateSubObjectRequest;
import bg.energo.phoenix.model.request.product.service.*;
import bg.energo.phoenix.model.request.product.service.subObject.contractTerm.CreateServiceContractTermRequest;
import bg.energo.phoenix.model.response.contract.order.service.ServiceContractServiceVersionResponse;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderServiceVersionResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentGroupShortResponse;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.SegmentResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesAreaResponse;
import bg.energo.phoenix.model.response.nomenclature.product.SalesChannelResponse;
import bg.energo.phoenix.model.response.penalty.PenaltyShortResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentGroupShortResponse;
import bg.energo.phoenix.model.response.priceComponent.PriceComponentShortResponse;
import bg.energo.phoenix.model.response.product.ProductServiceTemplateShortResponse;
import bg.energo.phoenix.model.response.service.*;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponseProjection;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupShortResponse;
import bg.energo.phoenix.model.response.terminations.TerminationShortResponse;
import bg.energo.phoenix.model.response.terms.TermsShortResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupsShortResponse;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.nomenclature.product.*;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceGroupsRepository;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.service.ServiceUnitRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetailsRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.AdvancedPaymentGroupRepository;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.repository.product.penalty.penaltyGroup.PenaltyGroupDetailsRepository;
import bg.energo.phoenix.repository.product.penalty.penaltyGroup.PenaltyGroupRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupDetailsRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.service.*;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupDetailsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupsRepository;
import bg.energo.phoenix.repository.product.termination.terminationGroup.TerminationGroupDetailsRepository;
import bg.energo.phoenix.repository.product.termination.terminationGroup.TerminationGroupRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.repository.receivable.collectionChannel.CollectionChannelRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.repository.template.ServiceTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.copy.group.CopyDomainWithVersionBaseService;
import bg.energo.phoenix.service.product.iap.advancedPaymentGroup.AdvancedPaymentGroupService;
import bg.energo.phoenix.service.product.iap.interimAdvancePayment.InterimAdvancePaymentService;
import bg.energo.phoenix.service.product.penalty.penalty.PenaltyService;
import bg.energo.phoenix.service.product.penalty.penaltyGroups.PenaltyGroupService;
import bg.energo.phoenix.service.product.price.priceComponent.PriceComponentService;
import bg.energo.phoenix.service.product.price.priceComponentGroup.PriceComponentGroupService;
import bg.energo.phoenix.service.product.term.terms.TermsService;
import bg.energo.phoenix.service.product.termination.terminationGroup.TerminationGroupService;
import bg.energo.phoenix.service.product.termination.terminations.TerminationsService;
import bg.energo.phoenix.util.StringUtil;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;
import static bg.energo.phoenix.model.enums.product.product.CopyDomainWithVersionBasedRequestFilter.INDIVIDUAL_SERVICE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.SERVICES;
import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceService implements CopyDomainWithVersionBaseService {
    private final ServiceFileRepository serviceFileRepository;
    // main object
    private final ServiceRepository serviceRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ServiceDetailCollectionChannelsRepository serviceDetailCollectionChannelsRepository;
    private final CollectionChannelRepository collectionChannelRepository;
    private final ServiceMapper serviceMapper;

    // nomenclature repositories
    private final ServiceGroupsRepository serviceGroupsRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final VatRateRepository vatRateRepository;
    private final ServiceUnitRepository serviceUnitRepository;
    private final CurrencyRepository currencyRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final SalesChannelRepository salesChannelRepository;
    private final SalesAreaRepository salesAreaRepository;
    private final SegmentRepository segmentRepository;

    // sub object services
    private final ServiceContractTermService serviceContractTermService;
    private final TermsRepository termsRepository;
    private final TermsGroupsRepository termsGroupsRepository;
    private final TermsGroupDetailsRepository termsGroupDetailsRepository;
    private final PenaltyService penaltyService;
    private final PenaltyGroupService penaltyGroupService;
    private final InterimAdvancePaymentService interimAdvancePaymentService;
    private final AdvancedPaymentGroupService advancedPaymentGroupService;
    private final TerminationsService terminationsService;
    private final TerminationGroupService terminationGroupService;
    private final PriceComponentService priceComponentService;
    private final PriceComponentGroupService priceComponentGroupService;
    private final TermsService termsService;

    // sub object repositories
    private final InterimAdvancePaymentRepository interimAdvancePaymentRepository;
    private final AdvancedPaymentGroupRepository advancedPaymentGroupRepository;
    private final AdvancedPaymentGroupDetailsRepository advancedPaymentGroupDetailsRepository;
    private final PriceComponentRepository priceComponentRepository;
    private final PriceComponentGroupRepository priceComponentGroupRepository;
    private final PriceComponentGroupDetailsRepository pcgDetailsRepository;
    private final TerminationRepository terminationRepository;
    private final TerminationGroupRepository terminationGroupRepository;
    private final TerminationGroupDetailsRepository terminationGroupDetailsRepository;
    private final PenaltyRepository penaltyRepository;
    private final PenaltyGroupRepository penaltyGroupRepository;
    private final PenaltyGroupDetailsRepository penaltyGroupDetailsRepository;
    private final ProductDetailsRepository productDetailsRepository;

    private final ServiceSalesAreasRepository serviceSalesAreasRepository;
    private final ServiceSalesChannelsRepository serviceSalesChannelsRepository;
    private final ServiceSegmentsRepository serviceSegmentsRepository;
    private final ServiceGridOperatorsRepository serviceGridOperatorsRepository;
    private final ServiceRelatedEntitiesService serviceRelatedEntitiesService;
    private final ServiceAdditionalParamsRepository serviceAdditionalParamsRepository;

    private final ServiceFileService serviceFileService;
    private final PermissionService permissionService;
    private final ServiceRelatedContractUpdateService serviceRelatedContractUpdateService;

    private final ContractTemplateRepository templateRepository;
    private final ServiceTemplateRepository serviceTemplateRepository;
    private final AccountManagerRepository accountManagerRepository;

    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final FileArchivationService fileArchivationService;

    /**
     * Creates a new {@link EPService} with all its sub objects and attributes based on the provided request.
     *
     * @param request the request containing all the information needed to create a new service
     * @return the id of the newly created {@link EPService}
     */
    @Transactional
    public Long create(CreateServiceRequest request) {
        log.debug("Creating new service with request {}", request);

        validateCreatePermissions(request.getBasicSettings().getIsIndividual());

        List<String> exceptionMessages = new ArrayList<>();
        if (BooleanUtils.isNotTrue(request.getBasicSettings().getIsIndividual())) {
            // in case of individual service, the name field will be empty and populated after the service is created
            validateNameUniqueness(request.getBasicSettings().getName(), null, exceptionMessages);
        }
        validateNumbersOfIncomeAccountAndCostCenterControllingOrder(request, exceptionMessages);

        EPService service = createService(request.getBasicSettings().getCustomerIdentifier());
        ServiceDetails serviceDetails = createServiceDetails(request, service, exceptionMessages);
        service.setLastServiceDetailId(serviceDetails.getId());
        serviceRepository.save(service);

        serviceContractTermService.createServiceContractTerms(serviceDetails, request.getContractTerms(), exceptionMessages);
        interimAdvancePaymentService.addInterimAdvancePaymentsToService(request.getInterimAdvancePayments(), serviceDetails, exceptionMessages);
        advancedPaymentGroupService.addInterimAdvancePaymentGroupsToService(request.getInterimAdvancePaymentGroups(), serviceDetails, exceptionMessages);
        penaltyService.addPenaltiesToService(request.getPenalties(), serviceDetails, exceptionMessages);
        penaltyGroupService.addPenaltyGroupsToService(request.getPenaltyGroups(), serviceDetails, exceptionMessages);
        terminationsService.addTerminationsToService(request.getTerminations(), serviceDetails, exceptionMessages);
        terminationGroupService.addTerminationGroupsToService(request.getTerminationGroups(), serviceDetails, exceptionMessages);
        priceComponentService.addPriceComponentsToService(request.getPriceComponents(), serviceDetails, exceptionMessages);
        priceComponentGroupService.addPriceComponentGroupsToService(request.getPriceComponentGroups(), serviceDetails, exceptionMessages);
        serviceFileService.assignServiceFilesToServiceDetails(request.getServiceFiles(), serviceDetails, exceptionMessages, false);
        archiveFiles(serviceDetails);
        serviceRelatedEntitiesService.addRelatedProductsAndServicesToService(request.getRelatedEntities(), serviceDetails, exceptionMessages);

        saveTemplates(request.getTemplateIds(), serviceDetails.getId(), exceptionMessages);
        createSubEntities(serviceDetails, request, exceptionMessages);
        createServiceAdditionalParams(serviceDetails, request.getAdditionalSettings().getServiceAdditionalParams());
        createConnectedCollectionChannels(request.getAdditionalSettings().getCollectionChannelIds(), serviceDetails.getId(), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        return service.getId();
    }

    private void archiveFiles(ServiceDetails serviceDetails) {
        List<ServiceFile> serviceFiles = serviceFileRepository.findActiveServiceDetailFiles(serviceDetails.getId());
        if (CollectionUtils.isNotEmpty(serviceFiles)) {
            for (ServiceFile serviceFile : serviceFiles) {
                try {
                    serviceFile.setNeedArchive(true);
                    serviceFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_FILE);
                    serviceFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_SERVICE_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), serviceDetails.getName()),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), ""),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );

                    fileArchivationService.archive(serviceFile);
                } catch (Exception e) {
                    log.error("Cannot archive file", e);
                }
            }
        }
    }

    /**
     * Validates if the user has the required permissions to create the service based on the service type.
     *
     * @param isIndividualService the service type, individual or not
     */
    private void validateCreatePermissions(Boolean isIndividualService) {
        if (BooleanUtils.isTrue(isIndividualService)) {
            if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_CREATE_INDIVIDUAL))) {
                log.error("You do not have permission to create an individual service.");
                throw new ClientException("You do not have permission to create an individual service.", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_CREATE))) {
                log.error("You do not have permission to create a non-individual service.");
                throw new ClientException("You do not have permission to create a non-individual service.", ErrorCode.ACCESS_DENIED);
            }
        }
    }


    /**
     * Validates the provided name for uniqueness.
     * Name can be same among versions of the same service but not among different [active] services.
     *
     * @param name              the name to be validated
     * @param id                the id of the service to be updated (if any)
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     */
    private void validateNameUniqueness(String name, Long id, List<String> exceptionMessages) {
        if (serviceDetailsRepository.existsByName(name, id)) {
            log.error("Service with name {} already exists", name);
            exceptionMessages.add("basicSettings.name-Service with name %s already exists;".formatted(name));
        }
    }


    /**
     * Validates the numbers of income account and cost center controlling order for the provided price components.
     * If there is a price component in the Service without filed Number of income account, the number of income account and cost controlling order is mandatory.
     * If there are no price components in the Service, both numbers are optional.
     *
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     */
    private void validateNumbersOfIncomeAccountAndCostCenterControllingOrder(BaseServiceRequest request, List<String> exceptionMessages) {
        // if there are no price components attached, then not mandatory
        if (CollectionUtils.isEmpty(request.getPriceComponents())) {
            return;
        }

        List<PriceComponent> priceComponentContext = fetchPriceComponents(request.getPriceComponents(), exceptionMessages);
        priceComponentRepository.findPriceComponentByPriceComponentGroupIds(CollectionUtils.emptyIfNull(request.getPriceComponentGroups()));
        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return;
        }

        validateNumberOfIncomeAccount(request.getBasicSettings().getIncomeAccountNumber(), priceComponentContext, exceptionMessages);
        validateNumberOfCostCenterControllingOrder(request.getBasicSettings().getCostCenterControllingOrder(), priceComponentContext, exceptionMessages);
    }


    /**
     * Validates the provided income account number for the provided price components.
     *
     * @param incomeAccountNumber the income account number to be validated
     * @param priceComponents     the list of price components to be validated
     * @param exceptionMessages   the list of exception messages to be populated in case of errors
     */
    private void validateNumberOfIncomeAccount(String incomeAccountNumber, List<PriceComponent> priceComponents, List<String> exceptionMessages) {
        if (StringUtils.isNotEmpty(incomeAccountNumber)) {
            return;
        }

        List<String> erroredPriceComponents = new ArrayList<>();
        for (int i = 0; i < priceComponents.size(); i++) {
            PriceComponent priceComponent = priceComponents.get(i);
            if (StringUtils.isEmpty(priceComponent.getIncomeAccountNumber())) {
                log.error("priceComponents[%s]-Number of income account is empty;".formatted(i));
                erroredPriceComponents.add("%s (%s)".formatted(priceComponent.getName(), priceComponent.getId()));
            }
        }

        // return one global error
        if (CollectionUtils.isNotEmpty(erroredPriceComponents)) {
            exceptionMessages.add("Income account number is mandatory in service as there are price components without income account number: %s;".formatted(String.join(", ", erroredPriceComponents)));
        }
    }


    /**
     * Validates the provided cost center controlling order for the provided price components.
     *
     * @param costCenterControllingOrder the cost center controlling order to be validated
     * @param priceComponents            the list of price components to be validated
     * @param exceptionMessages          the list of exception messages to be populated in case of errors
     */
    private void validateNumberOfCostCenterControllingOrder(String costCenterControllingOrder, List<PriceComponent> priceComponents, List<String> exceptionMessages) {
        if (StringUtils.isNotEmpty(costCenterControllingOrder)) {
            return;
        }

        List<String> erroredPriceComponents = new ArrayList<>();
        for (int i = 0; i < priceComponents.size(); i++) {
            PriceComponent priceComponent = priceComponents.get(i);
            if (StringUtils.isEmpty(priceComponent.getCostCenterControllingOrder())) {
                log.error("priceComponents[%s]-Number of cost center controlling order is empty;".formatted(i));
                erroredPriceComponents.add("%s (%s)".formatted(priceComponent.getName(), priceComponent.getId()));
            }
        }

        // return one global error
        if (CollectionUtils.isNotEmpty(erroredPriceComponents)) {
            exceptionMessages.add("Number of Cost center / Controlling order is mandatory in service as there are price components without number of Cost center / Controlling order: %s;".formatted(String.join(", ", erroredPriceComponents)));
        }
    }


    /**
     * Fetches the price components by the provided ids.
     *
     * @param priceComponents   the ids of the price components to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the list of fetched price components
     */
    private List<PriceComponent> fetchPriceComponents(List<Long> priceComponents, List<String> exceptionMessages) {
        List<PriceComponent> existingPriceComponents = priceComponentRepository
                .findByIdInAndStatusIn(priceComponents, List.of(PriceComponentStatus.ACTIVE));
        List<Long> existingPriceComponentIds = existingPriceComponents.stream().map(PriceComponent::getId).toList();

        for (int i = 0; i < priceComponents.size(); i++) {
            Long id = priceComponents.get(i);
            if (!existingPriceComponentIds.contains(id)) {
                log.error("priceSettings.priceComponents[%s]-Price component with ID %s not found;".formatted(i, id));
                exceptionMessages.add("priceSettings.priceComponents[%s]-Price component with ID %s not found;".formatted(i, id));
            }
        }

        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return Collections.emptyList();
        }

        return existingPriceComponents;
    }


    /**
     * Creates a new {@link EPService} entity with active status.
     *
     * @return created {@link EPService}
     */
    private EPService createService(String customerIdentifier) {
        EPService service = new EPService();
        service.setStatus(ServiceStatus.ACTIVE);
        service.setCustomerIdentifier(customerIdentifier);
        return serviceRepository.saveAndFlush(service);
    }

    /**
     * Creates service details with the provided data.
     *
     * @param request           the {@link CreateServiceRequest} containing the data to be used for creating the service details
     * @param EPService         the {@link EPService} to be used for creating the service details
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     */
    private ServiceDetails createServiceDetails(CreateServiceRequest request, EPService EPService, List<String> exceptionMessages) {
        ServiceDetails serviceDetails = serviceMapper.fromRequestToServiceDetailsEntity(
                request,
                EPService,
                fetchServiceGroup(request.getBasicSettings().getServiceGroupId(), List.of(ACTIVE), exceptionMessages),
                fetchServiceType(request.getBasicSettings().getServiceTypeId(), List.of(ACTIVE), exceptionMessages),
                fetchVatRate(request.getBasicSettings().getVatRateId(), List.of(ACTIVE), exceptionMessages),
                fetchServiceUnit(request.getBasicSettings().getServiceUnitId(), List.of(ACTIVE), exceptionMessages),
                fetchCurrency(request.getBasicSettings().getCashDepositCurrencyId(), List.of(ACTIVE), exceptionMessages), // cash deposit currency
                fetchCurrency(request.getBasicSettings().getBankGuaranteeCurrencyId(), List.of(ACTIVE), exceptionMessages), // bank guarantee currency
                fetchCurrency(request.getPriceSettings().getCurrencyId(), List.of(ACTIVE), exceptionMessages), // price settings currency
                fetchTerm(request.getTerm(), List.of(TermStatus.ACTIVE), exceptionMessages),
                fetchTermsGroup(request.getTermGroup(), List.of(TermGroupStatus.ACTIVE), exceptionMessages)
        );
        serviceDetails.setVersion(1L);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        return serviceDetailsRepository.saveAndFlush(serviceDetails);
    }


    /**
     * Fetches the {@link VatRate} with the provided id.
     * Should not be present if global vat rate specified, otherwise mandatory (validated in controller).
     *
     * @param vatRateId         the id of the {@link VatRate} to be fetched
     * @param statuses          the list of statuses of the {@link VatRate} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the {@link VatRate} fetched
     */
    private VatRate fetchVatRate(Long vatRateId, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        VatRate vatRate = null;
        if (vatRateId != null) {
            Optional<VatRate> vatRateOptional = vatRateRepository.findByIdAndStatus(vatRateId, statuses);
            if (vatRateOptional.isEmpty()) {
                log.error("Vat rate with id {} not found in statuses {}.", vatRateId, statuses);
                exceptionMessages.add("basicSettings.vatRateId-Vat rate with id %s not found in statuses %s;".formatted(vatRateId, statuses));
            } else {
                vatRate = vatRateOptional.get();
            }
        }
        return vatRate;
    }


    /**
     * Fetches the {@link ServiceType} with the provided id.
     * Mandatory nomenclature item.
     *
     * @param serviceTypeId     the id of the {@link ServiceType} to be fetched
     * @param statuses          the list of statuses of the {@link ServiceType} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the {@link ServiceType} fetched
     */
    private ServiceType fetchServiceType(Long serviceTypeId, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        ServiceType serviceType = null;
        Optional<ServiceType> serviceTypeOptional = serviceTypeRepository.findByIdAndStatuses(serviceTypeId, statuses);
        if (serviceTypeOptional.isEmpty()) {
            log.error("Service type with id {} not found in statuses {}.", serviceTypeId, statuses);
            exceptionMessages.add("basicSettings.serviceTypeId-Service type with id %s not found in statuses %s;".formatted(serviceTypeId, statuses));
        } else {
            serviceType = serviceTypeOptional.get();
        }
        return serviceType;
    }


    /**
     * Fetches the {@link ServiceGroups} with the provided id.
     * Optional nomenclature item.
     *
     * @param serviceGroupId    the id of the {@link ServiceGroups} to be fetched
     * @param statuses          the list of statuses of the {@link ServiceGroups} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the {@link ServiceGroups} fetched
     */
    private ServiceGroups fetchServiceGroup(Long serviceGroupId, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        ServiceGroups serviceGroup = null;
        if (serviceGroupId != null) {
            Optional<ServiceGroups> serviceGroupsOptional = serviceGroupsRepository.findByIdAndStatus(serviceGroupId, statuses);
            if (serviceGroupsOptional.isEmpty()) {
                log.error("Service group with id {} not found in statuses {}.", serviceGroupId, statuses);
                exceptionMessages.add("basicSettings.serviceGroupId-Service group with id %s not found in statuses %s;".formatted(serviceGroupId, statuses));
            } else {
                serviceGroup = serviceGroupsOptional.get();
            }
        }
        return serviceGroup;
    }


    /**
     * Fetches the {@link ServiceUnit} with the provided id.
     * Optional nomenclature item.
     *
     * @param serviceUnitId     the id of the {@link ServiceUnit} to be fetched
     * @param statuses          the list of statuses of the {@link ServiceUnit} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the {@link ServiceUnit} fetched
     */
    private ServiceUnit fetchServiceUnit(Long serviceUnitId, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        ServiceUnit serviceUnit = null;
        if (serviceUnitId != null) {
            Optional<ServiceUnit> serviceUnitOptional = serviceUnitRepository.findByIdAndStatusIn(serviceUnitId, statuses);
            if (serviceUnitOptional.isEmpty()) {
                log.error("Service unit with id {} not found in statuses {};", serviceUnitId, statuses);
                exceptionMessages.add("basicSettings.serviceUnitId-Service unit with id %s not found in statuses %s;".formatted(serviceUnitId, statuses));
            } else {
                serviceUnit = serviceUnitOptional.get();
            }
        }
        return serviceUnit;
    }


    /**
     * Fetches the {@link Currency} with the provided id.
     * Should not be present if [Equal Monthly Installments] false, mandatory otherwise (validated in controller).
     *
     * @param currencyId        the id of the {@link Currency} to be fetched
     * @param statuses          the list of statuses of the {@link Currency} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the {@link Currency} fetched
     */
    private Currency fetchCurrency(Long currencyId, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        Currency currency = null;
        if (currencyId != null) {
            Optional<Currency> currencyOptional = currencyRepository.findByIdAndStatus(currencyId, statuses);
            if (currencyOptional.isEmpty()) {
                log.error("Currency with id {} not found in statuses {};", currencyId, statuses);
                exceptionMessages.add("priceSettings.currencyId-Currency with id %s not found in statuses %s;".formatted(currencyId, statuses));
            } else {
                currency = currencyOptional.get();
            }
        }
        return currency;
    }


    /**
     * Fetches the {@link Terms} with the provided id.
     * Should not be present if terms group is present, mandatory otherwise (validated in controller).
     *
     * @param termId            the id of the {@link Terms} to be fetched
     * @param statuses          the list of statuses of the {@link Terms} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the {@link Terms} fetched
     */
    private Terms fetchTerm(Long termId, List<TermStatus> statuses, List<String> exceptionMessages) {
        Terms terms = null;
        if (termId != null) {
            Optional<Terms> termsOptional = termsRepository.findByIdForService(termId);
            if (termsOptional.isEmpty()) {
                log.error("Term with presented id {} and statuses {} not found", termId, statuses);
                exceptionMessages.add("basicSettings.term[0]-Term with presented id %s not found in statuses %s;".formatted(termId, statuses));
            } else {
                terms = termsOptional.get();
            }
        }
        return terms;
    }


    /**
     * Fetches the {@link TermsGroups} with the provided id.
     * Should not be present if terms is present, mandatory otherwise (validated in controller).
     *
     * @param termGroupId       the id of the {@link TermsGroups} to be fetched
     * @param statuses          the list of statuses of the {@link TermsGroups} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the {@link TermsGroups} fetched
     */
    private TermsGroups fetchTermsGroup(Long termGroupId, List<TermGroupStatus> statuses, List<String> exceptionMessages) {
        TermsGroups termsGroups = null;
        if (termGroupId != null) {
            Optional<TermsGroups> termsGroupsOptional = termsGroupsRepository.findByIdForService(termGroupId);
            if (termsGroupsOptional.isEmpty()) {
                log.error("Term Group with presented id {} and statuses {} not found", termGroupId, statuses);
                exceptionMessages.add("basicSettings.termGroup[0]-Term Group with presented id %s not found in statuses %s;".formatted(termGroupId, statuses));
            } else {
                termsGroups = termsGroupsOptional.get();
            }
        }
        return termsGroups;
    }


    /**
     * Fetches sub objects of the service to be updated.
     * Nomenclature items are fetched by ACTIVE status only when creating a new service.
     *
     * @param serviceDetails    the {@link ServiceDetails} to be updated
     * @param request           the request containing all the information needed to update the service
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     */
    private void createSubEntities(ServiceDetails serviceDetails, CreateServiceRequest request, List<String> exceptionMessages) {
        ServiceBasicSettingsRequest basicSettings = request.getBasicSettings();

        // sales channels, areas and segments are disabled for individual services

        List<SalesChannel> salesChannels = new ArrayList<>();
        if (BooleanUtils.isNotTrue(basicSettings.getIsIndividual()) && !basicSettings.getGlobalSalesChannel()) {
            log.debug("Searching requested sales channels with ids: {}", basicSettings.getSalesChannels());
            salesChannels = fetchSalesChannels(basicSettings.getSalesChannels(), List.of(ACTIVE), exceptionMessages);
        }

        List<SalesArea> salesAreas = new ArrayList<>();
        if (BooleanUtils.isNotTrue(basicSettings.getIsIndividual()) && !basicSettings.getGlobalSalesAreas()) {
            log.debug("Searching requested sales areas with ids: {};", basicSettings.getSalesAreas());
            salesAreas = fetchSalesAreas(basicSettings.getSalesAreas(), List.of(ACTIVE), exceptionMessages);
        }

        List<Segment> segments = new ArrayList<>();
        if (BooleanUtils.isNotTrue(basicSettings.getIsIndividual()) && !basicSettings.getGlobalSegment()) {
            log.debug("Searching requested segments with ids: {};", basicSettings.getSegments());
            segments = fetchSegments(basicSettings.getSegments(), List.of(ACTIVE), exceptionMessages);
        }

        List<GridOperator> gridOperators = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(basicSettings.getGridOperators()) && BooleanUtils.isNotTrue(request.getBasicSettings().getGlobalGridOperator())) {
            log.debug("Searching requested grid operators with ids: {};", basicSettings.getGridOperators());
            gridOperators = fetchGridOperators(basicSettings.getGridOperators(), List.of(ACTIVE), exceptionMessages);
        }

        createSubObjectsAndAssignToDetail(serviceDetails, salesAreas, salesChannels, segments, gridOperators);
    }

    /**
     * Fetches the {@link SalesChannel} with the provided ids.
     *
     * @param salesChannelIds   the list of ids of the {@link SalesChannel} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the list of {@link SalesChannel} fetched
     */
    private List<SalesChannel> fetchSalesChannels(Set<Long> salesChannelIds, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        List<SalesChannel> existingSalesChannels = salesChannelRepository.findByIdInAndStatusIn(salesChannelIds.stream().toList(), statuses);
        List<Long> existingSalesChannelIds = existingSalesChannels.stream().map(SalesChannel::getId).toList();

        List<Long> notMatchingSalesChannelIds = salesChannelIds.stream().filter(id -> !existingSalesChannelIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(notMatchingSalesChannelIds)) {
            notMatchingSalesChannelIds.forEach(id -> {
                log.error("Sales channel with ID {} not found in statuses {};", id, statuses);
                exceptionMessages.add("basicSettings.salesChannels-Sales channel with ID %s not found in statuses [%s];".formatted(id, statuses));
            });
        }

        return existingSalesChannels;
    }


    /**
     * Fetches the {@link SalesArea} with the provided ids.
     *
     * @param salesAreasIds     the list of ids of the {@link SalesArea} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the list of {@link SalesArea} fetched
     */
    private List<SalesArea> fetchSalesAreas(Set<Long> salesAreasIds, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        List<SalesArea> existingSalesAreas = salesAreaRepository.findByIdInAndStatusIn(salesAreasIds.stream().toList(), statuses);
        List<Long> existingSalesAreaIds = existingSalesAreas.stream().map(SalesArea::getId).toList();

        List<Long> notMatchingSalesAreaIds = salesAreasIds.stream().filter(id -> !existingSalesAreaIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(notMatchingSalesAreaIds)) {
            notMatchingSalesAreaIds.forEach(id -> {
                log.error("Sales Area with presented id not found {} in statuses {};", id, statuses);
                exceptionMessages.add(String.format("basicSettings.salesAreas-Sales Area with presented id [%s] not found in statuses [%s];", id, statuses));
            });
        }

        return existingSalesAreas;
    }


    /**
     * Fetches the {@link Segment} with the provided ids.
     *
     * @param segmentsIds       the list of ids of the {@link Segment} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the list of {@link Segment} fetched
     */
    private List<Segment> fetchSegments(Set<Long> segmentsIds, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        List<Segment> existingSegments = segmentRepository.findByIdInAndStatusIn(segmentsIds.stream().toList(), statuses);
        List<Long> existingSegmentIds = existingSegments.stream().map(Segment::getId).toList();

        List<Long> notMatchingSegmentIds = segmentsIds.stream().filter(id -> !existingSegmentIds.contains(id)).toList();

        if (CollectionUtils.isNotEmpty(notMatchingSegmentIds)) {
            notMatchingSegmentIds.forEach(id -> {
                log.error("Segment with presented id {} not found in statuses {};", id, statuses);
                exceptionMessages.add(String.format("basicSettings.segments-Segment with presented id [%s] not found in statuses [%s];", id, statuses));
            });
        }

        return existingSegments;
    }


    /**
     * Fetches the {@link GridOperator} with the provided ids.
     *
     * @param gridOperatorIds   the list of ids of the {@link GridOperator} to be fetched
     * @param exceptionMessages the list of exception messages to be populated in case of errors
     * @return the list of {@link GridOperator} fetched
     */
    private List<GridOperator> fetchGridOperators(Set<Long> gridOperatorIds, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        List<GridOperator> existingGridOperators = gridOperatorRepository.findByIdInAndStatusIn(gridOperatorIds.stream().toList(), statuses);
        List<Long> existingGridOperatorIds = existingGridOperators.stream().map(GridOperator::getId).toList();

        List<Long> notMatchingGridOperatorIds = gridOperatorIds.stream().filter(id -> !existingGridOperatorIds.contains(id)).toList();
        if (CollectionUtils.isNotEmpty(notMatchingGridOperatorIds)) {
            notMatchingGridOperatorIds.forEach(id -> {
                log.error("Grid operator with presented id {} not found in statuses {};", id, statuses);
                exceptionMessages.add(String.format("basicSettings.gridOperators-Grid Operator with presented id [%s] not found;", id));
            });
        }
        return existingGridOperators;
    }


    /**
     * Creates sub objects of a service and assigns them to the provided {@link ServiceDetails}.
     *
     * @param serviceDetails the {@link EPService} to be updated
     * @param salesAreas     the list of {@link SalesArea} to be assigned to the service
     * @param salesChannels  the list of {@link SalesChannel} to be assigned to the service
     * @param segments       the list of {@link Segment} to be assigned to the service
     * @param gridOperators  the list of {@link GridOperator} to be assigned to the service
     */
    private void createSubObjectsAndAssignToDetail(ServiceDetails serviceDetails,
                                                   List<SalesArea> salesAreas,
                                                   List<SalesChannel> salesChannels,
                                                   List<Segment> segments,
                                                   List<GridOperator> gridOperators) {
        serviceDetails.setSalesAreas(
                salesAreas
                        .stream()
                        .map(sa -> new ServiceSalesArea(null, serviceDetails, sa, ServiceSubobjectStatus.ACTIVE))
                        .collect(Collectors.toList())
        );

        serviceDetails.setSalesChannels(
                salesChannels
                        .stream()
                        .map(sch -> new ServiceSalesChannel(null, serviceDetails, sch, ServiceSubobjectStatus.ACTIVE))
                        .collect(Collectors.toList())
        );

        serviceDetails.setSegments(
                segments
                        .stream()
                        .map(s -> new ServiceSegment(null, serviceDetails, s, ServiceSubobjectStatus.ACTIVE))
                        .collect(Collectors.toList())
        );

        serviceDetails.setGridOperator(
                gridOperators
                        .stream()
                        .map(go -> new ServiceGridOperator(null, serviceDetails, go, ServiceSubobjectStatus.ACTIVE))
                        .collect(Collectors.toList())
        );

        serviceDetailsRepository.saveAndFlush(serviceDetails);
    }


    /**
     * Deletes the {@link EPService} with the provided id if all the validations are passed.
     *
     * @param id the id of the {@link EPService} to be deleted
     * @return the id of the deleted {@link EPService}
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting service with id {}", id);

        Optional<EPService> serviceOptional = serviceRepository.findById(id);
        if (serviceOptional.isEmpty()) {
            log.error("Service with id {} not found", id);
            throw new DomainEntityNotFoundException("id-Service with id [%s] not found;".formatted(id));
        }

        EPService service = serviceOptional.get();

        validateDeletePermissions(service);

        if (serviceDetailsRepository.hasServiceActiveConnectionToContract(service.getId())) {
            log.error("You can’t delete the service with ID: [%s] because it is connected to the contract".formatted(id));
            if (StringUtils.isNotEmpty(service.getCustomerIdentifier())) {
                throw new OperationNotAllowedException("You can’t delete an individual service because it is connected to the service contract.");
            } else {
                throw new OperationNotAllowedException("You can’t delete a service because it is connected to the service contract.");
            }
        }

        if (service.getStatus().equals(ServiceStatus.DELETED)) {
            log.error("Service with id {} is already deleted", id);
            throw new OperationNotAllowedException("id-Service with id [%s] is already deleted;".formatted(id));
        }

        if (serviceRepository.hasConnectionToService(id)) {
            log.error("You can’t delete the service with ID: [%s] because it is connected to the service".formatted(id));
            throw new OperationNotAllowedException("You can’t delete the service with ID: [%s] because it is connected to the service".formatted(id));
        }

        if (serviceRepository.hasConnectionToProduct(id)) {
            log.error("You can’t delete the service with ID: [%s] because it is connected to the product".formatted(id));
            throw new OperationNotAllowedException("You can’t delete the service with ID: [%s] because it is connected to the product".formatted(id));
        }

        if (serviceRepository.hasConnectionToServiceContract(id)) {
            log.error("You can’t delete the service with ID: [%s] because it is connected to the Service Contract".formatted(id));
            throw new OperationNotAllowedException("You can’t delete the service with ID: [%s] because it is connected to the Service Contract".formatted(id));
        }

        if (serviceRepository.hasConnectionToServiceOrder(id)) {
            log.error("You can’t delete the service with ID: [%s] because it is connected to the Service Order".formatted(id));
            throw new OperationNotAllowedException("You can’t delete the service with ID: [%s] because it is connected to the Service Order".formatted(id));
        }

        service.setStatus(ServiceStatus.DELETED);
        serviceRepository.save(service);
        return id;
    }


    /**
     * Validates the permissions for deleting the provided {@link EPService}.
     *
     * @param service the {@link EPService} to be validated
     */
    private void validateDeletePermissions(EPService service) {
        if (StringUtils.isNotEmpty(service.getCustomerIdentifier())) {
            // it means that the service is of individual type
            if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_DELETE_INDIVIDUAL))) {
                log.error("You don’t have permission to delete the individual service with ID: [%s]".formatted(service.getId()));
                throw new AccessDeniedException("You don’t have permission to delete the individual service with ID: [%s].".formatted(service.getId()));
            }
        } else {
            if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_DELETE))) {
                log.error("You don’t have permission to delete the service with ID: [%s]".formatted(service.getId()));
                throw new AccessDeniedException("You don’t have permission to delete the service with ID: [%s].".formatted(service.getId()));
            }
        }
    }


    /**
     * Fetches the {@link EPService} with the provided id.
     *
     * @param id        the id of the {@link EPService} to be fetched
     * @param versionId the version id of the {@link EPService} to be fetched
     * @return the {@link EPService} fetched
     */
    public ServiceResponse get(Long id, Long versionId) {
        log.debug("Fetching service with id {}", id);

        EPService service = serviceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service not found by id %s;".formatted(id)));

        validatePreviewPermissions(service);

        ServiceDetails details;
        if (versionId != null) {
            details = serviceDetailsRepository
                    .findByServiceIdAndVersionAndStatusIn(id, versionId, List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("versionId-Service details not found for given version id %s;".formatted(versionId)));
        } else {
            details = serviceDetailsRepository
                    .findFirstByServiceIdAndStatusInOrderByVersionDesc(id, List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Service detail not found by service id %s;".formatted(id)));
        }

        ServiceBasicSettingsResponse basicSettingsResponse = serviceMapper.fromDetailsEntityToBasicSettingsResponse(details, List.of(ACTIVE, INACTIVE), service);
        ServiceAdditionalSettingsResponse serviceAdditionalSettingsResponse = serviceMapper.fromDetailsEntityToAdditionalSettingsResponse(details);
        serviceAdditionalSettingsResponse.setCollectionChannels(serviceDetailCollectionChannelsRepository.getByDetailId(details.getId()));

        ServicePriceSettingsResponse servicePriceSettingsResponse = serviceMapper.fromDetailsEntityToPriceSettingsResponse(details, List.of(ACTIVE, INACTIVE));

        List<ServiceVersion> serviceVersions = serviceDetailsRepository.findAllServiceDetailsByServiceIdAndStatusIn(id, List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE));

        ServiceResponse serviceResponse = serviceMapper.fromEntityToResponse(
                service,
                details,
                serviceVersions,
                basicSettingsResponse,
                servicePriceSettingsResponse,
                serviceAdditionalSettingsResponse
        );

        ServiceBasicSettingsResponse basicSettings = serviceResponse.getBasicSettings();
        Boolean globalSegment = details.getGlobalSegment();
        if (globalSegment == null || !globalSegment) {
            basicSettings.setSegments(details
                    .getSegments()
                    .stream()
                    .filter(serviceSegment -> serviceSegment.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                    .map(ServiceSegment::getSegment)
                    .map(SegmentResponse::new).toList());
        }

        Boolean globalSalesArea = details.getGlobalSalesArea();
        if (globalSalesArea == null || !globalSalesArea) {
            basicSettings.setSalesAreas(details
                    .getSalesAreas()
                    .stream()
                    .filter(serviceSegment -> serviceSegment.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                    .map(ServiceSalesArea::getSalesArea)
                    .map(SalesAreaResponse::new).toList());

        }

        Boolean globalSalesChannel = details.getGlobalSalesChannel();
        if (globalSalesChannel == null || !globalSalesChannel) {
            basicSettings.setSalesChannels(details
                    .getSalesChannels()
                    .stream()
                    .filter(serviceSegment -> serviceSegment.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                    .map(ServiceSalesChannel::getSalesChannel)
                    .map(SalesChannelResponse::new)
                    .toList());

        }

        Boolean globalGridOperator = details.getGlobalGridOperator();
        if (BooleanUtils.isNotTrue(globalGridOperator)) {
            basicSettings.setGridOperators(serviceMapper.createGridOperatorResponse(details, List.of(ACTIVE, INACTIVE)));
        }
        serviceResponse.setBasicSettings(basicSettings);

        serviceResponse.setConnectedToContract(serviceDetailsRepository.hasActiveConnectionToContract(details.getId()));
        serviceResponse.setContractTerms(serviceMapper.createServiceContractTermsResponse(details));
        serviceResponse.setTerm(serviceMapper.createTermsShortResponse(details));
        serviceResponse.setTermGroup(createTermsGroupShortResponse(details.getTermsGroups()));
        serviceResponse.setPenalties(serviceMapper.createPenaltyResponse(details));
        serviceResponse.setPenaltyGroups(createPenaltyGroupResponse(details));
        serviceResponse.setTerminations(serviceMapper.createTerminationResponse(details));
        serviceResponse.setTerminationGroups(createTerminationGroupResponse(details));
        serviceResponse.setPriceComponents(serviceMapper.createPriceComponentResponse(details));
        serviceResponse.setPriceComponentGroups(createPriceComponentGroupResponse(details));
        serviceResponse.setInterimAdvancePayments(serviceMapper.createIAPShortResponse(details));
        serviceResponse.setInterimAdvancePaymentGroups(createIAPGroupShortResponse(details.getInterimAndAdvancePaymentGroups()));
        serviceResponse.setRelatedEntities(createRelatedEntitiesShortResponse(details.getLinkedProducts(), details.getLinkedServices()));
        serviceResponse.setServiceFiles(createServiceFileResponse(details.getServiceFiles()));
        serviceResponse.setLocked(checkForBoundObjectsForPreview(details));
        serviceResponse.setTemplateResponses(serviceTemplateRepository.findForContract(details.getId(), LocalDate.now()));
        return serviceResponse;
    }

    /**
     * Retrieves a preview of a service's description based on its ID and version.
     * If the version is not specified, the latest available version is used.
     *
     * @param id        the ID of the service to retrieve the description for
     * @param versionId the specific version of the service details to preview (optional)
     * @return {@link ServiceDescriptionResponse} containing the short and full descriptions of the service
     * @throws DomainEntityNotFoundException if the service or its details are not found
     */
    public ServiceDescriptionResponse previewDescription(Long id, Long versionId) {
        EPService service = serviceRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service not found by id %s;".formatted(id)));

        validatePreviewPermissions(service);

        ServiceDetails details;
        if (versionId != null) {
            details = serviceDetailsRepository
                    .findByServiceIdAndVersionAndStatusIn(id, versionId, List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("versionId-Service details not found for given version id %s;".formatted(versionId)));
        } else {
            details = serviceDetailsRepository
                    .findFirstByServiceIdAndStatusInOrderByVersionDesc(id, List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Service detail not found by service id %s;".formatted(id)));
        }

        return ServiceDescriptionResponse
                .builder()
                .shortDescription(details.getShortDescription())
                .fullDescription(details.getFullDescription())
                .build();
    }


    /**
     * Validates the permissions for previewing the service.
     *
     * @param service the service to be previewed
     */
    private void validatePreviewPermissions(EPService service) {
        if (StringUtils.isNotEmpty(service.getCustomerIdentifier())) {
            if (service.getStatus().equals(ServiceStatus.ACTIVE)) {
                if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_INDIVIDUAL_BASIC))) {
                    log.error("You don’t have permission to view the individual service with ID: [%s]".formatted(service.getId()));
                    throw new AccessDeniedException("You don’t have permission to view the individual service with ID: [%s]".formatted(service.getId()));
                }
            } else {
                if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_INDIVIDUAL_DELETED))) {
                    log.error("You don’t have permission to view the individual service with ID: [%s]".formatted(service.getId()));
                    throw new AccessDeniedException("You don’t have permission to view the individual service with ID: [%s]".formatted(service.getId()));
                }
            }
        } else {
            if (service.getStatus().equals(ServiceStatus.ACTIVE)) {
                if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_BASIC))) {
                    log.error("You don’t have permission to view the service with ID: [%s]".formatted(service.getId()));
                    throw new AccessDeniedException("You don’t have permission to view the service with ID: [%s]".formatted(service.getId()));
                }
            } else {
                if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_DELETED))) {
                    log.error("You don’t have permission to view the service with ID: [%s]".formatted(service.getId()));
                    throw new AccessDeniedException("You don’t have permission to view the service with ID: [%s]".formatted(service.getId()));
                }
            }
        }
    }


    private List<ServiceRelatedEntityShortResponse> createRelatedEntitiesShortResponse(List<ServiceLinkedProduct> serviceLinkToProduct, List<ServiceLinkedService> serviceLinkedServices) {
        List<ServiceRelatedEntityShortResponse> productLinkedEntityShortResponse = new ArrayList<>(serviceLinkToProduct
                .stream()
                .filter(p -> p.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .map(serviceLinkedProduct -> {
                    Optional<ProductDetails> productDetailsOptional = productDetailsRepository
                            .findLatestDetails(serviceLinkedProduct.getProduct().getId(),
                                    List.of(ProductDetailStatus.ACTIVE, ProductDetailStatus.INACTIVE),
                                    Sort.by(Sort.Direction.DESC, "version"));

                    return productDetailsOptional
                            .map(productDetails ->
                                    new ServiceRelatedEntityShortResponse(serviceLinkedProduct, productDetails))
                            .orElseGet(() ->
                                    new ServiceRelatedEntityShortResponse(serviceLinkedProduct, null));
                })
                .toList());

        productLinkedEntityShortResponse
                .addAll(serviceLinkedServices
                        .stream()
                        .filter(s -> s.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                        .map(productLinkToService -> {
                            Optional<ServiceDetails> serviceDetailsOptional = serviceDetailsRepository
                                    .findLastDetailByServiceId(productLinkToService.getService().getId(),
                                            List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE),
                                            Sort.by(Sort.Direction.DESC, "version"));

                            return serviceDetailsOptional
                                    .map(serviceDetails ->
                                            new ServiceRelatedEntityShortResponse(productLinkToService, serviceDetails))
                                    .orElseGet(() ->
                                            new ServiceRelatedEntityShortResponse(productLinkToService, null));
                        })
                        .toList());

        return productLinkedEntityShortResponse
                .stream()
                .sorted(Comparator.comparing(ServiceRelatedEntityShortResponse::getCreateDate))
                .toList();
    }


    private List<PenaltyGroupShortResponse> createPenaltyGroupResponse(ServiceDetails details) {
        return details.getPenaltyGroups()
                .stream().filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServicePenaltyGroup::getPenaltyGroup)
                .map(x -> penaltyGroupDetailsRepository.findFirstByPenaltyGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(x.getId(), LocalDate.now())
                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Penalty Group details for penalty group with id: %s do not exists!", x.getId()))))
                .map(PenaltyGroupShortResponse::new)
                .toList();
    }


    private List<PriceComponentGroupShortResponse> createPriceComponentGroupResponse(ServiceDetails details) {
        List<ServicePriceComponentGroup> priceComponentGroups = details.getPriceComponentGroups();
        return priceComponentGroups.stream().filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServicePriceComponentGroup::getPriceComponentGroup)
                .map(x -> pcgDetailsRepository.findFirstByPriceComponentGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(x.getId(), LocalDate.now())
                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Price component group details not found for group: %s", x.getId()))))
                .map(PriceComponentGroupShortResponse::new)
                .toList();
    }


    private List<TerminationGroupShortResponse> createTerminationGroupResponse(ServiceDetails details) {
        List<ServiceTerminationGroup> terminationGroups = details.getTerminationGroups();
        return terminationGroups.stream().filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServiceTerminationGroup::getTerminationGroup)
                .map(x -> terminationGroupDetailsRepository.findFirstByTerminationGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(x.getId(), LocalDate.now())
                        .orElseThrow(() -> new DomainEntityNotFoundException(String.format("Termination Group details with group id %s do not exists", x.getId()))))
                .map(TerminationGroupShortResponse::new)
                .toList();
    }


    private List<InterimAdvancePaymentGroupShortResponse> createIAPGroupShortResponse(List<ServiceInterimAndAdvancePaymentGroup> advancePaymentGroups) {
        List<AdvancedPaymentGroup> advancedPaymentGroups = advancePaymentGroups.stream()
                .filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(ServiceInterimAndAdvancePaymentGroup::getAdvancedPaymentGroup)
                .toList();
        List<AdvancedPaymentGroupDetails> details = new ArrayList<>();
        for (AdvancedPaymentGroup group : advancedPaymentGroups) {
            advancedPaymentGroupDetailsRepository.findFirstByAdvancedPaymentGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(group.getId(), LocalDate.now())
                    .ifPresent(details::add);
        }
        return details.stream().map(InterimAdvancePaymentGroupShortResponse::new).toList();
    }

    private List<FileWithStatusesResponse> createServiceFileResponse(List<ServiceFile> serviceFiles) {
        return serviceFiles
                .stream()
                .filter(f -> f.getStatus().equals(EntityStatus.ACTIVE))
                .sorted(Comparator.comparing(BaseEntity::getCreateDate))
                .map(f -> new FileWithStatusesResponse(f, accountManagerRepository.findByUserName(f.getSystemUserId())
                        .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""))).toList();
    }


    private TermsGroupsShortResponse createTermsGroupShortResponse(TermsGroups termsGroups) {
        if (termsGroups == null) {
            return null;
        }
        TermGroupDetails termGroupDetails = termsGroupDetailsRepository
                .findFirstByGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(termsGroups.getId(), LocalDateTime.now())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Term group not found with ID %s;".formatted(termsGroups.getId())));
        return new TermsGroupsShortResponse(termsGroups, termGroupDetails);
    }


    /**
     * Updates the {@link EPService} with the provided id if all the validations are passed.
     *
     * @param id      the id of the {@link EPService} to be updated
     * @param request the {@link EditServiceRequest} containing the updated values
     * @return the id of the updated {@link EPService}
     */
    @Transactional
    public Long update(Long id, EditServiceRequest request) {
        log.debug("Updating service with id {} and request {}", id, request);

        List<String> exceptionMessages = new ArrayList<>();

        EPService service = serviceRepository
                .findByIdAndStatusIn(id, List.of(ServiceStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active service not found by id %s;".formatted(id)));

        ServiceDetails sourceServiceDetail = serviceDetailsRepository
                .findByServiceAndVersion(service, request.getVersionId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service version not found by N %s;".formatted(request.getVersionId())));

        validateUpdatePermissions(sourceServiceDetail, request);
        validateUpdateOperation(request, service);
        validateNameUniqueness(request.getBasicSettings().getName(), service.getId(), exceptionMessages);
        validateNumbersOfIncomeAccountAndCostCenterControllingOrder(request, exceptionMessages);

        if (request.getUpdateExistingVersion()) {
            updateExistingVersion(request, service, sourceServiceDetail, exceptionMessages);
        } else {
            updateAsNewVersion(request, sourceServiceDetail, exceptionMessages);
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        return id;
    }


    /**
     * Validates if the user has the required permissions to update the {@link EPService}.
     *
     * @param targetedServiceDetail the {@link EPService} to be updated
     * @param request               the {@link EditServiceRequest} containing the updated values
     */
    private void validateUpdatePermissions(ServiceDetails targetedServiceDetail, EditServiceRequest request) {
        boolean isIndividualService = StringUtils.isNotEmpty(targetedServiceDetail.getService().getCustomerIdentifier());

        if (serviceDetailsRepository.hasActiveConnectionToContract(targetedServiceDetail.getId())) {
            if (isIndividualService) {
                // if the individual service version is connected to a contract, user must have a permission to edit "locked ind." service
                if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_EDIT_INDIVIDUAL_LOCKED))) {
                    log.error("You do not have permission to edit individual service that is connected to a contract.");
                    throw new AccessDeniedException("You do not have permission to edit individual service that is connected to a contract.");
                } else {
                    // if user has permission to edit "locked ind." service, customer identifier still should not be changed
                    if (!targetedServiceDetail.getService().getCustomerIdentifier().equals(request.getBasicSettings().getCustomerIdentifier())) {
                        log.error("Changing customer identifier of a service that is connected to a contract is not allowed.");
                        throw new AccessDeniedException("Changing customer identifier of a service that is connected to a contract is not allowed.");
                    }
                }
            } else {
                if (request.getUpdateExistingVersion()) {
                    // if user wants to update a version of a non-individual service that is connected to a contract,
                    // user must have a permission to edit "locked" service. Creating a new version is not prohibited.
                    if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_EDIT_LOCKED))) {
                        log.error("You do not have permission to edit service that is connected to a contract.");
                        throw new AccessDeniedException("You do not have permission to edit service that is connected to a contract.");
                    }
                }
            }
        } else {
            if (isIndividualService) {
                // if the individual service is not connected to a contract, user must have one of the following permissions
                if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_EDIT_INDIVIDUAL_BASIC, SERVICES_EDIT_INDIVIDUAL_LOCKED))) {
                    log.error("You do not have permission to edit individual service.");
                    throw new AccessDeniedException("You do not have permission to edit individual service.");
                }
            } else {
                // if the non-individual service is not connected to a contract, user must have one of the following permissions
                if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_EDIT_BASIC, SERVICES_EDIT_LOCKED))) {
                    log.error("You do not have permission to edit standard service.");
                    throw new AccessDeniedException("You do not have permission to edit standard service.");
                }
            }
        }
    }


    /**
     * Validates the fields that are required to stay the same in the update operation.
     *
     * @param request the {@link EditServiceRequest} containing the updated values
     * @param service the {@link EPService} to be updated
     */
    private void validateUpdateOperation(EditServiceRequest request, EPService service) {
        if ((StringUtils.isBlank(service.getCustomerIdentifier()) && StringUtils.isNotBlank(request.getBasicSettings().getCustomerIdentifier()))
                || (StringUtils.isNotBlank(service.getCustomerIdentifier()) && StringUtils.isBlank(request.getBasicSettings().getCustomerIdentifier()))) {
            log.error("basicSettings.customerIdentifier-You cannot change service's type (individual/standard);");
            throw new OperationNotAllowedException("basicSettings.customerIdentifier-You cannot change service's type (individual/standard);");
        }

        if (BooleanUtils.isTrue(request.getBasicSettings().getIsIndividual())) {
            if (!request.getUpdateExistingVersion()) {
                log.error("Individual service cannot have versions.");
                throw new OperationNotAllowedException("Individual service cannot have versions.");
            }
        }
    }


    /**
     * Updates the {@link EPService} with the provided id if all the validations are passed.
     *
     * @param request              the {@link EditServiceRequest} containing the updated values
     * @param sourceServiceDetails the {@link ServiceDetails} of the current version of the service
     * @param exceptionMessages    the list of exception messages to be filled in case of validation errors
     */
    private void updateExistingVersion(EditServiceRequest request, EPService service, ServiceDetails sourceServiceDetails, List<String> exceptionMessages) {
        if (StringUtils.isNotEmpty(service.getCustomerIdentifier())) {
            // if the service is individual and implementation logic has come to this place,
            // it means that either the identifier is the same, or it has passed the
            // permission validation and is allowed to be changed (when not connected to a contract)
            service.setCustomerIdentifier(request.getBasicSettings().getCustomerIdentifier());
        }

        // This should be performed before updating the service details, so that the properties are not overwritten
        updateSubEntitiesForExistingVersion(sourceServiceDetails, request, exceptionMessages);
        checkForBoundObjects(sourceServiceDetails);
        // updates service details as an existing version
        ServiceDetails updatedServiceDetail = updateServiceDetails(request, sourceServiceDetails, exceptionMessages);

        // when existing version is updated, we need to update all subcomponents
        serviceContractTermService.updateServiceContractTerms(request.getContractTerms(), updatedServiceDetail, exceptionMessages);
        interimAdvancePaymentService.updateServiceIAPsForExistingVersion(request.getInterimAdvancePayments(), updatedServiceDetail, exceptionMessages);
        advancedPaymentGroupService.updateServiceIAPGroupsForExistingVersion(request.getInterimAdvancePaymentGroups(), updatedServiceDetail, exceptionMessages);
        penaltyService.updateServicePenaltiesForExistingVersion(request.getPenalties(), updatedServiceDetail, exceptionMessages);
        penaltyGroupService.updateServicePenaltyGroupsForExistingVersion(request.getPenaltyGroups(), updatedServiceDetail, exceptionMessages);
        terminationsService.updateServiceTerminationsForExistingVersion(request.getTerminations(), updatedServiceDetail, exceptionMessages);
        terminationGroupService.updateServiceTerminationGroupsForExistingVersion(request.getTerminationGroups(), updatedServiceDetail, exceptionMessages);
        priceComponentService.updateServicePriceComponentsForExistingVersion(request.getPriceComponents(), updatedServiceDetail, exceptionMessages);
        priceComponentGroupService.updateServicePriceComponentGroupsForExistingVersion(request.getPriceComponentGroups(), updatedServiceDetail, exceptionMessages);
        serviceFileService.updateServiceFiles(request.getServiceFiles(), updatedServiceDetail, exceptionMessages);
        archiveFiles(updatedServiceDetail);
        serviceRelatedEntitiesService.updateRelatedProductsAndServicesToProduct(request.getRelatedEntities(), updatedServiceDetail, exceptionMessages);
        updateTemplates(request.getTemplateIds(), updatedServiceDetail.getId(), exceptionMessages);

        updateLinkedAdditionalParams(request, sourceServiceDetails, exceptionMessages);
        updateConnectedCollectionChannels(request.getAdditionalSettings().getCollectionChannelIds(), sourceServiceDetails.getId(), exceptionMessages);
    }

    public void checkForBoundObjects(ServiceDetails sourceServiceDetails) {
        List<String> serviceDetails = serviceDetailsRepository.checkForBoundObjects(sourceServiceDetails.getId()); //TODO SQL NEEDED
        if (isIndividualService(sourceServiceDetails)) {
            if (!checkIfHasIndividualLockedPermission()) {
                throw new ClientException("You can't edit Individual Service because it is connected to the SERVICE_CONTRACT;", ErrorCode.CONFLICT);
            }
        }
        if (!Set.of("").containsAll(new HashSet<>(serviceDetails))) {
            if (!checkIfHasLockedPermission()) {
                throw new ClientException("You can't edit Service because it is connected to the %s;".formatted(serviceDetails), ErrorCode.CONFLICT);
            }
        }
    }

    public Boolean checkForBoundObjectsForPreview(ServiceDetails sourceServiceDetails) {
        List<String> serviceDetails = serviceDetailsRepository.checkForBoundObjects(sourceServiceDetails.getId()); //TODO SQL NEEDED
        return !Set.of("").containsAll(new HashSet<>(serviceDetails));
    }

    public boolean isIndividualService(ServiceDetails serviceDetails) {
        EPService service = serviceDetails.getService();
        if (service != null) {
            String customerIdentifier = service.getCustomerIdentifier();
            return StringUtils.isNotEmpty(customerIdentifier);
        } else return false;
    }

    private boolean checkIfHasLockedPermission() {
        List<String> customerContext = permissionService.getPermissionsFromContext(SERVICES);
        return customerContext.contains(SERVICES_EDIT_LOCKED.getId());
    }

    private boolean checkIfHasIndividualLockedPermission() {
        List<String> customerContext = permissionService.getPermissionsFromContext(SERVICES);
        return customerContext.contains(SERVICES_EDIT_INDIVIDUAL_LOCKED.getId());
    }


    /**
     * Creates a new version of the {@link EPService} with the provided id if all the validations are passed.
     *
     * @param request           the {@link EditServiceRequest} containing the updated values
     * @param currServiceDetail the {@link ServiceDetails} of the current version of the service
     * @param exceptionMessages the list of exception messages to be filled in case of validation errors
     */
    private void updateAsNewVersion(EditServiceRequest request, ServiceDetails currServiceDetail, List<String> exceptionMessages) {
        // update as a new version
        ServiceDetails newVersion = updateServiceDetails(request, currServiceDetail, exceptionMessages);

        // update the service with the last detail ID
        EPService service = currServiceDetail.getService();
        service.setLastServiceDetailId(newVersion.getId());
        serviceRepository.save(service);

        // update nomenclature sub entities
        updateSubEntitiesForNewVersion(newVersion, currServiceDetail, request, exceptionMessages);

        // when new version is created, we need to create the service contract terms and other subcomponents
        serviceContractTermService.createServiceContractTerms(
                newVersion,
                request.getContractTerms().stream().map(CreateServiceContractTermRequest::new).toList(),
                exceptionMessages
        );

        // we need to update non-group entities and add group entities for a new version
        interimAdvancePaymentService.updateServiceIAPsForNewVersion(request.getInterimAdvancePayments(), newVersion, currServiceDetail, exceptionMessages);
        advancedPaymentGroupService.addInterimAdvancePaymentGroupsToService(request.getInterimAdvancePaymentGroups(), newVersion, exceptionMessages);
        penaltyService.updateServicePenaltiesForNewVersion(request.getPenalties(), newVersion, currServiceDetail, exceptionMessages);
        penaltyGroupService.addPenaltyGroupsToService(request.getPenaltyGroups(), newVersion, exceptionMessages);
        terminationsService.updateServiceTerminationsForNewVersion(request.getTerminations(), newVersion, currServiceDetail, exceptionMessages);
        terminationGroupService.addTerminationGroupsToService(request.getTerminationGroups(), newVersion, exceptionMessages);
        priceComponentService.updateServicePriceComponentsForNewVersion(request.getPriceComponents(), newVersion, currServiceDetail, exceptionMessages);
        priceComponentGroupService.addPriceComponentGroupsToService(request.getPriceComponentGroups(), newVersion, exceptionMessages);
        serviceFileService.assignServiceFilesToServiceDetails(request.getServiceFiles(), newVersion, exceptionMessages, true);
        archiveFiles(newVersion);
        serviceRelatedEntitiesService.addRelatedProductsAndServicesToService(request.getRelatedEntities(), newVersion, exceptionMessages);
        updateTemplates(request.getTemplateIds(), newVersion.getId(), exceptionMessages);
        createServiceAdditionalParams(newVersion, request.getAdditionalSettings().getServiceAdditionalParams());
        updateRelatedServiceContracts(newVersion, request.getServiceDetailIdsForUpdatingServiceContracts(), exceptionMessages);
        createConnectedCollectionChannels(request.getAdditionalSettings().getCollectionChannelIds(), newVersion.getId(), exceptionMessages);
    }

    public Boolean validateServiceRelatedContractsUpdate(EditServiceRequest request) {
        List<String> exceptionContext = new ArrayList<>();
        if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(AUTOMATIC_RELATED_CONTRACT_UPDATE_FOR_SERVICE))) {
            exceptionContext.add("serviceDetailIdsForUpdatingServiceContracts-You have not access to automatic related contract update for Service;");
        } else {
            serviceRelatedContractUpdateService.hasServiceDetailFixedParameters(request, exceptionContext);
        }
        return exceptionContext.isEmpty();
    }

    public List<String> validateServiceRelatedContractsUpdateTest(EditServiceRequest request) {
        List<String> exceptionContext = new ArrayList<>();
        if (!permissionService.permissionContextContainsPermissions(SERVICES, List.of(AUTOMATIC_RELATED_CONTRACT_UPDATE_FOR_SERVICE))) {
            exceptionContext.add("serviceDetailIdsForUpdatingServiceContracts-You have not access to automatic related contract update for Service;");
        } else {
            serviceRelatedContractUpdateService.hasServiceDetailFixedParameters(request, exceptionContext);
        }
        return exceptionContext;
    }

    private void updateRelatedServiceContracts(ServiceDetails version, List<Long> serviceDetailIds, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(serviceDetailIds)) {
            serviceRelatedContractUpdateService.updateServiceContracts(version, serviceDetailIds, exceptionMessages);
        }
    }

    /**
     * Updates existing version of a service detail or creates a new one,
     * and populates target service details with the parameters from request and fetched entities,
     * taking into consideration statuses of nomenclature.
     *
     * @param request           {@link EditServiceRequest} containing updated information
     * @param sourceDetail      source version from which update was called
     * @param exceptionMessages list of error messages to be populated in case of failed validations
     * @return populated {@link ServiceDetails} object
     */
    private ServiceDetails updateServiceDetails(EditServiceRequest request, ServiceDetails sourceDetail, List<String> exceptionMessages) {
        ServiceBasicSettingsRequest basicSettings = request.getBasicSettings();

        // mandatory parameter, will always be present in source version
        ServiceType serviceType = fetchServiceType(
                basicSettings.getServiceTypeId(),
                basicSettings.getServiceTypeId().equals(sourceDetail.getServiceType().getId())
                        ? List.of(ACTIVE, INACTIVE) // if the type is not changed, should search in active and inactive types
                        : List.of(ACTIVE), // if the type is changed, should search only in active types
                exceptionMessages
        );

        // service group is an optional parameter while creating a service, so it can be null
        ServiceGroups serviceGroups = fetchServiceGroup(
                basicSettings.getServiceGroupId(),
                getServiceGroupStatusesWhenUpdating(sourceDetail, basicSettings),
                exceptionMessages
        );

        // vat rate is an optional parameter while creating a service, so it can be null
        VatRate vatRate = fetchVatRate(
                basicSettings.getVatRateId(),
                getVatRateStatusesWhenUpdating(sourceDetail, basicSettings),
                exceptionMessages
        );

        // service unit is an optional parameter while creating a service, so it can be null
        ServiceUnit serviceUnit = fetchServiceUnit(
                basicSettings.getServiceUnitId(),
                getServiceUnitStatusesWhenUpdating(sourceDetail, basicSettings),
                exceptionMessages
        );

        // price settings, cash deposit and bank guarantee currencies are optional parameters while creating a service, so they can be null
        Currency priceSettingsCurrency = fetchCurrency(
                request.getPriceSettings().getCurrencyId(),
                getCurrencyStatusesWhenUpdating(request.getPriceSettings().getCurrencyId(), sourceDetail.getCurrency()),
                exceptionMessages
        );

        Currency cashDepositCurrency = fetchCurrency(
                request.getBasicSettings().getCashDepositCurrencyId(),
                getCurrencyStatusesWhenUpdating(request.getBasicSettings().getCashDepositCurrencyId(), sourceDetail.getCashDepositCurrency()),
                exceptionMessages
        );

        Currency bankGuaranteeCurrency = fetchCurrency(
                request.getBasicSettings().getBankGuaranteeCurrencyId(),
                getCurrencyStatusesWhenUpdating(request.getBasicSettings().getBankGuaranteeCurrencyId(), sourceDetail.getBankGuaranteeCurrency()),
                exceptionMessages
        );

        Terms terms = null;
        if (request.getTerm() != null) {
            List<Long> availableTermIds = termsRepository.findAllAvailableTermIdsForService(List.of(request.getTerm()));

            // if terms was not provided in source version, or it is different, then its availability should be checked
            if (sourceDetail.getTerms() == null || !request.getTerm().equals(sourceDetail.getTerms().getId())) {
                if (availableTermIds.contains(request.getTerm())) {
                    terms = fetchTerm(request.getTerm(), List.of(TermStatus.ACTIVE), exceptionMessages);
                } else {
                    log.error("term-Term with ID %s is not available for adding to service;".formatted(request.getTerm()));
                    exceptionMessages.add("basicSettings.term[0]-Term with ID %s is not available for adding to service;".formatted(request.getTerm()));
                }
            } else {
                // it means that the source version had terms and the provided terms is the same in request
                if (request.getUpdateExistingVersion()) {
                    // term should be the same if not changed when updating existing version
                    terms = sourceDetail.getTerms();
                } else {
                    // if terms is the same as in the source detail, then it should be cloned for a new version
                    terms = termsService.cloneTerms(sourceDetail.getTerms().getId());
                }
            }
        }

        TermsGroups termsGroups = fetchTermsGroup(
                request.getTermGroup(),
                List.of(TermGroupStatus.ACTIVE), // if group is an active one, then it's available
                exceptionMessages
        );

        ServiceDetails updatedServiceDetail;
        if (request.getUpdateExistingVersion()) {
            // persisted service detail will be updated with information from request
            updatedServiceDetail = serviceMapper.updateServiceDetail(
                    sourceDetail,
                    request,
                    serviceGroups,
                    serviceType,
                    vatRate,
                    serviceUnit,
                    cashDepositCurrency,
                    bankGuaranteeCurrency,
                    priceSettingsCurrency,
                    terms,
                    termsGroups
            );
        } else {
            // new service detail will be created
            updatedServiceDetail = serviceMapper.fromRequestToServiceDetailsEntity(
                    request,
                    sourceDetail.getService(),
                    serviceGroups,
                    serviceType,
                    vatRate,
                    serviceUnit,
                    cashDepositCurrency,
                    bankGuaranteeCurrency,
                    priceSettingsCurrency,
                    terms,
                    termsGroups
            );

            // version will be increased only in case of editing as a new version
            Long lastDetailVersion = serviceDetailsRepository.findLastDetailVersion(sourceDetail.getService().getId());
            updatedServiceDetail.setVersion(lastDetailVersion + 1);
            // last group detail ID will be set to service in the calling method
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        return serviceDetailsRepository.saveAndFlush(updatedServiceDetail);
    }


    /**
     * Fetches service group nomenclature statuses based on source detail's properties
     *
     * @param sourceDetail  {@link ServiceDetails} source version
     * @param basicSettings {@link ServiceBasicSettingsRequest} containing information
     * @return list of {@link NomenclatureItemStatus}
     */
    private List<NomenclatureItemStatus> getServiceGroupStatusesWhenUpdating(ServiceDetails sourceDetail, ServiceBasicSettingsRequest basicSettings) {
        List<NomenclatureItemStatus> serviceGroupStatuses = new ArrayList<>();
        if (basicSettings.getServiceGroupId() != null) {
            if (sourceDetail.getServiceGroup() == null) {
                // if the source detail did not have a group and a group is provided request, should search only in active groups
                serviceGroupStatuses.add(ACTIVE);
            } else if (basicSettings.getServiceGroupId().equals(sourceDetail.getServiceGroup().getId())) {
                // if the group is not changed, should search in active and inactive groups
                serviceGroupStatuses.add(ACTIVE);
                serviceGroupStatuses.add(INACTIVE);
            } else {
                // if the group is changed, should search only in active groups
                serviceGroupStatuses.add(ACTIVE);
            }
        }
        return serviceGroupStatuses;
    }


    /**
     * Fetches vat rate nomenclature statuses based on source detail's properties
     *
     * @param sourceDetail  {@link ServiceDetails} source version
     * @param basicSettings {@link ServiceBasicSettingsRequest} containing information
     * @return list of {@link NomenclatureItemStatus}
     */
    private List<NomenclatureItemStatus> getVatRateStatusesWhenUpdating(ServiceDetails sourceDetail, ServiceBasicSettingsRequest basicSettings) {
        List<NomenclatureItemStatus> vatRateStatuses = new ArrayList<>();
        if (basicSettings.getVatRateId() != null) {
            if (sourceDetail.getVatRate() == null) {
                // if the source detail did not have a vat rate and a vat rate is provided in request, should search only in active items
                vatRateStatuses.add(ACTIVE);
            } else if (basicSettings.getVatRateId().equals(sourceDetail.getVatRate().getId())) {
                // if the vat rate is not changed, should search in active and inactive items
                vatRateStatuses.add(ACTIVE);
                vatRateStatuses.add(INACTIVE);
            } else {
                // if the vat rate is changed, should search only in active items
                vatRateStatuses.add(ACTIVE);
            }
        }

        return vatRateStatuses;
    }


    /**
     * Fetches service unit nomenclature statuses based on source detail's properties
     *
     * @param sourceDetail  {@link ServiceDetails} source version
     * @param basicSettings {@link ServiceBasicSettingsRequest} containing information
     * @return list of {@link NomenclatureItemStatus}
     */
    private List<NomenclatureItemStatus> getServiceUnitStatusesWhenUpdating(ServiceDetails sourceDetail, ServiceBasicSettingsRequest basicSettings) {
        List<NomenclatureItemStatus> serviceUnitStatuses = new ArrayList<>();
        if (basicSettings.getServiceUnitId() != null) {
            if (sourceDetail.getServiceUnit() == null) {
                // if the source detail did not have a service unit and a service unit is provided in request, should search only in active items
                serviceUnitStatuses.add(ACTIVE);
            } else if (basicSettings.getServiceUnitId().equals(sourceDetail.getServiceUnit().getId())) {
                // if the service unit is not changed, should search in active and inactive items
                serviceUnitStatuses.add(ACTIVE);
                serviceUnitStatuses.add(INACTIVE);
            } else {
                // if the vat rate is changed, should search only in active items
                serviceUnitStatuses.add(ACTIVE);
            }
        }
        return serviceUnitStatuses;
    }


    /**
     * Fetches currency nomenclature statuses based on source detail's properties
     *
     * @param requestedCurrencyId id of the requested currency
     * @param sourceCurrency      source currency
     * @return list of {@link NomenclatureItemStatus}
     */
    private List<NomenclatureItemStatus> getCurrencyStatusesWhenUpdating(Long requestedCurrencyId, Currency sourceCurrency) {
        List<NomenclatureItemStatus> currencyStatuses = new ArrayList<>();
        if (requestedCurrencyId != null) {
            if (sourceCurrency == null) {
                currencyStatuses.add(ACTIVE);
            } else if (requestedCurrencyId.equals(sourceCurrency.getId())) {
                currencyStatuses.add(ACTIVE);
                currencyStatuses.add(INACTIVE);
            } else {
                currencyStatuses.add(ACTIVE);
            }
        }
        return currencyStatuses;
    }


    /**
     * Updates sub entities for existing version of service details
     *
     * @param sourceServiceDetail source service details
     * @param request             edit service request
     * @param exceptionMessages   list of error messages to be populated in case of failed validtions
     */
    private void updateSubEntitiesForExistingVersion(ServiceDetails sourceServiceDetail,
                                                     EditServiceRequest request,
                                                     List<String> exceptionMessages) {
        ServiceBasicSettingsRequest basicSettingsRequest = request.getBasicSettings();

        if (StringUtils.isEmpty(sourceServiceDetail.getService().getCustomerIdentifier())) {
            // only non-individual service can have the following properties
            updateSalesAreaForExistingVersion(basicSettingsRequest, sourceServiceDetail, exceptionMessages);
            updateSalesChannelsForExistingVersion(basicSettingsRequest, sourceServiceDetail, exceptionMessages);
            updateSegmentsForExistingVersion(basicSettingsRequest, sourceServiceDetail, exceptionMessages);
        }
        if (BooleanUtils.isNotTrue(request.getBasicSettings().getGlobalGridOperator())) {
            updateGridOperatorsForExistingVersion(basicSettingsRequest, sourceServiceDetail, exceptionMessages);
        } else {
            List<ServiceGridOperator> redundantGridOperators = serviceGridOperatorsRepository.findByServiceDetailsIdAndStatusIn(sourceServiceDetail.getId(), List.of(ServiceSubobjectStatus.ACTIVE));
            redundantGridOperators.forEach(go -> go.setStatus(ServiceSubobjectStatus.DELETED));
            serviceGridOperatorsRepository.saveAll(redundantGridOperators);
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
    }


    /**
     * Updates sales areas for existing version of service details
     *
     * @param basicSettingsRequest basic settings request
     * @param sourceServiceDetails source service details
     * @param exceptionMessages    list of error messages to be populated in case of failed validtions
     */
    private void updateSalesAreaForExistingVersion(ServiceBasicSettingsRequest basicSettingsRequest,
                                                   ServiceDetails sourceServiceDetails,
                                                   List<String> exceptionMessages) {
        if (sourceServiceDetails.getGlobalSalesArea() && !basicSettingsRequest.getGlobalSalesAreas()) {
            // source version had global sales area, but requested one - does not, so provided sales areas should be active
            // and new service sales area entities should be created
            List<ServiceSalesArea> serviceSalesAreas = fetchSalesAreas(basicSettingsRequest.getSalesAreas(), List.of(ACTIVE), exceptionMessages)
                    .stream()
                    .map(sa -> new ServiceSalesArea(null, sourceServiceDetails, sa, ServiceSubobjectStatus.ACTIVE))
                    .toList();
            serviceSalesAreasRepository.saveAll(serviceSalesAreas);
        } else if (!sourceServiceDetails.getGlobalSalesArea() && basicSettingsRequest.getGlobalSalesAreas()) {
            // source version didn't have global sales area, but requested one - has, so persisted sales area entities should be deleted
            List<ServiceSalesArea> persistedSalesAreas = serviceSalesAreasRepository.findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));
            persistedSalesAreas.forEach(s -> s.setStatus(ServiceSubobjectStatus.DELETED));
            serviceSalesAreasRepository.saveAll(persistedSalesAreas);
        } else if (!sourceServiceDetails.getGlobalSalesArea()) {
            // this means that the requested version must not have global sales area too, so sales areas are provided in request
            // and all requested sales areas should be processed
            List<ServiceSalesArea> dbSaleAreas = serviceSalesAreasRepository // fetch active persisted sale areas
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            List<Long> dbSalesAreaIds = dbSaleAreas
                    .stream()
                    .map(sa -> sa.getSalesArea().getId())
                    .toList();

            List<SalesArea> salesAreas = fetchSalesAreas(
                    basicSettingsRequest.getSalesAreas().stream()
                            .filter(id -> !dbSalesAreaIds.contains(id))
                            .collect(Collectors.toSet()),
                    List.of(ACTIVE), // if new sales areas are added, they should be searched in active sales areas
                    exceptionMessages
            );

            List<ServiceSalesArea> serviceSalesAreas = salesAreas.stream()
                    .map(sa -> new ServiceSalesArea(null, sourceServiceDetails, sa, ServiceSubobjectStatus.ACTIVE))
                    .toList();

            serviceSalesAreasRepository.saveAll(serviceSalesAreas);

            // delete removed sale areas
            dbSaleAreas
                    .stream()
                    .filter(s -> !basicSettingsRequest.getSalesAreas().contains(s.getSalesArea().getId()))
                    .toList()
                    .forEach(s -> s.setStatus(ServiceSubobjectStatus.DELETED));
            serviceSalesAreasRepository.saveAll(dbSaleAreas);
        }
        // if both, source version and the requested properties have global sale areas, no action is needed
    }


    /**
     * Updates sales channels for existing version of service details
     *
     * @param basicSettingsRequest basic settings request of service
     * @param sourceServiceDetails source service details
     * @param exceptionMessages    list of error messages to be populated in case of failed validtions
     */
    private void updateSalesChannelsForExistingVersion(ServiceBasicSettingsRequest basicSettingsRequest,
                                                       ServiceDetails sourceServiceDetails,
                                                       List<String> exceptionMessages) {
        if (sourceServiceDetails.getGlobalSalesChannel() && !basicSettingsRequest.getGlobalSalesChannel()) {
            // source version had global sales channel, but requested one - does not, so provided sales channels should be active
            // and new service sales channel entities should be created
            List<ServiceSalesChannel> serviceSalesChannels = fetchSalesChannels(basicSettingsRequest.getSalesChannels(), List.of(ACTIVE), exceptionMessages)
                    .stream()
                    .map(sch -> new ServiceSalesChannel(null, sourceServiceDetails, sch, ServiceSubobjectStatus.ACTIVE))
                    .toList();
            serviceSalesChannelsRepository.saveAll(serviceSalesChannels);
        } else if (!sourceServiceDetails.getGlobalSalesChannel() && basicSettingsRequest.getGlobalSalesChannel()) {
            // source version didn't have global sales channel, but requested one - has, so persisted sales channel entities should be deleted
            List<ServiceSalesChannel> persistedSalesChannels = serviceSalesChannelsRepository.findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));
            persistedSalesChannels.forEach(sch -> sch.setStatus(ServiceSubobjectStatus.DELETED));
            serviceSalesChannelsRepository.saveAll(persistedSalesChannels);
        } else if (!sourceServiceDetails.getGlobalSalesChannel()) {
            // this means that the updated version must not have global sales channel too, so sales channels are proivded in request
            // and all requested sales channels should be processed
            List<ServiceSalesChannel> dbSalesChannels = serviceSalesChannelsRepository // fetch active persisted sales channels
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            List<Long> dbSalesChannelIds = dbSalesChannels
                    .stream()
                    .map(sch -> sch.getSalesChannel().getId())
                    .toList();

            List<SalesChannel> salesChannels = fetchSalesChannels(
                    basicSettingsRequest.getSalesChannels().stream()
                            .filter(id -> !dbSalesChannelIds.contains(id))
                            .collect(Collectors.toSet()),
                    List.of(ACTIVE), // if new sales channels are added, they should be searched in active sales channels
                    exceptionMessages
            );

            List<ServiceSalesChannel> serviceSalesChannels = salesChannels.stream()
                    .map(sch -> new ServiceSalesChannel(null, sourceServiceDetails, sch, ServiceSubobjectStatus.ACTIVE))
                    .toList();

            serviceSalesChannelsRepository.saveAll(serviceSalesChannels);

            // delete removed sales channels
            dbSalesChannels
                    .stream()
                    .filter(s -> !basicSettingsRequest.getSalesChannels().contains(s.getSalesChannel().getId()))
                    .toList()
                    .forEach(s -> s.setStatus(ServiceSubobjectStatus.DELETED));
            serviceSalesChannelsRepository.saveAll(dbSalesChannels);
        }
        // if both, source version nd the requested properties have global sale channel, no action is needed
    }


    /**
     * Updates segments for existing version of service details
     *
     * @param basicSettingsRequest request with basic settings for service
     * @param sourceServiceDetails source service details instance
     * @param exceptionMessages    list of exception messages to be filled in case of errors
     */
    private void updateSegmentsForExistingVersion(ServiceBasicSettingsRequest basicSettingsRequest,
                                                  ServiceDetails sourceServiceDetails,
                                                  List<String> exceptionMessages) {
        if (sourceServiceDetails.getGlobalSegment() && !basicSettingsRequest.getGlobalSegment()) {
            // source version had global segment, but updated one - does not, so new service segment entities should be created
            List<Segment> segments = fetchSegments(basicSettingsRequest.getSegments(), List.of(ACTIVE), exceptionMessages);
            List<ServiceSegment> serviceSegments = fetchSegments(basicSettingsRequest.getSegments(), List.of(ACTIVE), exceptionMessages)
                    .stream()
                    .map(seg -> new ServiceSegment(null, sourceServiceDetails, seg, ServiceSubobjectStatus.ACTIVE))
                    .toList();
            serviceSegmentsRepository.saveAll(serviceSegments);
        } else if (!sourceServiceDetails.getGlobalSegment() && basicSettingsRequest.getGlobalSegment()) {
            // source version didn't have global segment, but updated one - has, so persisted segment entities should be deleted
            List<ServiceSegment> persistedSegments = serviceSegmentsRepository.findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));
            persistedSegments.forEach(s -> s.setStatus(ServiceSubobjectStatus.DELETED));
            serviceSegmentsRepository.saveAll(persistedSegments);
        } else if (!sourceServiceDetails.getGlobalSegment()) {
            // this means that the updated version must not have global segment too, so segments are provided in request
            // and all requested segments should be processed
            List<ServiceSegment> dbSegments = serviceSegmentsRepository // fetch active persisted segments
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            List<Long> dbSegmentIds = dbSegments
                    .stream()
                    .map(seg -> seg.getSegment().getId())
                    .toList();

            List<Segment> segments = fetchSegments(
                    basicSettingsRequest.getSegments().stream()
                            .filter(id -> !dbSegmentIds.contains(id))
                            .collect(Collectors.toSet()),
                    List.of(ACTIVE), // if new segments are added, they should be searched in active segments
                    exceptionMessages
            );

            List<ServiceSegment> serviceSegments = segments.stream()
                    .map(seg -> new ServiceSegment(null, sourceServiceDetails, seg, ServiceSubobjectStatus.ACTIVE))
                    .toList();

            serviceSegmentsRepository.saveAll(serviceSegments);

            // delete removed segments
            dbSegments
                    .stream()
                    .filter(s -> !basicSettingsRequest.getSegments().contains(s.getSegment().getId()))
                    .toList()
                    .forEach(s -> s.setStatus(ServiceSubobjectStatus.DELETED));
            serviceSegmentsRepository.saveAll(dbSegments);
        }
    }


    /**
     * Updates grid operators for existing version of service details
     *
     * @param basicSettingsRequest request with basic settings for service
     * @param sourceServiceDetails source service details instance
     * @param exceptionMessages    list of exception messages to be filled in case of errors
     */
    private void updateGridOperatorsForExistingVersion(ServiceBasicSettingsRequest basicSettingsRequest,
                                                       ServiceDetails sourceServiceDetails,
                                                       List<String> exceptionMessages) {
        // grid operators are optional when creating, so this list may be empty
        List<ServiceGridOperator> dbGridOperators = serviceGridOperatorsRepository // fetch grid operators from source service details
                .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        List<GridOperator> tempList = new ArrayList<>();
        Set<Long> requestGridOperators = basicSettingsRequest.getGridOperators();

        if (CollectionUtils.isNotEmpty(requestGridOperators)) {
            if (dbGridOperators.isEmpty()) {
                // this means that all provided grid operators in request should be active
                List<GridOperator> gridOperators = fetchGridOperators(
                        requestGridOperators,
                        List.of(ACTIVE),
                        exceptionMessages
                );

                tempList.addAll(gridOperators);
            } else {
                // this means that newly added ones should be filtered by active status
                List<Long> dbGridOperatorIds = dbGridOperators
                        .stream()
                        .map(sgo -> sgo.getGridOperator().getId())
                        .toList();

                List<GridOperator> gridOperators = fetchGridOperators(
                        requestGridOperators.stream()
                                .filter(id -> !dbGridOperatorIds.contains(id))
                                .collect(Collectors.toSet()), // if new grid operators are added, they should be searched in active grid operators
                        List.of(ACTIVE),
                        exceptionMessages
                );

                tempList.addAll(gridOperators);
            }
        }

        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return;
        }

        if (CollectionUtils.isNotEmpty(tempList)) {
            // new grid operators should be added
            List<ServiceGridOperator> serviceGridOperators = tempList.stream()
                    .map(sgo -> new ServiceGridOperator(null, sourceServiceDetails, sgo, ServiceSubobjectStatus.ACTIVE))
                    .toList();

            serviceGridOperatorsRepository.saveAll(serviceGridOperators);
        }

        if (CollectionUtils.isNotEmpty(dbGridOperators)) {
            if (CollectionUtils.isEmpty(requestGridOperators)) {
                // this means that all grid operators were removed
                dbGridOperators.forEach(go -> go.setStatus(ServiceSubobjectStatus.DELETED));
                serviceGridOperatorsRepository.saveAll(dbGridOperators);
            } else {
                // filter removed grid operators
                dbGridOperators
                        .stream()
                        .filter(sgo -> !requestGridOperators.contains(sgo.getGridOperator().getId()))
                        .toList()
                        .forEach(sgo -> sgo.setStatus(ServiceSubobjectStatus.DELETED));
                serviceGridOperatorsRepository.saveAll(dbGridOperators);
            }
        }
    }


    /**
     * Updates sub entities (sales areas, segments, sales channels, grid operators) for new version of service details
     *
     * @param updatedServiceDetailsInstance updated service details instance
     * @param sourceServiceDetails          source service details instance
     * @param request                       request with basic settings for service
     * @param exceptionMessages             list of exception messages to be filled in case of errors
     */
    private void updateSubEntitiesForNewVersion(ServiceDetails updatedServiceDetailsInstance,
                                                ServiceDetails sourceServiceDetails,
                                                EditServiceRequest request,
                                                List<String> exceptionMessages) {
        ServiceBasicSettingsRequest basicSettings = request.getBasicSettings();

        // fetch nomenclatures with statuses according to update logic and create new entities for a new version
        createSubObjectsAndAssignToDetail(
                updatedServiceDetailsInstance,
                fetchSalesAreasWhenUpdatingAsNewVersion(basicSettings.getGlobalSalesAreas(), sourceServiceDetails, basicSettings.getSalesAreas(), exceptionMessages),
                fetchSalesChannelsWhenUpdatingAsNewVersion(basicSettings.getGlobalSalesChannel(), sourceServiceDetails, basicSettings.getSalesChannels(), exceptionMessages),
                fetchSegmentsWhenUpdatingAsNewVersion(basicSettings.getGlobalSegment(), sourceServiceDetails, basicSettings.getSegments(), exceptionMessages),
                fetchGridOperatorsWhenUpdatingAsNewVersion(basicSettings.getGlobalGridOperator(), sourceServiceDetails, basicSettings.getGridOperators(), exceptionMessages)
        );
    }


    /**
     * Fetches sales areas for new version of service details when updating
     *
     * @param globalSalesArea       flag indicating whether global sales area was specified
     * @param sourceServiceDetails  source service details instance
     * @param requestedSalesAreaIds requested sales area ids
     * @param exceptionMessages     list of exception messages to be filled in case of errors
     * @return list of sales areas
     */
    private List<SalesArea> fetchSalesAreasWhenUpdatingAsNewVersion(boolean globalSalesArea,
                                                                    ServiceDetails sourceServiceDetails,
                                                                    Set<Long> requestedSalesAreaIds,
                                                                    List<String> exceptionMessages) {
        List<SalesArea> salesAreas = new ArrayList<>();
        if (!globalSalesArea) { // if global sales area was not specified, then sales areas should be provided
            log.debug("Processing requested sales areas with ids: {} for updating as a new version;", requestedSalesAreaIds);

            // this list may be empty if source version had global sales areas specified
            List<ServiceSalesArea> serviceSaleAreas = serviceSalesAreasRepository // fetch persisted sales areas
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            if (serviceSaleAreas.isEmpty()) {
                // if new sales areas are added, they should be searched in active sales areas
                salesAreas.addAll(fetchSalesAreas(
                        requestedSalesAreaIds,
                        List.of(ACTIVE),
                        exceptionMessages
                ));
            } else {
                List<Long> dbSalesAreaIds = serviceSaleAreas
                        .stream()
                        .map(sa -> sa.getSalesArea().getId())
                        .toList();

                salesAreas.addAll(fetchSalesAreas(
                        requestedSalesAreaIds.stream()
                                .filter(id -> !dbSalesAreaIds.contains(id))
                                .collect(Collectors.toSet()),
                        List.of(ACTIVE), // if new sales areas are added, they should be searched in active sales areas
                        exceptionMessages
                ));

                salesAreas.addAll(fetchSalesAreas(
                        requestedSalesAreaIds.stream()
                                .filter(dbSalesAreaIds::contains)
                                .collect(Collectors.toSet()),
                        List.of(ACTIVE, INACTIVE), // if existing sales areas are left, they should be searched in active and inactive sales areas
                        exceptionMessages
                ));
            }
        }

        return salesAreas;
    }


    /**
     * Fetches sales channels for new version of service details when updating as a new version
     *
     * @param globalSalesChannel       flag indicating whether global sales channel was specified
     * @param sourceServiceDetails     source service details instance
     * @param requestedSalesChannelIds requested sales channel ids
     * @param exceptionMessages        list of exception messages to be filled in case of errors
     * @return list of sales channels
     */
    private List<SalesChannel> fetchSalesChannelsWhenUpdatingAsNewVersion(boolean globalSalesChannel,
                                                                          ServiceDetails sourceServiceDetails,
                                                                          Set<Long> requestedSalesChannelIds,
                                                                          List<String> exceptionMessages) {
        List<SalesChannel> salesChannels = new ArrayList<>();
        if (!globalSalesChannel) { // if global sales channel was not specified, then sales channels should be provided
            log.debug("Processing requested sales channels with ids: {} for updating as a new version;", requestedSalesChannelIds);

            // this list may be empty if source version had global sales channels specified
            List<ServiceSalesChannel> serviceSalesChannels = serviceSalesChannelsRepository // fetch sales channels from source service details
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            if (serviceSalesChannels.isEmpty()) {
                salesChannels.addAll(fetchSalesChannels(
                        requestedSalesChannelIds,
                        List.of(ACTIVE),
                        exceptionMessages
                ));
            } else {
                List<Long> dbSalesChannelIds = serviceSalesChannels
                        .stream()
                        .map(sch -> sch.getSalesChannel().getId())
                        .toList();

                salesChannels.addAll(fetchSalesChannels(
                        requestedSalesChannelIds.stream()
                                .filter(id -> !dbSalesChannelIds.contains(id))
                                .collect(Collectors.toSet()),
                        List.of(ACTIVE), // if new sales channels are added, they should be searched in active sales channels
                        exceptionMessages
                ));

                salesChannels.addAll(fetchSalesChannels(
                        requestedSalesChannelIds.stream()
                                .filter(dbSalesChannelIds::contains)
                                .collect(Collectors.toSet()),
                        List.of(ACTIVE, INACTIVE), // if existing sales channels are left, they should be searched in active and inactive sales channels
                        exceptionMessages
                ));
            }
        }

        return salesChannels;
    }


    /**
     * Fetches segments for new version of service details when updating as a new version of service details
     *
     * @param globalSegment        flag indicating whether global segment was specified
     * @param sourceServiceDetails source service details instance
     * @param requestedSegmentIds  requested segment ids
     * @param exceptionMessages    list of exception messages to be filled in case of errors
     * @return list of segments
     */
    private List<Segment> fetchSegmentsWhenUpdatingAsNewVersion(boolean globalSegment,
                                                                ServiceDetails sourceServiceDetails,
                                                                Set<Long> requestedSegmentIds,
                                                                List<String> exceptionMessages) {
        List<Segment> segments = new ArrayList<>();
        if (!globalSegment) { // if global segment was not specified, then segments should be provided
            log.debug("Processing requested segments with ids: {} for updating as a new version;", requestedSegmentIds);

            // this list may be empty if source version had global segments specified
            List<ServiceSegment> serviceSegments = serviceSegmentsRepository // fetch segments from source service details
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            if (serviceSegments.isEmpty()) {
                segments.addAll(fetchSegments(
                        requestedSegmentIds,
                        List.of(ACTIVE),
                        exceptionMessages
                ));
            } else {
                List<Long> dbSegmentIds = serviceSegments
                        .stream()
                        .map(ss -> ss.getSegment().getId())
                        .toList();

                segments.addAll(fetchSegments(
                        requestedSegmentIds.stream()
                                .filter(id -> !dbSegmentIds.contains(id))
                                .collect(Collectors.toSet()),
                        List.of(ACTIVE), // if new segments are added, they should be searched in active segments
                        exceptionMessages
                ));

                segments.addAll(fetchSegments(
                        requestedSegmentIds.stream()
                                .filter(dbSegmentIds::contains)
                                .collect(Collectors.toSet()),
                        List.of(ACTIVE, INACTIVE), // if existing segments are left, they should be searched in active and inactive segments
                        exceptionMessages
                ));
            }
        }

        return segments;
    }


    /**
     * Fetches grid operators for new version of service details when updating as a new version of service details
     *
     * @param sourceServiceDetails     source service details instance
     * @param requestedGridOperatorIds requested grid operator ids
     * @param exceptionMessages        list of exception messages to be filled in case of errors
     * @return list of grid operators
     */
    private List<GridOperator> fetchGridOperatorsWhenUpdatingAsNewVersion(Boolean globalGridOperator,
                                                                          ServiceDetails sourceServiceDetails,
                                                                          Set<Long> requestedGridOperatorIds,
                                                                          List<String> exceptionMessages) {
        List<GridOperator> gridOperators = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(requestedGridOperatorIds) && BooleanUtils.isNotTrue(globalGridOperator)) {
            log.debug("Processing requested grid operators with ids: {} for updating service as a new version;", requestedGridOperatorIds);

            // this list may be empty, as grid operators are not mandatory when creating
            List<ServiceGridOperator> serviceGridOperators = serviceGridOperatorsRepository // fetch grid operators from source service details
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            if (CollectionUtils.isEmpty(serviceGridOperators)) {
                // if new grid operators are added, they should be searched in active grid operators
                gridOperators.addAll(fetchGridOperators(
                        requestedGridOperatorIds,
                        List.of(ACTIVE),
                        exceptionMessages
                ));
            } else {
                List<Long> dbGridOperatorIds = serviceGridOperators
                        .stream()
                        .map(sgo -> sgo.getGridOperator().getId())
                        .toList();

                gridOperators.addAll(fetchGridOperators(
                        requestedGridOperatorIds.stream()
                                .filter(id -> !dbGridOperatorIds.contains(id))
                                .collect(Collectors.toSet()),
                        List.of(ACTIVE), // if new grid operators are added, they should be searched in active grid operators
                        exceptionMessages
                ));

                gridOperators.addAll(fetchGridOperators(
                        requestedGridOperatorIds.stream()
                                .filter(dbGridOperatorIds::contains)
                                .collect(Collectors.toSet()),
                        List.of(ACTIVE, INACTIVE), // if existing grid operators are left, they should be searched in active and inactive grid operators
                        exceptionMessages
                ));
            }
        }

        return gridOperators;
    }


    /**
     * Returns a {@link Page} of {@link ServiceListResponse} based on the provided {@link ServiceListRequest}.
     *
     * @param request the {@link ServiceListRequest} to be used for filtering
     * @return the {@link Page} of {@link ServiceListResponse} that match the provided criteria
     */
    public Page<ServiceListResponse> list(ServiceListRequest request) {
        log.debug("Fetching services based on the provided request: {}", request);

        String sortBy = ServiceTableColumn.SERVICE_DATE_OF_CREATION.getValue();
        if (request.getSortBy() != null && StringUtils.isNotEmpty(request.getSortBy().getValue())) {
            sortBy = request.getSortBy().getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String searchBy = ServiceSearchField.ALL.getValue();
        if (request.getSearchBy() != null && StringUtils.isNotEmpty(request.getSearchBy().getValue())) {
            searchBy = request.getSearchBy().getValue();
        }

        StringBuilder consumptionPurposes = new StringBuilder();
        if (CollectionUtils.isNotEmpty(request.getConsumptionPurposes())) {
            consumptionPurposes.append("{");
            consumptionPurposes.append(
                    request.getConsumptionPurposes()
                            .stream()
                            .map(ServiceConsumptionPurpose::name)
                            .collect(Collectors.joining(","))
            );
            consumptionPurposes.append("}");
        }

        String excludeOldVersion = String.valueOf(Boolean.TRUE.equals(request.getExcludeOldVersions()));

        return serviceRepository
                .list(
                        searchBy,
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getServiceDetailStatuses()),
                        CollectionUtils.isNotEmpty(request.getServiceGroupIds()) ? request.getServiceGroupIds() : new ArrayList<>(),
                        CollectionUtils.isNotEmpty(request.getServiceTypeIds()) ? request.getServiceTypeIds() : new ArrayList<>(),
                        CollectionUtils.isNotEmpty(request.getServiceContractTermNames()) ? request.getServiceContractTermNames() : new ArrayList<>(),
                        sortDirection.name(),
                        CollectionUtils.isNotEmpty(request.getSalesChannelsIds()) ? request.getSalesChannelsIds() : new ArrayList<>(),
                        request.getGlobalSalesChannel(),
                        sortDirection.name(),
                        CollectionUtils.isNotEmpty(request.getSegmentIds()) ? request.getSegmentIds() : new ArrayList<>(),
                        request.getGlobalSegment(),
                        consumptionPurposes.isEmpty() ? null : consumptionPurposes.toString(),
                        request.getIndividualServiceOption() == null || request.getIndividualServiceOption().equals(IndividualServiceOption.ALL)
                                ? IndividualServiceOption.ALL.name() : request.getIndividualServiceOption().name(),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(getListingStandardPermissions()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(getListingIndividualPermissions()),
                        excludeOldVersion,
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(new Sort.Order(sortDirection, sortBy))
                        )
                ).map(ServiceListResponse::new);
    }


    /**
     * @return list of permissions for listing services
     */
    private List<String> getListingPermissions() {
        List<String> permissions = new ArrayList<>();

        if (permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_BASIC))) {
            permissions.add("STANDARD_AND_ACTIVE");
        }

        if (permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_DELETED))) {
            permissions.add("STANDARD_AND_DELETED");
        }

        if (permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_INDIVIDUAL_BASIC))) {
            permissions.add("INDIVIDUAL_AND_ACTIVE");
        }

        if (permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_INDIVIDUAL_DELETED))) {
            permissions.add("INDIVIDUAL_AND_DELETED");
        }

        return permissions;
    }

    private List<ServiceStatus> getListingStandardPermissions() {
        List<ServiceStatus> permissions = new ArrayList<>();

        if (permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_BASIC))) {
            permissions.add(ServiceStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_DELETED))) {
            permissions.add(ServiceStatus.DELETED);
        }

        return permissions;
    }

    private List<ServiceStatus> getListingIndividualPermissions() {
        List<ServiceStatus> permissions = new ArrayList<>();

        if (permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_INDIVIDUAL_BASIC))) {
            permissions.add(ServiceStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(SERVICES, List.of(SERVICES_VIEW_INDIVIDUAL_DELETED))) {
            permissions.add(ServiceStatus.DELETED);
        }

        return permissions;
    }

    /**
     * Returns a list of unique combined names of service contract terms for listing purposes.
     *
     * @return the list of unique combined names of service contract terms
     */
    public Page<ContractTermNameResponse> getContractTermNames(int page, int size, String prompt) {
        log.debug("Fetching unique combined names of service contract terms");
        return serviceContractTermService.getDistinctContractTermsByNameAndStatus(List.of(ServiceSubobjectStatus.ACTIVE), page, size, StringUtil.underscoreReplacer(prompt));
    }

    /**
     * Returns a list of available products and services for service, satisfying the following criteria:
     * <ul>
     *     <li>product or service is active and their details are active</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available products and services for service
     */
    public Page<AvailableServiceRelatedEntitiyResponse> findAvailableProductsAndServices(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available products and services for service with request: {}", request);
        return serviceRepository
                .findAvailableProductsAndServices(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getExcludedId(), // ID of the product which is being edited
                        request.getExcludedItemId(), // ID of the service/product which is selected in dropdown
                        request.getExcludedItemType(), // Type of the service/product which is selected in dropdown
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available terms for service, satisfying the following criteria:
     * <ul>
     *     <li>term is not already assigned to active service</li>
     *     <li>term is not already assigned to active product</li>
     *     <li>term name contains the prompt</li>
     *     </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available terms for service
     */
    public Page<ShortResponseProjection> findAvailableTerms(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available terms for service with request: {}", request);
        return termsRepository
                .findAvailableTermsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available terms groups for service, satisfying the following criteria:
     * <ul>
     *     <li>terms group is not already assigned to active service</li>
     *     <li>terms group is not already assigned to active product</li>
     *     <li>terms group name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available terms groups for service
     */
    public Page<AvailableServiceRelatedGroupEntityResponse> findAvailableTermsGroups(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available terms groups for service with request: {}", request);
        return termsGroupsRepository
                .findAvailableTermsGroupsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available interim advance payments for service, satisfying the following criteria:
     * <ul>
     *     <li>interim advance payment is not already assigned to active service</li>
     *     <li>interim advance payment is not already assigned to active product</li>
     *     <li>interim advance payment name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available interim advance payments for service
     */
    public Page<AvailableServiceRelatedEntitiyResponse> findAvailableInterimAdvancePayments(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available interim advance payments for service with request: {}", request);
        return interimAdvancePaymentRepository
                .findAvailableInterimAdvancePaymentsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available interim advance payments groups for service, satisfying the following criteria:
     * <ul>
     *     <li>interim advance payments group is not already assigned to active service</li>
     *     <li>interim advance payments group is not already assigned to active product</li>
     *     <li>interim advance payments group name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available interim advance payments groups for service
     */
    public Page<AvailableServiceRelatedGroupEntityResponse> findAvailableInterimAdvancePaymentsGroups(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available interim advance payments groups for service with request: {}", request);
        return advancedPaymentGroupRepository
                .findAvailableInterimAdvancePaymentsGroupsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available price components for service, satisfying the following criteria:
     * <ul>
     *     <li>price component is not already assigned to active service</li>
     *     <li>price component is not already assigned to active product</li>
     *     <li>price component name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available price components for service
     */
    public Page<AvailableServiceRelatedEntitiyResponse> findAvailablePriceComponents(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available price components for service with request: {}", request);
        return priceComponentRepository
                .findAvailablePriceComponentsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available price component groups for service, satisfying the following criteria:
     * <ul>
     *     <li>price component group is not already assigned to active service</li>
     *     <li>price component group is not already assigned to active product</li>
     *     <li>price component group name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available price component groups for service
     */
    public Page<AvailableServiceRelatedGroupEntityResponse> findAvailablePriceComponentGroups(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available price component groups for service with request: {}", request);
        return priceComponentGroupRepository
                .findAvailablePriceComponentGroupsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available terminations for service, satisfying the following criteria:
     * <ul>
     *     <li>termination is not already assigned to active service</li>
     *     <li>termination is not already assigned to active product</li>
     *     <li>termination name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available terminations for service
     */
    public Page<AvailableServiceRelatedEntitiyResponse> findAvailableTerminations(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available terminations for service with request: {}", request);
        return terminationRepository
                .findAvailableTerminationsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available termination groups for service, satisfying the following criteria:
     * <ul>
     *     <li>termination group is not already assigned to active service</li>
     *     <li>termination group is not already assigned to active product</li>
     *     <li>termination group name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available termination groups for service
     */
    public Page<AvailableServiceRelatedGroupEntityResponse> findAvailableTerminationGroups(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available termination groups for service with request: {}", request);
        return terminationGroupRepository
                .findAvailableTerminationGroupsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available penalties for service, satisfying the following criteria:
     * <ul>
     *     <li>penalty is not already assigned to active service</li>
     *     <li>penalty is not already assigned to active product</li>
     *     <li>penalty name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available penalties for service
     */
    public Page<AvailableServiceRelatedEntitiyResponse> findAvailablePenalties(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available penalties for service with request: {}", request);
        return penaltyRepository
                .findAvailablePenaltiesForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Returns a list of available penalty groups for service, satisfying the following criteria:
     * <ul>
     *     <li>penalty group is not already assigned to active service</li>
     *     <li>penalty group is not already assigned to active product</li>
     *     <li>penalty group name contains the prompt</li>
     * </ul>
     *
     * @param request the request containing the search criteria
     * @return page of available penalty groups for service
     */
    public Page<AvailableServiceRelatedGroupEntityResponse> findAvailablePenaltyGroups(AvailableServiceRelatedEntityRequest request) {
        log.debug("Fetching available penalty groups for service with request: {}", request);
        return penaltyGroupRepository
                .findAvailablePenaltyGroupsForService(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );
    }


    /**
     * Copies the service with the given id and version id, if present - latest version otherwise.
     *
     * @param id      service id to copy
     * @param version service version id to copy
     * @return the response containing the copied objects
     */
    @Transactional
    public ServiceCopyResponse copy(Long id, Long version) {
        EPService service = serviceRepository
                .findByIdAndStatusIn(id, List.of(ServiceStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Service not found by id"));

        ServiceDetails referenceDetails;
        if (version != null) {
            referenceDetails = serviceDetailsRepository
                    .findByServiceIdAndVersionAndStatusIn(id, version, List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("version-Service details not found for given version id %s;".formatted(version)));
        } else {
            referenceDetails = serviceDetailsRepository
                    .findFirstByServiceIdAndStatusInOrderByVersionDesc(id, List.of(ServiceDetailStatus.ACTIVE, ServiceDetailStatus.INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Service details not found for given id %s;".formatted(id)));
        }

        ServiceCopyResponse serviceResponse = serviceMapper.fromEntityToCopyResponse(service, referenceDetails);
        serviceResponse.getAdditionalSettings().setCollectionChannels(serviceDetailCollectionChannelsRepository.getByDetailId(referenceDetails.getId()));

        ServiceBasicSettingsResponse basicSettings = serviceResponse.getBasicSettings();
        Boolean globalSegment = referenceDetails.getGlobalSegment();
        if (globalSegment == null || !globalSegment) {
            basicSettings.setSegments(referenceDetails
                    .getSegments()
                    .stream()
                    .filter(serviceSegment -> serviceSegment.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                    .map(ServiceSegment::getSegment)
                    .filter(x -> x.getStatus().equals(ACTIVE))
                    .map(SegmentResponse::new).toList());
        }

        Boolean globalSalesArea = referenceDetails.getGlobalSalesArea();
        if (globalSalesArea == null || !globalSalesArea) {
            basicSettings.setSalesAreas(referenceDetails
                    .getSalesAreas()
                    .stream()
                    .filter(serviceSegment -> serviceSegment.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                    .map(ServiceSalesArea::getSalesArea)
                    .filter(x -> x.getStatus().equals(ACTIVE))
                    .map(SalesAreaResponse::new).toList());

        }

        Boolean globalSalesChannel = referenceDetails.getGlobalSalesChannel();
        if (globalSalesChannel == null || !globalSalesChannel) {
            basicSettings.setSalesChannels(referenceDetails
                    .getSalesChannels()
                    .stream()
                    .filter(serviceSegment -> serviceSegment.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                    .map(ServiceSalesChannel::getSalesChannel)
                    .filter(x -> x.getStatus().equals(ACTIVE))
                    .map(SalesChannelResponse::new)
                    .toList());

        }

        // if service is individual, name and name transliterated values should not be set
        if (StringUtils.isNotEmpty(service.getCustomerIdentifier())) {
            basicSettings.setName(null);
            basicSettings.setNameTransliterated(null);
        }

        serviceResponse.setServiceContractTerms(serviceMapper.createServiceContractTermsResponseForCopy(referenceDetails));
        if (BooleanUtils.isNotTrue(referenceDetails.getGlobalGridOperator())) {
            basicSettings.setGridOperators(serviceMapper.createGridOperatorResponse(referenceDetails, List.of(ACTIVE)));
        }
        serviceResponse.setRelatedEntities(createRelatedEntitiesShortResponse(referenceDetails.getLinkedProducts(), referenceDetails.getLinkedServices()));
        serviceResponse.setPenalties(copyPenalties(referenceDetails.getPenalties()));
        serviceResponse.setTerminations(copyTerminations(referenceDetails.getTerminations()));
        serviceResponse.setInterimAdvancePayments(copyInterimAdvancePayments(referenceDetails.getInterimAndAdvancePayments()));
        serviceResponse.setPriceComponents(copyPriceComponents(referenceDetails.getPriceComponents()));
        Terms terms = referenceDetails.getTerms();
        if (terms != null && terms.getStatus().equals(TermStatus.ACTIVE)) {
            Terms clonedTerm = termsService.copyTerms(terms);
            if (clonedTerm != null) {
                serviceResponse.setTerms(new TermsShortResponse(clonedTerm));
            }
        }

        serviceResponse.setPenaltyGroups(createPenaltyGroupResponse(referenceDetails));
        serviceResponse.setTerminationGroups(createTerminationGroupResponse(referenceDetails));
        serviceResponse.setTermGroup(createTermsGroupShortResponse(referenceDetails.getTermsGroups()));
        serviceResponse.setPriceComponentGroups(createPriceComponentGroupResponse(referenceDetails));
        serviceResponse.setInterimAdvancePaymentGroups(createIAPGroupShortResponse(referenceDetails.getInterimAndAdvancePaymentGroups()));
        serviceResponse.setTemplateResponses(serviceTemplateRepository.findForCopy(referenceDetails.getId(), LocalDate.now()));
        return serviceResponse;
    }

    private List<PriceComponentShortResponse> copyPriceComponents(List<ServicePriceComponent> priceComponents) {
        return priceComponentService.copyPriceComponentsForSubObjects(
                        priceComponents.stream()
                                .filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                                .map(ServicePriceComponent::getPriceComponent)
                                .toList()
                )
                .stream()
                .filter(Objects::nonNull)
                .map(PriceComponentShortResponse::new)
                .toList();
    }


    private List<InterimAdvancePaymentShortResponse> copyInterimAdvancePayments(List<ServiceInterimAndAdvancePayment> interimAndAdvancePayments) {
        return interimAdvancePaymentService
                .copyIAPForSubObjects(interimAndAdvancePayments
                        .stream()
                        .filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                        .map(ServiceInterimAndAdvancePayment::getInterimAndAdvancePayment)
                        .toList())
                .stream()
                .filter(Objects::nonNull)
                .map(InterimAdvancePaymentShortResponse::new)
                .toList();

    }


    private List<PenaltyShortResponse> copyPenalties(List<ServicePenalty> penalties) {
        List<Penalty> list = penalties.stream()
                .filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .map(ServicePenalty::getPenalty)
                .toList();
        return penaltyService.clonePenaltiesForSubObjects(list).stream().map(PenaltyShortResponse::new).toList();
    }

    private List<TerminationShortResponse> copyTerminations(List<ServiceTermination> serviceTerminations) {
        List<Termination> list = serviceTerminations.stream()
                .filter(x -> x.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .map(ServiceTermination::getTermination)
                .toList();
        return terminationsService.copyTerminationsForSubObjects(list).stream().map(TerminationShortResponse::new).toList();
    }


    @Override
    public CopyDomainWithVersion getGroupType() {
        return CopyDomainWithVersion.SERVICES;
    }

    @Override
    public Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request) {
        log.debug("Searching for services with request: {} for copying.", request);

        return serviceRepository.findByCopyGroupBaseRequest(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getFilter() != null && request.getFilter().equals(INDIVIDUAL_SERVICE) ? request.getFilter().getValue() : null,
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(Sort.Direction.DESC, "id")
                )
        );
    }

    @Override
    public List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long groupId) {
        return serviceDetailsRepository.findByCopyGroupBaseRequest(groupId);
    }

    public Page<ContractServiceFilterResponse> serviceContractList(ContractSaleListRequest request) {
        //TODO INDIVIDUAL SERVICE LOGIC SHOULD BE ADDED
        PageRequest page = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "name"));
        return serviceDetailsRepository.findContractSales(request.getServiceName(), request.getSegmentIds(), page);
    }


    /**
     * @return available service versions within the context of a service order, optionally filtered by the customer detail id and prompt.
     */
    public Page<ServiceOrderServiceVersionResponse> getVersionsForServiceOrder(Long customerDetailId, String prompt, Long id, int page, int size) {
        log.debug("Searching for available service versions for service order.");

        return serviceDetailsRepository
                .getAvailableVersionsForServiceOrders(
                        customerDetailId,
                        EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(prompt)),
                        id,
                        PageRequest.of(page, size)
                );
    }

    public Page<ServiceContractServiceVersionResponse> getAvailableVersionsForServiceContract(Long customerId, String prompt, int page, int size) {
        log.debug("Searching for available service versions for service contract.");

        Page<ServiceDetails> availableVersions = serviceDetailsRepository
                .getAvailableVersionsForServiceContract(
                        customerId,
                        EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(prompt)),
                        PageRequest.of(page, size)
                );
        return availableVersions.map(ServiceContractServiceVersionResponse::new);
    }


    /**
     * Fetches the service versions with the purpose of showing them in the service orders listing filter.
     *
     * @param prompt search prompt
     * @return list of service versions optionally filtered by prompt
     */
    public Page<ServiceVersionShortResponse> listServiceVersionsForServiceOrders(String prompt, int page, int size) {
        log.debug("Fetching service versions for listing filter.");

        return serviceDetailsRepository
                .getServiceVersionsForServiceOrdersListing(
                        EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(prompt)),
                        PageRequest.of(page, size)
                );
    }


    /**
     * Fetches the service versions with the purpose of showing them in the service contracts listing filter.
     *
     * @param prompt search prompt
     * @return list of service versions optionally filtered by prompt
     */
    public Page<ServiceVersionShortResponse> listServiceVersionsForServiceContracts(String prompt, int page, int size) {
        log.debug("Fetching service versions for listing filter.");

        return serviceDetailsRepository.getServiceVersionsForServiceContractsListing(
                EPBStringUtils.fromPromptToQueryParameter(StringUtil.underscoreReplacer(prompt)),
                PageRequest.of(page, size)
        );
    }

    private void createServiceAdditionalParams(ServiceDetails serviceDetails, List<ServiceAdditionalParamsRequest> additionalParamsRequests) {
        if (additionalParamsRequests == null) {
            additionalParamsRequests = new ArrayList<>();
        }
        List<ServiceAdditionalParams> serviceAdditionalParamList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Long orderId = (long) i;
            ServiceAdditionalParams serviceAdditionalParams = new ServiceAdditionalParams(
                    null,
                    orderId,
                    serviceDetails.getId(),
                    null,
                    null);

            additionalParamsRequests
                    .stream()
                    .filter(adp -> adp.orderingId().equals(orderId))
                    .findFirst()
                    .ifPresent(adPar -> {
                        serviceAdditionalParams.setLabel(adPar.label());
                        serviceAdditionalParams.setValue(adPar.value());
                    });
            serviceAdditionalParamList.add(serviceAdditionalParams);

        }
        serviceAdditionalParamsRepository.saveAllAndFlush(serviceAdditionalParamList);

    }

    private void updateLinkedAdditionalParams(BaseServiceRequest request, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return;
        }

        List<ServiceAdditionalParamsRequest> additionalParamsFromReq = request.getAdditionalSettings().getServiceAdditionalParams();
        if (additionalParamsFromReq == null) {
            additionalParamsFromReq = new ArrayList<>();
        }
        List<ServiceAdditionalParams> additionalParamsFromDb =
                serviceAdditionalParamsRepository.findServiceAdditionalParamsByServiceDetailId(serviceDetails.getId());

        for (int i = 0; i < 12; i++) {
            Long orderId = (long) i;
            Optional<ServiceAdditionalParams> paramFromDbOptional = additionalParamsFromDb
                    .stream()
                    .filter(db -> db.getOrderingId().equals(orderId))
                    .findFirst();
            if (paramFromDbOptional.isEmpty()) {
                return;
            }
            ServiceAdditionalParams paramFromDb = paramFromDbOptional.get();
            Optional<ServiceAdditionalParamsRequest> additionalParamsRequestOptional = additionalParamsFromReq
                    .stream()
                    .filter(adp -> adp.orderingId().equals(orderId))
                    .findFirst();
            if (additionalParamsRequestOptional.isPresent()) {
                ServiceAdditionalParamsRequest additionalParamsRequest = additionalParamsRequestOptional.get();
                paramFromDb.setLabel(additionalParamsRequest.label());
                paramFromDb.setValue(additionalParamsRequest.value());
                paramFromDb.setValue(additionalParamsRequest.value());
            } else {
                paramFromDb.setLabel(null);
                paramFromDb.setValue(null);
            }
            serviceAdditionalParamsRepository.save(paramFromDb);
        }
    }

    /**
     * Assigns the specified collection channels to the given service details.
     *
     * @param collectionChannelIds The IDs of the collection channels to assign.
     * @param serviceDetailId      The ID of the service details to assign the collection channels to.
     * @param errorMessages        A list to store any error messages that occur during the assignment process.
     */
    private void createConnectedCollectionChannels(List<Long> collectionChannelIds, Long serviceDetailId, List<String> errorMessages) {
        if (CollectionUtils.isNotEmpty(collectionChannelIds)) {
            log.debug("Assigning collection channels to service details");
            List<ServiceDetailCollectionChannels> serviceDetailCollectionChannelsList = new ArrayList<>();
            for (Long collectionChannelId : collectionChannelIds) {
                Optional<CollectionChannel> collectionChannel = collectionChannelRepository.findByIdAndStatuses(collectionChannelId, List.of(EntityStatus.ACTIVE));
                if (collectionChannel.isEmpty()) {
                    errorMessages.add("collectionChannelIds-Active Collection Channel with id: %s not found;".formatted(collectionChannelId));
                } else {
                    ServiceDetailCollectionChannels serviceDetailCollectionChannels = new ServiceDetailCollectionChannels();
                    serviceDetailCollectionChannels.setCollectionChannelId(collectionChannelId);
                    serviceDetailCollectionChannels.setServiceDetailsId(serviceDetailId);
                    serviceDetailCollectionChannels.setStatus(EntityStatus.ACTIVE);
                    serviceDetailCollectionChannelsList.add(serviceDetailCollectionChannels);
                }
            }
            serviceDetailCollectionChannelsRepository.saveAll(serviceDetailCollectionChannelsList);
        }
    }

    /**
     * Updates the connected collection channels for a service detail.
     *
     * @param collectionChannelIds The list of collection channel IDs to connect to the service detail.
     * @param serviceDetailId      The ID of the service detail.
     * @param errorMessages        A list to store any error messages that occur during the update.
     */
    private void updateConnectedCollectionChannels(List<Long> collectionChannelIds, Long serviceDetailId, List<String> errorMessages) {
        List<Long> oldList = serviceDetailCollectionChannelsRepository.getCollectionChannelIdsByServiceDetailId(serviceDetailId);
        List<Long> addedElementsFromList = EPBListUtils.getAddedElementsFromList(Objects.requireNonNullElse(oldList, new ArrayList<>()), Objects.requireNonNullElse(collectionChannelIds, new ArrayList<>()));
        if (CollectionUtils.isNotEmpty(addedElementsFromList)) {
            createConnectedCollectionChannels(addedElementsFromList, serviceDetailId, errorMessages);
        }
        List<Long> deletedElementsFromList = EPBListUtils.getDeletedElementsFromList(Objects.requireNonNullElse(oldList, new ArrayList<>()), Objects.requireNonNullElse(collectionChannelIds, new ArrayList<>()));
        if (CollectionUtils.isNotEmpty(deletedElementsFromList)) {
            serviceDetailCollectionChannelsRepository.deleteByCollectionChannelIdInAndServiceDetailsId(deletedElementsFromList, serviceDetailId);
        }
    }

    public void saveTemplates(Set<TemplateSubObjectRequest> templateRequests, Long productDetailId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateRequests)) {
            return;
        }
        Map<ProductServiceTemplateType, List<Long>> requestMap = new HashMap<>();

        for (TemplateSubObjectRequest templateRequest : templateRequests) {
            if (!requestMap.containsKey(templateRequest.getTemplateType())) {
                List<Long> value = new ArrayList<>();
                value.add(templateRequest.getTemplateId());
                requestMap.put(templateRequest.getTemplateType(), value);
            } else {
                requestMap.get(templateRequest.getTemplateType()).add(templateRequest.getTemplateId());
            }
        }

        List<ServiceTemplate> productContractTemplates = new ArrayList<>();
        createNewProductTemplates(productDetailId, errorMessages, requestMap, productContractTemplates);
        if (!errorMessages.isEmpty()) {
            return;
        }
        serviceTemplateRepository.saveAll(productContractTemplates);
    }

    private void createNewProductTemplates(Long serviceDetailId, List<String> errorMessages, Map<ProductServiceTemplateType, List<Long>> requestMap, List<ServiceTemplate> serviceContractTemplates) {
        AtomicInteger i = new AtomicInteger(0);
        requestMap.forEach((key, value) -> {
            if (key.equals(ProductServiceTemplateType.INVOICE_TEMPLATE)) {
                Long templateId = value.get(0);
                if (templateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.INVOICE, ContractTemplateType.DOCUMENT, LocalDate.now())) {
                    serviceContractTemplates.add(new ServiceTemplate(templateId, serviceDetailId, key));
                    return;
                }
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i.getAndIncrement(), templateId));
                return;
            } else if (key.equals(ProductServiceTemplateType.EMAIL_TEMPLATE)) {
                Long templateId = value.get(0);
                if (templateRepository.existsByIdAndTemplatePurposeAndTemplateType(templateId, ContractTemplatePurposes.INVOICE, ContractTemplateType.EMAIL, LocalDate.now())) {
                    serviceContractTemplates.add(new ServiceTemplate(templateId, serviceDetailId, key));
                    return;
                }
                errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i.getAndIncrement(), templateId));

                return;
            }
            Set<Long> allIdByIdAndLanguages = templateRepository.findAllIdByIdAndLanguages(value, key.getPurpose(ContractTemplatePurposes.SERVICE), key.getLanguage(), key.getTemplateTypes(), ContractTemplateStatus.ACTIVE, LocalDate.now());
            for (Long l : value) {
                if (!allIdByIdAndLanguages.contains(l)) {
                    errorMessages.add("templateIds[%s]-Template with id %s was not found or has wrong purpose;".formatted(i.getAndIncrement(), l));
                    continue;
                }
                serviceContractTemplates.add(new ServiceTemplate(l, serviceDetailId, key));
            }
        });
    }

    public void updateTemplates(Set<TemplateSubObjectRequest> templateIds, Long serviceDetailId, List<String> errorMessages) {
        if (CollectionUtils.isEmpty(templateIds)) {
            return;
        }
        Map<ProductServiceTemplateType, List<Long>> templatesToCreate = new HashMap<>();
        Map<Pair<Long, ProductServiceTemplateType>, ServiceTemplate> templateMap = serviceTemplateRepository.findByServiceDetailId(serviceDetailId).stream().collect(Collectors.toMap(x -> Pair.of(x.getTemplateId(), x.getType()), j -> j));
        List<ServiceTemplate> productContractTemplates = new ArrayList<>();
        for (TemplateSubObjectRequest templateRequest : templateIds) {
            ServiceTemplate remove = templateMap.remove(Pair.of(templateRequest.getTemplateId(), templateRequest.getTemplateType()));
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
        createNewProductTemplates(serviceDetailId, errorMessages, templatesToCreate, productContractTemplates);
        Collection<ServiceTemplate> values = templateMap.values();
        values.forEach(x -> {
            x.setStatus(EntityStatus.DELETED);
            productContractTemplates.add(x);
        });
        if (!errorMessages.isEmpty()) {
            return;
        }
        serviceTemplateRepository.saveAll(productContractTemplates);
    }


    public List<ProductServiceTemplateShortResponse> findTemplates(Long serviceDetailId) {
        return serviceTemplateRepository.findForContract(serviceDetailId, LocalDate.now());
    }

}


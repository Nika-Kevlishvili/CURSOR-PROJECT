package bg.energo.phoenix.service.pod.pod;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.address.Country;
import bg.energo.phoenix.model.entity.nomenclature.address.Municipality;
import bg.energo.phoenix.model.entity.nomenclature.address.PopulatedPlace;
import bg.energo.phoenix.model.entity.nomenclature.address.ZipCode;
import bg.energo.phoenix.model.entity.nomenclature.customer.legalForm.LegalForm;
import bg.energo.phoenix.model.entity.nomenclature.pod.MeasurementType;
import bg.energo.phoenix.model.entity.nomenclature.pod.PodAdditionalParameters;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.pod.pod.PodContractResponse;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetailsAdditionalParameters;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PODDisconnectionPowerSupply;
import bg.energo.phoenix.model.enums.pod.pod.PODMeasurementType;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.pod.pod.*;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponse;
import bg.energo.phoenix.model.response.contract.pods.ContractPodsResponseImpl;
import bg.energo.phoenix.model.response.nomenclature.address.*;
import bg.energo.phoenix.model.response.nomenclature.pod.PodViewMeasurementType;
import bg.energo.phoenix.model.response.pod.pod.*;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.address.*;
import bg.energo.phoenix.repository.nomenclature.customer.legalForm.LegalFormRepository;
import bg.energo.phoenix.repository.nomenclature.pod.BalancingGroupCoordinatorsRepository;
import bg.energo.phoenix.repository.nomenclature.pod.MeasurementTypeRepository;
import bg.energo.phoenix.repository.nomenclature.pod.PodAdditionalParametersRepository;
import bg.energo.phoenix.repository.nomenclature.pod.UserTypeRepository;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsAdditionalParametersRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.ACTIVE;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.INACTIVE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointOfDeliveryService {

    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final PermissionService permissionService;
    private final CountryRepository countryRepository;
    private final BalancingGroupCoordinatorsRepository balancingGroupCoordinatorsRepository;
    private final StreetRepository streetRepository;
    private final ResidentialAreaRepository residentialAreaRepository;
    private final DistrictRepository districtRepository;
    private final PopulatedPlaceRepository populatedPlaceRepository;
    private final ZipCodeRepository zipCodeRepository;
    private final LegalFormRepository legalFormRepository;
    private final MeasurementTypeRepository measurementTypeRepository;
    private final UserTypeRepository userTypeRepository;
    private final PodAdditionalParametersRepository podAdditionalParametersRepository;
    private final PointOfDeliveryDetailsAdditionalParametersRepository pointOfDeliveryDetailsAdditionalParametersRepository;

    private static void fillLocalAddressData(PointOfDeliveryDetails details, PODLocalAddressData localAddressData) {
        details.setCountryId(localAddressData.getCountryId());

        details.setPopulatedPlaceId(localAddressData.getPopulatedPlaceId());

        details.setZipCodeId(localAddressData.getZipCodeId());

        details.setDistrictId(localAddressData.getDistrictId());

        details.setResidentialAreaId(localAddressData.getResidentialAreaId());

        details.setStreetId(localAddressData.getStreetId());

        details.setForeignStreetType(localAddressData.getStreetType());

        details.setForeignResidentialAreaType(localAddressData.getResidentialAreaType());
    }

    private static void fillForeignAddressData(PointOfDeliveryDetails details, PODForeignAddressData addressData) {
        details.setCountryId(addressData.getCountryId());
        details.setRegionForeign(addressData.getRegion());
        details.setMunicipalityForeign(addressData.getMunicipality());
        details.setPopulatedPlaceForeign(addressData.getPopulatedPlace());
        details.setZipCodeForeign(addressData.getZipCode());
        details.setDistrictForeign(addressData.getDistrict());
        details.setForeignResidentialAreaType(addressData.getResidentialAreaType());
        details.setResidentialAreaForeign(addressData.getResidentialArea());
        details.setForeignStreetType(addressData.getStreetType());
        details.setStreetForeign(addressData.getStreet());
    }

    @Transactional
    public PodResponse create(PodCreateRequest request, List<String> permissions) {
        log.debug("Creating Point of Delivery with request: {};", request);
        List<String> exceptionMessages = new ArrayList<>();

        checkRequestPermissions(request, permissions);
        if (pointOfDeliveryRepository.existsByIdentifierIgnoreCaseAndStatusIn(request.getIdentifier(), List.of(PodStatus.ACTIVE))) {
            exceptionMessages.add("identifier-Point of delivery exists with this identifier;");
        }

        if (request.getSlp()) {
            Optional<MeasurementType> measurementType = measurementTypeRepository.findByIdAndStatus(request.getMeasurementTypeId(), ACTIVE);
            if (measurementType.isEmpty()) {
                exceptionMessages.add("measurementTypeId-MeasurementType with ACTIVE status not found!;");
            } else if (!Objects.equals(request.getGridOperatorId(), measurementType.get().getGridOperatorId())) {
                exceptionMessages.add("measurementTypeId-GridOperator and MeasurementType not matched!;");
            }
        }

        Optional<GridOperator> operator = gridOperatorRepository.findByIdAndStatus(request.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE));
        validateGridOperator(operator, request.getIdentifier(), exceptionMessages);
        PointOfDelivery pointOfDelivery = pointOfDeliveryRepository.save(PointOfDeliveryMapper.mapCreateRequestToPod(request));
        PointOfDeliveryDetails details = PointOfDeliveryMapper.mapCreateRequestToPodDetails(request, pointOfDelivery.getId());
        processCustomer(request, exceptionMessages, details);
        processAddress(request, details, exceptionMessages);
        processBalancingGroupCoordinator(request, List.of(ACTIVE), exceptionMessages);
        processUserType(request, List.of(ACTIVE), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        PointOfDeliveryDetails savedDetails = pointOfDeliveryDetailsRepository.saveAndFlush(details);
        processPodAdditionalParams(request.getPodAdditionalParameters(), savedDetails, permissions, exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        pointOfDelivery.setLastPodDetailId(savedDetails.getId());
        pointOfDeliveryRepository.saveAndFlush(pointOfDelivery);
        return new PodResponse(pointOfDelivery.getId(), details.getId(), details.getVersionId());
    }

    private void processPodAdditionalParams(
            Set<Long> podAdditionalParameters,
            PointOfDeliveryDetails pointOfDeliveryDetails,
            List<String> permissions,
            List<String> errorMessages
    ) {
        if (haveAdditionalParamsPermission(permissions)) {
            if (CollectionUtils.isNotEmpty(podAdditionalParameters)) {
                podAdditionalParameters.forEach(id -> {
                            Optional<PodAdditionalParameters> podAdditionalParameter = podAdditionalParametersRepository.findByIdAndStatus(id, List.of(ACTIVE));
                            if (podAdditionalParameter.isPresent()) {
                                savePointOfDeliveryAdditionalParameter(pointOfDeliveryDetails, podAdditionalParameter.get());
                            } else {
                                errorMessages.add("podAdditionalParameters-[podAdditionalParameters] active podAdditionalParameter with id: %s can't be found;".formatted(id));
                            }
                        }
                );
            }
        } else {
            if (CollectionUtils.isNotEmpty(podAdditionalParameters)) {
                throw new ClientException("You dont have permission to add pod additional params;", ErrorCode.ACCESS_DENIED);
            } else {
                Optional<PodAdditionalParameters> defaultPodAdditionalParameter = podAdditionalParametersRepository.findDefaultSelection();
                defaultPodAdditionalParameter.ifPresent(
                        ap -> savePointOfDeliveryAdditionalParameter(pointOfDeliveryDetails, ap)
                );
            }
        }
    }

    private void processEditPodAdditionalParams(
            Set<Long> podAdditionalParameters,
            PointOfDeliveryDetails pointOfDeliveryDetails,
            List<String> permissions,
            List<String> errorMessages
    ) {
        if (haveAdditionalParamsPermission(permissions)) {
            Map<Long, PointOfDeliveryDetailsAdditionalParameters> existingMap = EPBListUtils.transformToMap(
                    pointOfDeliveryDetailsAdditionalParametersRepository.findAllByPodDetailIdAndStatus(pointOfDeliveryDetails.getId(), EntityStatus.ACTIVE),
                    PointOfDeliveryDetailsAdditionalParameters::getPodAdditionalParamId
            );

            if (CollectionUtils.isNotEmpty(podAdditionalParameters)) {
                Set<Long> added = podAdditionalParameters
                        .stream()
                        .filter(current -> !existingMap.containsKey(current))
                        .collect(Collectors.toSet());

                processPodAdditionalParams(added, pointOfDeliveryDetails, permissions, errorMessages);
                validateAndDeletePodAdditionalParams(existingMap, podAdditionalParameters);
            } else {
                validateAndDeletePodAdditionalParams(existingMap, SetUtils.emptySet());
            }
        }
    }

    private void validateAndDeletePodAdditionalParams(Map<Long, PointOfDeliveryDetailsAdditionalParameters> existingMap, Set<Long> podAdditionalParameters) {
        List<PointOfDeliveryDetailsAdditionalParameters> deleted = existingMap
                .entrySet()
                .stream()
                .filter(entry -> !podAdditionalParameters.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .peek(currentMapping -> currentMapping.setStatus(EntityStatus.DELETED))
                .toList();

        if (CollectionUtils.isNotEmpty(deleted)) {
            pointOfDeliveryDetailsAdditionalParametersRepository.saveAllAndFlush(deleted);
        }
    }

    private void savePointOfDeliveryAdditionalParameter(PointOfDeliveryDetails pointOfDeliveryDetails,
                                                        PodAdditionalParameters podAdditionalParameter
    ) {
        PointOfDeliveryDetailsAdditionalParameters mapping = new PointOfDeliveryDetailsAdditionalParameters();
        mapping.setPodAdditionalParamId(podAdditionalParameter.getId());
        mapping.setPodDetailId(pointOfDeliveryDetails.getId());
        mapping.setStatus(EntityStatus.ACTIVE);
        pointOfDeliveryDetailsAdditionalParametersRepository.save(mapping);
    }

    private boolean haveAdditionalParamsPermission(List<String> permissions) {
        if (CollectionUtils.isNotEmpty(permissions)) {
            return permissions.contains(PermissionEnum.POD_EDIT_ADDITIONAL_PARAMS.getId());
        } else {
            List<String> customerContext = permissionService.getPermissionsFromContext(PermissionContextEnum.POD);
            return customerContext.contains(PermissionEnum.POD_EDIT_ADDITIONAL_PARAMS.getId());
        }
    }

    private void processBalancingGroupCoordinator(PodBaseRequest request, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        if (request.getBalancingGroupCoordinatorId() != null) {
            if (!balancingGroupCoordinatorsRepository.existsByIdAndStatusIn(request.getBalancingGroupCoordinatorId(), statuses)) {
                log.error("balancingGroupCoordinatorId-Balancing group coordinator not found in statuses %s;".formatted(statuses));
                exceptionMessages.add("balancingGroupCoordinatorId-Balancing group coordinator not found in statuses %s;".formatted(statuses));
            }
        }
    }

    private void processUserType(PodBaseRequest request, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        if (request.getUserTypeId() != null) {
            if (!userTypeRepository.existsByIdAndStatusIn(request.getUserTypeId(), statuses)) {
                log.error("userTypeId-User type not found in statuses %s;".formatted(statuses));
                exceptionMessages.add("userTypeId-User type not found in statuses %s;".formatted(statuses));
            }
        }
    }

    private void processCustomer(PodCreateRequest request, List<String> exceptionMessages, PointOfDeliveryDetails details) {
        if (request.getCustomerIdentifier() != null) {
            Optional<Customer> customer = customerRepository.findByIdentifierAndStatus(request.getCustomerIdentifier(), CustomerStatus.ACTIVE);
            if (customer.isEmpty()) {
                exceptionMessages.add("customerIdentifier-customer not found!;");
            } else {
                details.setCustomerId(customer.get().getId());
            }
        }
    }

    private void processAddress(PodCreateRequest request, PointOfDeliveryDetails details, List<String> exceptionMessages) {
        PodAddressRequest addressRequest = request.getAddressRequest();
        if (Boolean.TRUE.equals(addressRequest.getForeign())) {
            assignForeignAddressData(details, addressRequest.getForeignAddressData(), exceptionMessages);
        } else {
            assignLocalAddressData(details, addressRequest.getLocalAddressData(), exceptionMessages);
        }
    }

    public PointOfDeliveryResponse view(Long id, Integer versionId) {
        PointOfDelivery pointOfDelivery = pointOfDeliveryRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id - Point of delivery not found!;"));
        PointOfDeliveryDetails details;
        if (versionId != null) {
            details = pointOfDeliveryDetailsRepository.findByPodIdAndVersionId(id, versionId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Point of delivery do not have details;"));
        } else {
            details = pointOfDeliveryDetailsRepository.findById(pointOfDelivery.getLastPodDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Point of delivery do not have details;"));
        }

        PointOfDeliveryResponse response = PointOfDeliveryMapper.createPointOfDeliveryResponse(pointOfDelivery, details);
        GridOperator gridOperator = gridOperatorRepository.findByIdAndStatus(pointOfDelivery.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id - Grid operator not found;"));
        response.setGridOperatorId(gridOperator.getId());
        response.setGridOperatorName(gridOperator.getName());

        if (details.getMeasurementType().equals(PODMeasurementType.SLP)) {
            /*MeasurementType measurementType = measurementTypeRepository.findByIdAndStatusIn(details.getPodMeasurementTypeId(), List.of(ACTIVE, INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id - MeasurementType not found;"));*/
            Optional<MeasurementType> measurementTypeOptional = measurementTypeRepository.findByIdAndStatusIn(details.getPodMeasurementTypeId(), List.of(ACTIVE, INACTIVE));
            if (measurementTypeOptional.isPresent()) {
                MeasurementType measurementType = measurementTypeOptional.get();
                PodViewMeasurementType podViewMeasurementType = new PodViewMeasurementType();
                podViewMeasurementType.setMeasurementTypeId(details.getPodMeasurementTypeId());
                podViewMeasurementType.setMeasurementTypeName(measurementType.getName());
                response.setPodViewMeasurementType(podViewMeasurementType);
            }
        }

        if (details.getBalancingGroupCoordinatorId() != null) {
            balancingGroupCoordinatorsRepository.findByIdAndStatus(details.getBalancingGroupCoordinatorId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .ifPresent(bgc -> {
                        response.setBalancingGroupCoordinatorId(bgc.getId());
                        response.setBalancingGroupCoordinatorName(bgc.getName());
                    });
        }

        if (details.getUserTypeId() != null) {
            userTypeRepository.findByIdAndStatusIn(details.getUserTypeId(), List.of(ACTIVE, INACTIVE))
                    .ifPresent(ut -> {
                        response.setUserTypeId(ut.getId());
                        response.setUserTypeName(ut.getName());
                    });
        }

        if (details.getCustomerId() != null) {
            Customer customer = customerRepository.findByIdAndStatuses(details.getCustomerId(), List.of(CustomerStatus.ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer not found;"));
            CustomerDetails customerDetails = customerDetailsRepository.findFirstByCustomerId(details.getCustomerId(), Sort.by(Sort.Direction.DESC, "versionId"))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer not found;"));
            response.setCustomerId(customerDetails.getCustomerId());

            if (customerDetails.getLegalFormId() != null) {
                LegalForm legalForm = legalFormRepository.findByIdAndStatus(customerDetails.getLegalFormId(), List.of(ACTIVE, INACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("legalFormId-Legal form not found!;"));
                response.setCustomerName(customerDetails.getName() + " " + legalForm.getName());
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(customerDetails.getName());
                if (customerDetails.getMiddleName() != null) {
                    sb.append(" ");
                    sb.append(customerDetails.getMiddleName());
                }
                if (customerDetails.getLastName() != null) {
                    sb.append(" ");
                    sb.append(customerDetails.getLastName());
                }
                response.setCustomerName(sb.toString());
            }
            response.setCustomerIdentifier(customer.getIdentifier());
        }
        attachAddressResponse(details, response);

        List<PointOfDeliveryDetails> podDetails = pointOfDeliveryDetailsRepository.findAllByPodIdOrderByVersionIdAsc(id);
        List<PointOfDeliveryVersionResponse> versionResponses = new ArrayList<>();
        for (PointOfDeliveryDetails podDetail : podDetails) {
            versionResponses.add(new PointOfDeliveryVersionResponse(podDetail));
        }
        response.setVersions(versionResponses);
        response.setLocked(isAbleToEditCurrentVersionForView(details));
        response.setPointOfDeliveryAdditionalParamsShortResponse(getPointOfDeliveryAdditionalParamsShortResponse(details.getId()));
        return response;
    }

    private List<ShortResponse> getPointOfDeliveryAdditionalParamsShortResponse(Long podDetailId) {
        return pointOfDeliveryDetailsAdditionalParametersRepository.findAllAdditionalParametersByPodDetailId(podDetailId);
    }

    @Transactional
    public PodResponse edit(Long id, PodUpdateRequest request, List<String> permissions) {
        log.debug("Editing Point of Delivery with id: {};", id);

        PointOfDelivery pointOfDelivery = pointOfDeliveryRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Point of delivery not found by ID %s;".formatted(id)));

        PointOfDeliveryDetails details;
        if (request.getVersionId() != null) {
            details = pointOfDeliveryDetailsRepository
                    .findByPodIdAndVersionId(id, request.getVersionId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Requested point of delivery version not found;"));
        } else {
            details = pointOfDeliveryDetailsRepository
                    .findById(pointOfDelivery.getLastPodDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Latest point of delivery version not found;"));
        }
        if (!Objects.equals(pointOfDelivery.getImpossibleToDisconnect(), request.isImpossibleToDisconnect())) {
            if (!(permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_IMPOSSIBLE_DISCONNECT)) || permissions.contains(PermissionEnum.POD_MI_IMPOSSIBLE_DISCONNECT.getId()))) {
                throw new ClientException("impossibleToDisconnect-You need impossibleToDisconnect permission to update value!;", ErrorCode.ACCESS_DENIED);
            }
            pointOfDelivery.setImpossibleToDisconnect(request.isImpossibleToDisconnect());
        }
        if (!(permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_EDIT)) ||
                permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_EDIT_LOCKED)) ||
                permissions.contains(PermissionEnum.POD_MI_UPDATE.getId()))) {
            return new PodResponse(pointOfDelivery.getId(), details.getId(), details.getVersionId());
        }

        if (isEditingBilling(pointOfDelivery, request)) {
            validateBillingEditing(permissions);
            PointOfDeliveryMapper.addBilling(request, pointOfDelivery);
            pointOfDelivery.setBlockedBilling(request.isBlockedBilling());
        }

        if (isEditingDisconnection(pointOfDelivery, request)) {
            validateDisconnectionEditing(permissions);
            pointOfDelivery.setBlockedDisconnection(request.isBlockedDisconnection());
            PointOfDeliveryMapper.addDisconnection(request, pointOfDelivery);
        }


        List<String> exceptionMessages = new ArrayList<>();
        processBalancingGroupCoordinator(request, getNomenclatureStatuses(details.getBalancingGroupCoordinatorId(), request.getBalancingGroupCoordinatorId()), exceptionMessages);
        processUserType(request, getNomenclatureStatuses(details.getUserTypeId(), request.getUserTypeId()), exceptionMessages);

        PointOfDeliveryDetails detailsUpdating;
        if (request.isUpdateExistingVersion()) {
            isAbleToEditCurrentVersion(details);
            detailsUpdating = details;
            PointOfDeliveryMapper.updatePointOfDeliveryDetails(request, detailsUpdating, details.getVersionId(), details.getPodId());
        } else {
            detailsUpdating = new PointOfDeliveryDetails();
            detailsUpdating.setPodId(pointOfDelivery.getId());
            PointOfDeliveryMapper.updatePointOfDeliveryDetails(request, detailsUpdating, getAndIncrementLatestVersion(pointOfDelivery.getId(), exceptionMessages), details.getPodId());
        }

        updateAddressData(details, detailsUpdating, request, exceptionMessages);

        String customerIdentifier = request.getCustomerIdentifier();
        if (customerIdentifier != null) {
            Optional<Customer> customer = customerRepository.findByIdentifierAndStatus(customerIdentifier, CustomerStatus.ACTIVE);
            if (customer.isEmpty()) {
                exceptionMessages.add("customerIdentifier-customer not found!;");
            } else {
                detailsUpdating.setCustomerId(customer.get().getId());
            }
        } else {
            detailsUpdating.setCustomerId(null);
        }

        detailsUpdating.setUserTypeId(request.getUserTypeId());
        detailsUpdating.setBalancingGroupCoordinatorId(request.getBalancingGroupCoordinatorId());
        pointOfDeliveryDetailsRepository.saveAndFlush(detailsUpdating);

        processEditPodAdditionalParams(request.getPodAdditionalParameters(), detailsUpdating, permissions, exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        if (request.isImpossibleToDisconnect() != pointOfDelivery.getImpossibleToDisconnect()) {
            validateImpossibleDisconnect(permissions);
            pointOfDelivery.setImpossibleToDisconnect(request.isImpossibleToDisconnect());
        }

        if (!request.isUpdateExistingVersion()) {
            pointOfDelivery.setLastPodDetailId(detailsUpdating.getId());
        }

        if (request.getSlp()) {
            Optional<MeasurementType> measurementType = measurementTypeRepository.findByIdAndStatus(request.getMeasurementTypeId(), ACTIVE);
            if (measurementType.isEmpty()) {
                exceptionMessages.add("measurementTypeId-MeasurementType with ACTIVE status not found!;");
            } else if (!Objects.equals(pointOfDelivery.getGridOperatorId(), measurementType.get().getGridOperatorId())) {
                exceptionMessages.add("measurementTypeId-GridOperator and MeasurementType not matched!;");
            }
        }

        pointOfDeliveryRepository.save(pointOfDelivery);

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        return new PodResponse(pointOfDelivery.getId(), detailsUpdating.getId(), detailsUpdating.getVersionId());
    }

    /**
     * Retrieves a list of Point of Delivery filtered with the search criteria.
     *
     * @param podSearchRequest Search criteria
     * @return Page of Point of Delivery
     */
    public Page<PointOfDeliveryFilterResponse> list(PodSearchRequest podSearchRequest) {
        log.debug("Searching for Point of Delivery with request: {};", podSearchRequest);
        Boolean disconnectionPowerSupply = convertDisconnectionEnum(podSearchRequest.getDisconnection());

        return pointOfDeliveryRepository.list(
                EPBStringUtils.fromPromptToQueryParameter(podSearchRequest.getPrompt()),
                podSearchRequest.getSearchFields() == null ? null : podSearchRequest.getSearchFields().getColumn(),
                CollectionUtils.isEmpty(podSearchRequest.getPodTypes()) ? null : podSearchRequest.getPodTypes(),
                CollectionUtils.isEmpty(podSearchRequest.getGridOperatorIds()) ? null : podSearchRequest.getGridOperatorIds(),
                CollectionUtils.isEmpty(podSearchRequest.getConsumptionPurposes()) ? null : podSearchRequest.getConsumptionPurposes(),
                CollectionUtils.isEmpty(podSearchRequest.getVoltageLevels()) ? null : podSearchRequest.getVoltageLevels(),
                CollectionUtils.isEmpty(podSearchRequest.getMeasurementTypes()) ? null : podSearchRequest.getMeasurementTypes(),
                podSearchRequest.getProvidedPowerFrom(),
                podSearchRequest.getProvidedPowerTo(),
                String.valueOf(podSearchRequest.isExcludeOldVersions()),
                getSearchStatuses(),
                disconnectionPowerSupply,
                PageRequest.of(
                        podSearchRequest.getPage(),
                        podSearchRequest.getSize(),
                        Sort.by(
                                podSearchRequest.getSortDirection(),
                                podSearchRequest.getSortBy().getColumn()
                        )
                )
        );

    }

    /**
     * Sets deleted status to Point of Delivery if the following conditions are met:
     * <ul>
     *     <li>POD is not already deleted</li>
     *     <li>POD is not connected to Discount</li>
     *     <li>POD is not connected to Meter</li>
     *     <li>POD is not connected to Billing Data by Profile</li>
     *     <li>POD is not connected to Billing Data by Scales</li>
     * </ul>
     *
     * @param id ID of Point of Delivery that should be deleted
     */
    @Transactional
    public Long delete(Long id) {
        PointOfDelivery pod = pointOfDeliveryRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Point of delivery not found!;"));

        if (pod.getStatus().equals(PodStatus.DELETED)) {
            log.error("id-Point of delivery with ID %s is already deleted;".formatted(id));
            throw new OperationNotAllowedException("id-Point of delivery with ID %s is already deleted;".formatted(id));
        }

        if (pointOfDeliveryRepository.hasActiveConnectionToDiscount(id)) {
            log.error("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Discount;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Discount;".formatted(id));
        }

        if (pointOfDeliveryRepository.hasActiveConnectionToMeter(id)) {
            log.error("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Meter;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Meter;".formatted(id));
        }

        if (pointOfDeliveryRepository.hasActiveConnectionToBillingDataByProfile(id)) {
            log.error("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Billing Data by Profile;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Billing Data by Profile;".formatted(id));
        }

        if (pointOfDeliveryRepository.hasActiveConnectionToBillingDataByScales(id)) {
            log.error("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Billing Data by Scales;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Billing Data by Scales;".formatted(id));
        }

        if (pointOfDeliveryRepository.hasActiveConnectionToProductContract(id)) {
            log.error("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Product Contract;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Product Contract;".formatted(id));
        }

        if (pointOfDeliveryRepository.hasActiveConnectionToServiceContract(id)) {
            log.error("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Service Contract;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Service Contract;".formatted(id));
        }

        if (pointOfDeliveryRepository.hasActiveConnectionToServiceOrder(id)) {
            log.error("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Service Order;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Service Order;".formatted(id));
        }

        if (pointOfDeliveryRepository.hasActiveConnectionToAction(id)) {
            log.error("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Action;".formatted(id));
            throw new OperationNotAllowedException("id-You can’t delete the Point of Delivery with ID %s because it is connected to the Action;".formatted(id));
        }

        pod.setStatus(PodStatus.DELETED);
        pointOfDeliveryRepository.save(pod);
        return pod.getId();
    }

    private Integer getAndIncrementLatestVersion(Long podId, List<String> exceptionMessages) {
        Optional<Integer> maxVersionId = pointOfDeliveryDetailsRepository.findMaxVersionId(podId);
        if (maxVersionId.isPresent()) {
            return maxVersionId.get() + 1;
        } else {
            exceptionMessages.add("id-Can not find max versionId for pod;");
            return null;
        }
    }

    private Integer getAndIncrementLatestVersion(Long podId) {
        Optional<Integer> maxVersionId = pointOfDeliveryDetailsRepository.findMaxVersionId(podId);
        if (maxVersionId.isPresent()) {
            return maxVersionId.get() + 1;
        } else {
            throw new DomainEntityNotFoundException("id-Can not find max versionId for pod;");
        }
    }

    private List<PodStatus> getSearchStatuses() {
        List<PodStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_VIEW_BASIC))) {
            statuses.add(PodStatus.ACTIVE);
        }
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_VIEW_DELETED))) {
            statuses.add(PodStatus.DELETED);
        }
        return statuses;
    }

    private boolean isEditingBilling(PointOfDelivery pod, PodUpdateRequest request) {
        BlockedBillingRequest blockedBillingRequest = request.getBlockedBillingRequest();
        if (Boolean.TRUE.equals(pod.getBlockedBilling()) != request.isBlockedBilling()) {
            return true;
        }
        if (!request.isBlockedBilling()) {
            return false;
        }
        if (!Objects.equals(pod.getBlockedBillingReason(), blockedBillingRequest.getReason())) {
            return true;
        }
        if (!Objects.equals(pod.getBlockedBillingInfo(), blockedBillingRequest.getAdditionalInfo())) {
            return true;
        }
        if (!Objects.equals(pod.getBlockedBillingDateFrom(), blockedBillingRequest.getFrom())) {
            return true;
        }
        return !Objects.equals(pod.getBlockedBillingDateTo(), blockedBillingRequest.getTo());
    }

    private boolean isEditingDisconnection(PointOfDelivery pod, PodUpdateRequest request) {
        BlockedDisconnectionRequest disconnectionRequest = request.getBlockedDisconnectionRequest();
        if (Boolean.TRUE.equals(pod.getBlockedDisconnection()) != request.isBlockedDisconnection()) {
            return true;
        }
        if (!request.isBlockedDisconnection()) {
            return false;
        }
        if (!Objects.equals(pod.getBlockedDisconnectionReason(), disconnectionRequest.getReason())) {
            return true;
        }
        if (!Objects.equals(pod.getBlockedDisconnectionReason(), disconnectionRequest.getAdditionalInfo())) {
            return true;
        }
        if (!Objects.equals(pod.getBlockedDisconnectionDateFrom(), disconnectionRequest.getFrom())) {
            return true;
        }
        return !Objects.equals(pod.getBlockedDisconnectionDateTo(), disconnectionRequest.getTo());
    }

    private void validateImpossibleDisconnect(List<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_IMPOSSIBLE_DISCONNECT))) {
                throw new ClientException("You need Impossible disconnection permission!;", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissions.contains(PermissionEnum.POD_MI_IMPOSSIBLE_DISCONNECT.getId())) {
                throw new ClientException("You need Impossible disconnection permission!;", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void validateDisconnectionEditing(List<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_BLOCK_DISCONNECTION))) {
                throw new ClientException("You need block disconnection permission!;", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissions.contains(PermissionEnum.POD_MI_DISCONNECTION.getId())) {
                throw new ClientException("You need block disconnection permission!;", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void validateBillingEditing(List<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_BLOCK_BILLING))) {
                throw new ClientException("You need block billing permission!;", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (!permissions.contains(PermissionEnum.POD_MI_BILLING.getId())) {
                throw new ClientException("You need block billing permission!;", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void updateAddressData(PointOfDeliveryDetails details, PointOfDeliveryDetails detailsUpdating, PodUpdateRequest request, List<String> exceptionMessages) {
        PodAddressRequest addressRequest = request.getAddressRequest();
        if (Boolean.TRUE.equals(addressRequest.getForeign())) {
            updateForeignData(details, detailsUpdating, addressRequest.getForeignAddressData(), exceptionMessages);
        } else {
            updateLocalAddressData(details, detailsUpdating, addressRequest.getLocalAddressData(), exceptionMessages);
        }
    }

    private void updateLocalAddressData(PointOfDeliveryDetails details, PointOfDeliveryDetails detailsUpdating, PODLocalAddressData localAddressData, List<String> exceptionMessages) {
        Long populatedPlaceId = localAddressData.getPopulatedPlaceId();
        if (!countryRepository.existsByIdAndStatusIn(localAddressData.getCountryId(), getNomenclatureStatuses(details.getCountryId(), localAddressData.getCountryId()))) {
            exceptionMessages.add("addressRequest.localAddressData.countryId-Country do not exists;");
        }
        if (!populatedPlaceRepository.existsByIdAndMunicipalityRegionCountryId(populatedPlaceId, localAddressData.getCountryId(), getNomenclatureStatuses(details.getPopulatedPlaceId(), localAddressData.getPopulatedPlaceId()))) {
            exceptionMessages.add("addressRequest.localAddressData.populatedPlaceId-Populated place do not exists or Populated place has inactive parent object;");
        }
        if (!zipCodeRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getZipCodeId(), populatedPlaceId, getNomenclatureStatuses(details.getZipCodeId(), localAddressData.getZipCodeId()))) {
            exceptionMessages.add("addressRequest.localAddressData.zipCodeId-ZipCode do not exists;");
        }
        if (localAddressData.getDistrictId() != null) {
            if (!districtRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getDistrictId(), populatedPlaceId, getNomenclatureStatuses(details.getDistrictId(), localAddressData.getDistrictId()))) {
                exceptionMessages.add("addressRequest.localAddressData.districtId-District do not exists;");
            }
        }
        if (localAddressData.getResidentialAreaId() != null) {
            if (!residentialAreaRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getResidentialAreaId(), populatedPlaceId, getNomenclatureStatuses(details.getResidentialAreaId(), localAddressData.getResidentialAreaId()))) {
                exceptionMessages.add("addressRequest.localAddressData.residentialAreaId-Residential Area do not exists;");
            }
        }
        if (localAddressData.getStreetId() != null) {
            if (!streetRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getStreetId(), populatedPlaceId, getNomenclatureStatuses(details.getStreetId(), localAddressData.getStreetId()))) {
                exceptionMessages.add("addressRequest.localAddressData.streetId-Street do not exists;");
            }
        }
        if (localAddressData.getZipCodeId() != null) {
            if (!zipCodeRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getZipCodeId(), populatedPlaceId, getNomenclatureStatuses(details.getZipCodeId(), localAddressData.getZipCodeId()))) {
                exceptionMessages.add("addressRequest.localAddressData.zipCodeId-Zip Code do not exists;");
            }
        }
        fillLocalAddressData(detailsUpdating, localAddressData);
    }

    private void updateForeignData(PointOfDeliveryDetails details, PointOfDeliveryDetails detailsUpdating, PODForeignAddressData request, List<String> exceptionMessages) {
        Long countryId = request.getCountryId();
        if (!countryRepository.existsByIdAndStatusIn(countryId, getNomenclatureStatuses(details.getCountryId(), countryId))) {
            exceptionMessages.add("addressRequest.localAddressData.countryId-Country do not exists;");
        }
        fillForeignAddressData(detailsUpdating, request);
    }

    private void isAbleToEditCurrentVersion(PointOfDeliveryDetails details) {
        List<PointOfDeliveryDetails> boundList = pointOfDeliveryDetailsRepository.checkForBoundObjects(details.getId()); //TODO SQL NEEDED
        if (!CollectionUtils.isEmpty(boundList)) {
            if (!checkIfHasLockedPermission()) {
                throw new ClientException("You can't edit POD because it is connected to the: ProductContract;", ErrorCode.CONFLICT);
            }
        }
    }

    private boolean isAbleToEditCurrentVersionForView(PointOfDeliveryDetails details) {
        List<PointOfDeliveryDetails> boundList = pointOfDeliveryDetailsRepository.checkForBoundObjects(details.getId()); //TODO SQL NEEDED
        if (!CollectionUtils.isEmpty(boundList)) {
            return true;
        } else return false;
    }

    private boolean checkIfHasLockedPermission() {
        List<String> customerContext = permissionService.getPermissionsFromContext(PermissionContextEnum.POD);
        return customerContext.contains(PermissionEnum.POD_EDIT_LOCKED.getId());
    }

    private List<NomenclatureItemStatus> getNomenclatureStatuses(Long oldAddressId, Long newAddressId) {
        if (Objects.equals(oldAddressId, newAddressId)) {
            return List.of(NomenclatureItemStatus.ACTIVE, INACTIVE);
        }
        return List.of(NomenclatureItemStatus.ACTIVE);
    }

    private void attachAddressResponseToContractResponse(PointOfDeliveryDetails details, PodContractResponse response) {
        if (Boolean.TRUE.equals(details.getForeignAddress())) {
            attachForeignDataToContractResponse(details, response);
        } else {
            attachLocalDataToContractResponse(details, response);
        }
        response.setForeign(details.getForeignAddress());
        response.setStreetNumber(details.getStreetNumber());
        response.setAddressAdditionalInfo(details.getAddressAdditionalInfo());
        response.setBlock(details.getBlock());
        response.setEntrance(details.getEntrance());
        response.setFloor(details.getFloor());
        response.setApartment(details.getApartment());
        response.setMailbox(details.getMailbox());
    }

    private void attachAddressResponse(PointOfDeliveryDetails details, PointOfDeliveryResponse response) {
        if (Boolean.TRUE.equals(details.getForeignAddress())) {
            attachForeignData(details, response);
        } else {
            attachLocalData(details, response);
        }
        response.setStreetNumber(details.getStreetNumber());
        response.setAddressAdditionalInfo(details.getAddressAdditionalInfo());
        response.setBlock(details.getBlock());
        response.setEntrance(details.getEntrance());
        response.setFloor(details.getFloor());
        response.setApartment(details.getApartment());
        response.setMailbox(details.getMailbox());
    }

    private void attachLocalData(PointOfDeliveryDetails details, PointOfDeliveryResponse response) {
        Country country = countryRepository.findByIdAndStatus(details.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Country do not exists;"));
        response.setCountry(new CountryResponse(country));
        PopulatedPlace populatedPlace = populatedPlaceRepository.findByIdAndStatus(details.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Populated place not found;"));
        response.setPopulatedPlace(new PopulatedPlaceResponse(populatedPlace));
        Municipality municipality = populatedPlace.getMunicipality();
        response.setMunicipality(new MunicipalityResponse(municipality));
        response.setRegion(new RegionResponse(municipality.getRegion()));
        ZipCode zipCode = zipCodeRepository.findById(details.getZipCodeId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Zipcode not found!;"));
        response.setZipCode(new ZipCodeResponse(zipCode));
        if (details.getDistrictId() != null) {
            districtRepository.findById(details.getDistrictId())
                    .ifPresent(x -> response.setDistrict(new DistrictResponse(x)));
        }
        if (details.getResidentialAreaId() != null) {
            residentialAreaRepository.findByIdAndStatuses(details.getResidentialAreaId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .ifPresent(x -> response.setResidentialArea(new ResidentialAreaResponse(x)));
        }
        if (details.getZipCodeId() != null) {
            zipCodeRepository.findById(details.getZipCodeId())
                    .ifPresent(x -> response.setZipCode(new ZipCodeResponse(x)));
        }
        if (details.getStreetId() != null) {
            streetRepository.findById(details.getStreetId())
                    .ifPresent(x -> response.setStreets(new StreetsResponse(x)));
        }
        response.setResidentialAreaTypeForeign(details.getForeignResidentialAreaType());
        response.setStreetTypeForeign(details.getForeignStreetType());
    }

    private void attachLocalDataToContractResponse(PointOfDeliveryDetails details, PodContractResponse response) {
        Country country = countryRepository.findByIdAndStatus(details.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Country do not exists;"));
        response.setCountry(new CountryResponse(country));
        PopulatedPlace populatedPlace = populatedPlaceRepository.findByIdAndStatus(details.getPopulatedPlaceId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Populated place not found;"));
        response.setPopulatedPlace(new PopulatedPlaceResponse(populatedPlace));
        Municipality municipality = populatedPlace.getMunicipality();
        response.setMunicipality(new MunicipalityResponse(municipality));
        response.setRegion(new RegionResponse(municipality.getRegion()));
        ZipCode zipCode = zipCodeRepository.findById(details.getZipCodeId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Zipcode not found!;"));
        response.setZipCode(new ZipCodeResponse(zipCode));
        if (details.getDistrictId() != null) {
            districtRepository.findById(details.getDistrictId())
                    .ifPresent(x -> response.setDistrict(new DistrictResponse(x)));
        }
        if (details.getResidentialAreaId() != null) {
            residentialAreaRepository.findByIdAndStatuses(details.getResidentialAreaId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                    .ifPresent(x -> response.setResidentialArea(new ResidentialAreaResponse(x)));
        }
        if (details.getZipCodeId() != null) {
            zipCodeRepository.findById(details.getZipCodeId())
                    .ifPresent(x -> response.setZipCode(new ZipCodeResponse(x)));
        }
        if (details.getStreetId() != null) {
            streetRepository.findById(details.getStreetId())
                    .ifPresent(x -> response.setStreets(new StreetsResponse(x)));
        }
    }

    private void attachForeignData(PointOfDeliveryDetails details, PointOfDeliveryResponse response) {
        Country country = countryRepository.findByIdAndStatus(details.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Country do not exists;"));
        response.setCountry(new CountryResponse(country));
        response.setDistrictForeign(details.getDistrictForeign());
        response.setZipCodeForeign(details.getZipCodeForeign());
        response.setPopulatedPlaceForeign(details.getPopulatedPlaceForeign());
        response.setMunicipalityForeign(details.getMunicipalityForeign());
        response.setRegionForeign(details.getRegionForeign());
        response.setResidentialAreaForeign(details.getResidentialAreaForeign());
        response.setResidentialAreaTypeForeign(details.getForeignResidentialAreaType());
        response.setStreetForeign(details.getStreetForeign());
        response.setStreetTypeForeign(details.getForeignStreetType());
        response.setResidentialAreaForeign(details.getResidentialAreaForeign());
    }

    private void attachForeignDataToContractResponse(PointOfDeliveryDetails details, PodContractResponse response) {
        Country country = countryRepository.findByIdAndStatus(details.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Country do not exists;"));
        response.setCountry(new CountryResponse(country));
        response.setDistrictForeign(details.getDistrictForeign());
        response.setZipCodeForeign(details.getZipCodeForeign());
        response.setPopulatedPlaceForeign(details.getPopulatedPlaceForeign());
        response.setMunicipalityForeign(details.getMunicipalityForeign());
        response.setRegionForeign(details.getRegionForeign());
        response.setResidentialAreaForeign(details.getResidentialAreaForeign());
        response.setResidentialAreaTypeForeign(details.getForeignResidentialAreaType());
        response.setStreetForeign(details.getStreetForeign());
        response.setStreetTypeForeign(details.getForeignStreetType());
        response.setResidentialAreaForeign(details.getResidentialAreaForeign());
    }

    private void checkRequestPermissions(PodCreateRequest request, List<String> permissions) {
        if (CollectionUtils.isEmpty(permissions)) {
            if (request.isBlockedBilling() && (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_BLOCK_BILLING)))) {
                throw new ClientException("You need block billing permission!;", ErrorCode.ACCESS_DENIED);

            }
            if (request.isBlockedDisconnection() && (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_BLOCK_DISCONNECTION)))) {
                throw new ClientException("You need block disconnection permission!;", ErrorCode.ACCESS_DENIED);
            }
            if (request.isImpossibleToDisconnect() && (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.POD, List.of(PermissionEnum.POD_IMPOSSIBLE_DISCONNECT)))) {
                throw new ClientException("You need Impossible disconnection permission!;", ErrorCode.ACCESS_DENIED);
            }
        } else {
            if (request.isBlockedBilling() && !permissions.contains(PermissionEnum.POD_MI_BILLING
                    .getId())) {
                throw new ClientException("You need block billing permission!;", ErrorCode.ACCESS_DENIED);

            }
            if (request.isBlockedDisconnection() && !permissions.contains(PermissionEnum.POD_MI_DISCONNECTION.getId())) {
                throw new ClientException("You need block disconnection permission!;", ErrorCode.ACCESS_DENIED);

            }
            if (request.isBlockedDisconnection() && !permissions.contains(PermissionEnum.POD_MI_IMPOSSIBLE_DISCONNECT.getId())) {
                throw new ClientException("You need Impossible disconnection permission!;", ErrorCode.ACCESS_DENIED);
            }
        }
    }

    private void validateGridOperator(Optional<GridOperator> gridOperatorOptional, String identifier, List<String> messages) {
        if (gridOperatorOptional.isEmpty()) {
            messages.add("gridOperatorId- Grid operator not found!;");
            return;
        }
        GridOperator gridOperator = gridOperatorOptional.get();
        String gridOperatorCode = gridOperator.getGridOperatorCode();
        if (gridOperatorCode == null) {
            return;
        }
        switch (gridOperatorCode) {
            case "0" -> {
                if (!(identifier.toLowerCase().startsWith("32x") || identifier.toLowerCase().startsWith("tso"))) {
                    messages.add("identifier-wrong identifier!;");
                }
            }
            case "1" -> {
                if (!(identifier.toLowerCase().startsWith("32z1") && identifier.length() == 16)) {
                    messages.add("identifier-wrong identifier!;");
                }
            }
            case "2" -> {
                if (identifier.toLowerCase().startsWith("evn") && identifier.length() == 10) {
                    break;
                } else if ((identifier.toLowerCase().startsWith("bg") && identifier.length() == 33) || ((StringUtils.isNumeric(identifier) && identifier.length() == 7))) {
                    break;
                }
                messages.add("identifier-wrong identifier!;");
            }
            case "3" -> {
                if (!(identifier.toLowerCase().startsWith("32z4") && identifier.length() == 16)) {
                    messages.add("identifier-wrong identifier!;");
                }
            }
            case "4" -> {
                if (!(identifier.toLowerCase().startsWith("32z5") && identifier.length() == 16)) {
                    messages.add("identifier-wrong identifier!;");
                }
            }
        }
    }

    private void assignLocalAddressData(PointOfDeliveryDetails details, PODLocalAddressData localAddressData, List<String> exceptionMessages) {
        Long populatedPlaceId = localAddressData.getPopulatedPlaceId();
        if (!countryRepository.existsByIdAndStatusIn(localAddressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE))) {
            exceptionMessages.add("addressRequest.localAddressData.countryId-Country do not exists;");
        }
        if (!populatedPlaceRepository.existsByIdAndMunicipalityRegionCountryId(populatedPlaceId, localAddressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE))) {
            exceptionMessages.add("addressRequest.localAddressData.populatedPlaceId-Populated place do not exists or Populated place has inactive parent object;");
        }
        if (!zipCodeRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getZipCodeId(), populatedPlaceId, List.of(NomenclatureItemStatus.ACTIVE))) {
            exceptionMessages.add("addressRequest.localAddressData.zipCodeId-ZipCode do not exists;");
        }
        if (localAddressData.getDistrictId() != null) {
            if (!districtRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getDistrictId(), populatedPlaceId, List.of(NomenclatureItemStatus.ACTIVE))) {
                exceptionMessages.add("addressRequest.localAddressData.districtId-District do not exists;");
            }
        }
        if (localAddressData.getResidentialAreaId() != null) {
            if (!residentialAreaRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getResidentialAreaId(), populatedPlaceId, List.of(NomenclatureItemStatus.ACTIVE))) {
                exceptionMessages.add("addressRequest.localAddressData.residentialAreaId-Residential Area do not exists;");
            }
        }
        if (localAddressData.getStreetId() != null) {
            if (!streetRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getStreetId(), populatedPlaceId, List.of(NomenclatureItemStatus.ACTIVE))) {
                exceptionMessages.add("addressRequest.localAddressData.streetId-Street do not exists;");
            }
        }
        if (localAddressData.getZipCodeId() != null) {
            if (!zipCodeRepository.existsByIdAndPopulatedPlaceIdAndStatusIn(localAddressData.getZipCodeId(), populatedPlaceId, List.of(NomenclatureItemStatus.ACTIVE))) {
                exceptionMessages.add("addressRequest.localAddressData.zipCodeId-Zip Code do not exists;");
            }
        }
        fillLocalAddressData(details, localAddressData);
    }

    private void assignForeignAddressData(PointOfDeliveryDetails details, PODForeignAddressData addressData, List<String> exceptionMessages) {
        if (!countryRepository.existsByIdAndStatusIn(addressData.getCountryId(), List.of(NomenclatureItemStatus.ACTIVE))) {
            exceptionMessages.add("addressRequest.foreignAddressData.countryId-Country do not exists;");
        }
        fillForeignAddressData(details, addressData);
    }

    /**
     * Checks whether a point of delivery with provided identifier and statuses exists in the system.
     *
     * @param identifier Identifier of the pod
     * @param statuses   Statuses to filter pods by
     * @return {@link PodBasicInfoResponse} if exists
     */
    public PodBasicInfoResponse existsByIdentifier(String identifier, List<PodStatus> statuses) {
        log.debug("Checking if point of delivery with identifier {} exists", identifier);

        PointOfDelivery pod = pointOfDeliveryRepository
                .findByIdentifierAndStatusIn(identifier, statuses)
                .orElseThrow(() -> new DomainEntityNotFoundException("identifier-Point of delivery not found by identifier: %s and statuses: %s;".formatted(identifier, statuses)));

        PointOfDeliveryDetails details = pointOfDeliveryDetailsRepository
                .findById(pod.getLastPodDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Point of delivery do not have details;"));

        return new PodBasicInfoResponse(
                pod.getId(),
                pod.getIdentifier(),
                details.getName(),
                pod.getStatus(),
                pod.getCreateDate()
        );
    }

    public PodContractResponse getContractResponse(String identifier) {
        PointOfDelivery pointOfDelivery = pointOfDeliveryRepository.findByIdentifierAndStatus(identifier, PodStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("id - Point of delivery not found!;"));
        PointOfDeliveryDetails details = pointOfDeliveryDetailsRepository.findById(pointOfDelivery.getLastPodDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Point of delivery do not have details;"));
        PodContractResponse response = PointOfDeliveryMapper.createPointOfDeliveryContractResponse(pointOfDelivery, details);
        GridOperator gridOperator = gridOperatorRepository.findByIdAndStatus(pointOfDelivery.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id - Grid operator not found;"));
        Long measurementTypeId = details.getPodMeasurementTypeId();
        if (details.getMeasurementType().equals(PODMeasurementType.SLP) && measurementTypeId != null) {
            MeasurementType measurementType = measurementTypeRepository.findByIdAndStatusIn(details.getPodMeasurementTypeId(), List.of(ACTIVE, INACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("id - MeasurementType not found;"));
            PodViewMeasurementType podViewMeasurementType = new PodViewMeasurementType();
            podViewMeasurementType.setMeasurementTypeId(measurementType.getId());
            podViewMeasurementType.setMeasurementTypeName(measurementType.getName());
            response.setPodViewMeasurementType(podViewMeasurementType);
        }
        response.setGridOperatorId(gridOperator.getId());
        response.setGridOperatorName(gridOperator.getName());
        attachAddressResponseToContractResponse(details, response);
        response.setPodAdditionalParameters(
                EPBListUtils.transform(
                        getPointOfDeliveryAdditionalParamsShortResponse(details.getId()),
                        ShortResponse::id,
                        Collectors.toSet()
                )
        );
        response.setPodAdditionalParametersShortResponse(getPointOfDeliveryAdditionalParamsShortResponse(details.getId()));
        return response;
    }

    @Transactional
    public ContractPodsResponse createForContract(PodContractRequest request) {
        Optional<PointOfDelivery> podOptional = pointOfDeliveryRepository.findByIdentifierAndStatus(request.getIdentifier(), PodStatus.ACTIVE);
        List<String> messages = new ArrayList<>();
        if (podOptional.isPresent()) {
            PointOfDelivery pointOfDelivery = podOptional.get();
            PodContractResponse contractResponse = getContractResponse(request.getIdentifier());
            PointOfDeliveryDetails pointOfDeliveryDetails = pointOfDeliveryDetailsRepository.findById(pointOfDelivery.getLastPodDetailId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Point of delivery do not have details;"));
            GridOperator gridOperator = gridOperatorRepository.findById(pointOfDelivery.getGridOperatorId()).orElseThrow(() -> new DomainEntityNotFoundException("Grid operator not found in update!;"));
            MeasurementType measurementType = null;
            if (request.getSlp()) {
                measurementType = measurementTypeRepository.findByIdAndStatus(request.getMeasurementTypeId(), ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("measurementTypeId-MeasurementType with ACTIVE status not found!;"));
                if (!request.getGridOperatorId().equals(measurementType.getGridOperatorId())) {
                    throw new OperationNotAllowedException("measurementTypeId-GridOperator and MeasurementType not matched!");
                }
            }

            if (request.equalsResponse(contractResponse)) {
                ContractPodsResponseImpl contractPodsResponse = new ContractPodsResponseImpl(
                        pointOfDelivery.getIdentifier(),
                        pointOfDeliveryDetails.getName(),
                        pointOfDeliveryDetails.getId(),
                        pointOfDeliveryDetails.getVersionId(),
                        pointOfDelivery.getId(),
                        pointOfDeliveryDetails.getType(),
                        gridOperator.getName(),
                        pointOfDeliveryDetails.getConsumptionPurpose(),
                        pointOfDeliveryDetails.getMeasurementType(),
                        pointOfDeliveryDetails.getEstimatedMonthlyAvgConsumption(),
                        null
                );
                if (measurementType != null) {
                    PodViewMeasurementType podViewMeasurementType = new PodViewMeasurementType();
                    podViewMeasurementType.setMeasurementTypeId(measurementType.getId());
                    podViewMeasurementType.setMeasurementTypeName(measurementType.getName());
                    contractPodsResponse.setPodViewMeasurementType(podViewMeasurementType);
                }
                contractPodsResponse.setPodAdditionalParametersShortResponse(getPointOfDeliveryAdditionalParamsShortResponse(pointOfDeliveryDetails.getId()));

                return contractPodsResponse;
            }
            return updateContractPod(pointOfDelivery, pointOfDeliveryDetails, request, messages);
        }
        return createContractPod(request, messages);
    }

    private ContractPodsResponse createContractPod(PodContractRequest request, List<String> exceptionMessages) {
        Optional<GridOperator> operator = gridOperatorRepository.findByIdAndStatus(request.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE));
        validateGridOperator(operator, request.getIdentifier(), exceptionMessages);
        PointOfDelivery pod = PointOfDeliveryMapper.createContractPod(request);
        PointOfDeliveryDetails details = PointOfDeliveryMapper.createContractPodDetails(request);
        PodAddressRequest addressRequest = request.getAddressRequest();
        PointOfDeliveryMapper.fillGeneralAddressData(details, addressRequest);
        details.setForeignAddress(addressRequest.getForeign());
        details.setVersionId(1);
        if (Boolean.TRUE.equals(addressRequest.getForeign())) {
            assignForeignAddressData(details, addressRequest.getForeignAddressData(), exceptionMessages);
        } else {
            assignLocalAddressData(details, addressRequest.getLocalAddressData(), exceptionMessages);
        }

        MeasurementType measurementType = null;
        if (request.getSlp()) {
            measurementType = measurementTypeRepository.findByIdAndStatus(request.getMeasurementTypeId(), ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("measurementTypeId-measurementTypeId not found!;"));
            if (!operator.get().getId().equals(measurementType.getGridOperatorId())) {
                throw new OperationNotAllowedException("measurementTypeId-GridOperator and MeasurementType not matched!");
            }
        }
        PodViewMeasurementType podViewMeasurementType = new PodViewMeasurementType();
        if (measurementType != null) {
            podViewMeasurementType.setMeasurementTypeId(measurementType.getId());
            podViewMeasurementType.setMeasurementTypeName(measurementType.getName());
        }

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        PointOfDelivery saved = pointOfDeliveryRepository.saveAndFlush(pod);
        details.setPodId(saved.getId());
        pointOfDeliveryDetailsRepository.saveAndFlush(details);
        processPodAdditionalParams(request.getPodAdditionalParameters(), details, null, exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        saved.setLastPodDetailId(details.getId());
        pointOfDeliveryRepository.saveAndFlush(saved);
        GridOperator gridOperator = operator.get();
        ContractPodsResponseImpl contractPodsResponse = new ContractPodsResponseImpl(
                saved.getIdentifier(),
                details.getName(),
                details.getId(),
                details.getVersionId(),
                saved.getId(),
                details.getType(),
                gridOperator.getName(),
                details.getConsumptionPurpose(),
                details.getMeasurementType(),
                details.getEstimatedMonthlyAvgConsumption(),
                podViewMeasurementType
        );
        contractPodsResponse.setPodAdditionalParametersShortResponse(getPointOfDeliveryAdditionalParamsShortResponse(details.getId()));
        return contractPodsResponse;
    }

    private ContractPodsResponse updateContractPod(PointOfDelivery pointOfDelivery, PointOfDeliveryDetails oldDetails, PodContractRequest request, List<String> exceptionMessages) {
        PointOfDeliveryDetails details = PointOfDeliveryMapper.createContractPodDetails(request);
        PointOfDeliveryDetails newDetails = PointOfDeliveryMapper.fillNewDetails(oldDetails, details, getAndIncrementLatestVersion(pointOfDelivery.getId()), pointOfDelivery.getId());
        PodAddressRequest addressRequest = request.getAddressRequest();
        PointOfDeliveryMapper.fillGeneralAddressData(newDetails, addressRequest);
        newDetails.setForeignAddress(addressRequest.getForeign());
        if (Boolean.TRUE.equals(addressRequest.getForeign())) {
            updateForeignData(oldDetails, newDetails, addressRequest.getForeignAddressData(), exceptionMessages);
        } else {
            updateLocalAddressData(oldDetails, newDetails, addressRequest.getLocalAddressData(), exceptionMessages);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);

        pointOfDeliveryDetailsRepository.saveAndFlush(newDetails);
        processPodAdditionalParams(request.getPodAdditionalParameters(), newDetails, null, exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        pointOfDelivery.setLastPodDetailId(newDetails.getId());
        pointOfDeliveryRepository.save(pointOfDelivery);
        GridOperator gridOperator = gridOperatorRepository.findById(pointOfDelivery.getGridOperatorId()).orElseThrow(() -> new DomainEntityNotFoundException("Grid operator not found in update!;"));
        MeasurementType measurementType = null;
        if (request.getSlp()) {
            measurementType = measurementTypeRepository.findByIdAndStatus(request.getMeasurementTypeId(), ACTIVE).orElseThrow(() -> new DomainEntityNotFoundException("measurementTypeId-measurementTypeId not found in update!;"));
            if (!request.getGridOperatorId().equals(measurementType.getGridOperatorId())) {
                throw new OperationNotAllowedException("measurementTypeId-GridOperator and MeasurementType not matched!");
            }
        }
        PodViewMeasurementType podViewMeasurementType = new PodViewMeasurementType();
        if (measurementType != null) {
            podViewMeasurementType.setMeasurementTypeId(measurementType.getId());
            podViewMeasurementType.setMeasurementTypeName(measurementType.getName());
        }
        ContractPodsResponseImpl contractPodsResponse = new ContractPodsResponseImpl(
                pointOfDelivery.getIdentifier(),
                newDetails.getName(),
                newDetails.getId(),
                newDetails.getVersionId(),
                pointOfDelivery.getId(),
                newDetails.getType(),
                gridOperator.getName(),
                newDetails.getConsumptionPurpose(),
                newDetails.getMeasurementType(),
                newDetails.getEstimatedMonthlyAvgConsumption(),
                podViewMeasurementType
        );
        contractPodsResponse.setPodAdditionalParametersShortResponse(getPointOfDeliveryAdditionalParamsShortResponse(newDetails.getId()));
        return contractPodsResponse;
    }

    /**
     * Converts a list of PODDisconnectionPowerSupply values to a Boolean.
     * If the list is null or empty, returns null.
     * If the list contains both YES and NO values, returns null.
     * Otherwise, returns true if the list contains a YES value, false otherwise.
     *
     * @param disconnectionPowerSupplies the list of PODDisconnectionPowerSupply values to convert
     * @return the Boolean value representing the disconnection power supply, or null if the input is invalid
     */
    private Boolean convertDisconnectionEnum(List<PODDisconnectionPowerSupply> disconnectionPowerSupplies) {
        if (disconnectionPowerSupplies == null || disconnectionPowerSupplies.isEmpty())
            return null;

        if (disconnectionPowerSupplies.contains(PODDisconnectionPowerSupply.YES) &&
                disconnectionPowerSupplies.contains(PODDisconnectionPowerSupply.NO)) {
            return null;
        }

        return disconnectionPowerSupplies.contains(PODDisconnectionPowerSupply.YES);
    }

}

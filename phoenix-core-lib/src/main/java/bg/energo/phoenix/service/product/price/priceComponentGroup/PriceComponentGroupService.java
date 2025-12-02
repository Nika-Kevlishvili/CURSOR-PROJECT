package bg.energo.phoenix.service.product.price.priceComponentGroup;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroup;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroupDetails;
import bg.energo.phoenix.model.entity.product.price.priceComponentGroup.PriceComponentGroupPriceComponent;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductPriceComponentGroups;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServicePriceComponentGroup;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupPriceComponentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupSearchField;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponentGroup.PriceComponentGroupTableColumn;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.request.product.price.priceComponentGroup.CreatePriceComponentGroupRequest;
import bg.energo.phoenix.model.request.product.price.priceComponentGroup.EditPriceComponentGroupRequest;
import bg.energo.phoenix.model.request.product.price.priceComponentGroup.PriceComponentGroupListRequest;
import bg.energo.phoenix.model.request.product.price.priceComponentGroup.priceComponent.BasePriceComponentGroupPriceComponentRequest;
import bg.energo.phoenix.model.request.product.price.priceComponentGroup.priceComponent.CreatePriceComponentGroupPriceComponentRequest;
import bg.energo.phoenix.model.request.product.price.priceComponentGroup.priceComponent.EditPriceComponentGroupPriceComponentRequest;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupListResponse;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupPriceComponentResponse;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupResponse;
import bg.energo.phoenix.model.response.priceComponentGroup.PriceComponentGroupVersion;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.product.price.applicationModel.ApplicationModelRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupDetailsRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupPriceComponentsRepository;
import bg.energo.phoenix.repository.product.price.priceComponentGroup.PriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.product.ProductPriceComponentGroupRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServicePriceComponentGroupRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.group.CopyDomainWithVersionBaseService;
import bg.energo.phoenix.service.product.price.priceComponent.PriceComponentService;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.versionDates.CalculateVersionDates;
import bg.energo.phoenix.util.versionDates.VersionWithDatesModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.CONFLICT;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PRICE_COMPONENT_GROUP;
import static bg.energo.phoenix.permissions.PermissionEnum.PRICE_COMPONENT_GROUP_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.PRICE_COMPONENT_GROUP_VIEW_DELETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceComponentGroupService implements CopyDomainWithVersionBaseService {

    private final PermissionService permissionService;
    private final PriceComponentGroupMapper priceComponentGroupMapper;
    private final PriceComponentRepository priceComponentRepository;
    private final PriceComponentService priceComponentService;
    private final ApplicationModelRepository applicationModelRepository;
    private final PriceComponentGroupRepository pcgRepository;
    private final PriceComponentGroupDetailsRepository pcgDetailsRepository;
    private final PriceComponentGroupPriceComponentsRepository pcgPriceComponentsRepository;
    private final PriceComponentGroupRepository priceComponentGroupRepository;
    private final ServicePriceComponentGroupRepository servicePriceComponentGroupRepository;
    private final ProductPriceComponentGroupRepository productPriceComponentGroupRepository;

    /**
     * Creates a new price component group with the provided request.
     *
     * @param request the {@link CreatePriceComponentGroupRequest} object containing the details of the price component group to be created.
     * @return the ID of the created {@link PriceComponentGroup}.
     * @throws ClientException               if any of the arguments provided are invalid.
     * @throws DomainEntityNotFoundException if the active price component with the specified ID is not found.
     */
    @Transactional
    public Long create(CreatePriceComponentGroupRequest request) {
        log.debug("Creating price component group with request: {}", request);

        PriceComponentGroup group = new PriceComponentGroup();
        group.setStatus(PriceComponentGroupStatus.ACTIVE);
        pcgRepository.saveAndFlush(group);

        PriceComponentGroupDetails details = createPriceComponentGroupNewVersion(
                group.getId(),
                request.getName().trim(),
                1L,
                LocalDate.now()
        );

        List<CreatePriceComponentGroupPriceComponentRequest> priceComponentsList = request.getPriceComponentsList();
        if (CollectionUtils.isNotEmpty(priceComponentsList)) {
            if (EPBListUtils.notAllUnique(priceComponentsList)) {
                log.error("Price component group price components list contains duplicates");
                throw new ClientException("priceComponentsList-Price component group price components list contains duplicates", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            List<Long> priceComponentIds = priceComponentsList
                    .stream()
                    .map(BasePriceComponentGroupPriceComponentRequest::getPriceComponentId)
                    .toList();

            List<Long> availablePriceComponentIds = priceComponentRepository
                    .getAvailablePriceComponentsIn(priceComponentIds)
                    .stream()
                    .map(PriceComponent::getId)
                    .toList();

            for (int i = 0; i < priceComponentsList.size(); i++) {
                CreatePriceComponentGroupPriceComponentRequest req = priceComponentsList.get(i);
                createPriceComponentGroupPriceComponent(availablePriceComponentIds, req.getPriceComponentId(), details.getId(), i);
            }
        }

        return group.getId();
    }

    /**
     * Creates and saves a new version of a {@link PriceComponentGroup}
     *
     * @param priceComponentGroupId {@link PriceComponentGroup} to which belongs the new version
     * @param name                  Name of the new version
     * @param versionId             Version number
     * @param startDate             Date from which the version becomes 'current'
     * @return created {@link PriceComponentGroupDetails}
     */
    private PriceComponentGroupDetails createPriceComponentGroupNewVersion(Long priceComponentGroupId,
                                                                           String name,
                                                                           long versionId,
                                                                           LocalDate startDate) {
        PriceComponentGroupDetails details = new PriceComponentGroupDetails();
        details.setPriceComponentGroupId(priceComponentGroupId);
        details.setName(name);
        details.setVersionId(versionId);
        details.setStartDate(startDate);
        return pcgDetailsRepository.saveAndFlush(details);
    }


    /**
     * Creates a new {@link PriceComponentGroupPriceComponent} for the provided {@link PriceComponentGroupDetails}
     *
     * @param availablePriceComponentIds  List of available price component IDs
     * @param requestedPriceComponentId   Requested price component ID
     * @param priceComponentGroupDetailId ID of the {@link PriceComponentGroupDetails} to which belongs the new {@link PriceComponentGroupPriceComponent}
     * @param index                       Index of the price component in the price component group price components list
     */
    private void createPriceComponentGroupPriceComponent(List<Long> availablePriceComponentIds,
                                                         Long requestedPriceComponentId,
                                                         Long priceComponentGroupDetailId,
                                                         int index) {
        // check if requested price component is available
        if (!availablePriceComponentIds.contains(requestedPriceComponentId)) {
            log.error("Price component with ID {} is not available", requestedPriceComponentId);
            throw new ClientException("priceComponentsList[%s].priceComponentId-Price component ID %s is not available"
                    .formatted(index, requestedPriceComponentId), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        // create new PriceComponentGroupPriceComponent for this version
        PriceComponentGroupPriceComponent pcgpc = new PriceComponentGroupPriceComponent();
        pcgpc.setPriceComponentGroupDetailId(priceComponentGroupDetailId);
        pcgpc.setPriceComponentId(requestedPriceComponentId);
        pcgpc.setStatus(PriceComponentGroupPriceComponentStatus.ACTIVE);
        pcgPriceComponentsRepository.saveAndFlush(pcgpc);

        markPriceComponentAsUnavailable(index, requestedPriceComponentId, priceComponentGroupDetailId);
    }


    /**
     * Sets provided {@link PriceComponentGroupDetails} ID to the {@link PriceComponent}, thus marking it as unavailable
     *
     * @param priceComponentId            ID of the {@link PriceComponent} to be marked as unavailable
     * @param priceComponentGroupDetailId ID of the {@link PriceComponentGroupDetails} to which belongs the {@link PriceComponent}
     */
    private void markPriceComponentAsUnavailable(int index, Long priceComponentId, Long priceComponentGroupDetailId) {
        PriceComponent pc = priceComponentRepository
                .findById(priceComponentId)
                .orElseThrow(() -> new DomainEntityNotFoundException("priceComponentsList[%s].priceComponentId-Price component with ID %s".formatted(index, priceComponentId)));
        pc.setPriceComponentGroupDetailId(priceComponentGroupDetailId);
        priceComponentRepository.saveAndFlush(pc);
    }


    /**
     * Updates existing version of a price component group with the provided request and ID, or creates a new version.
     *
     * @param request the {@link EditPriceComponentGroupRequest} object containing the details of the price component group to be updated.
     * @param groupId the ID of the price component group to be updated.
     * @return the ID of the updated {@link PriceComponentGroup}.
     * @throws DomainEntityNotFoundException if the active price component with the specified ID is not found.
     * @throws ClientException               if any of the arguments provided are invalid.
     */
    @Transactional
    public Long update(EditPriceComponentGroupRequest request, Long groupId) {
        log.debug("Updating price component group with request: {}", request);

        if (!request.getUpdateExistingVersion() && request.getStartDate().isBefore(LocalDate.now())) {
            if (priceComponentGroupRepository.hasLockedConnection(groupId) && !hasEditLockedPermission()) {
                throw new OperationNotAllowedException(String.format("id-You can’t edit the Group of price components with id: %s because it is connected to the product contract version , Service contract version or Service order", groupId));
            }
        }

        // not to produce null pointer exceptions
        if (CollectionUtils.isEmpty(request.getPriceComponentsList())) {
            request.setPriceComponentsList(Collections.emptyList());
        }

        if (!pcgRepository.existsByIdAndStatusIn(groupId, List.of(PriceComponentGroupStatus.ACTIVE))) {
            log.error("Active price component group with ID {} not found", groupId);
            throw new DomainEntityNotFoundException("groupId-Active price component group with ID %s not found".formatted(groupId));
        }

        PriceComponentGroupDetails currVersion = pcgDetailsRepository
                .findByPriceComponentGroupIdAndVersionId(groupId, request.getVersionId())
                .orElseThrow(() -> new DomainEntityNotFoundException("versionId-Price component group version with ID %s not found".formatted(request.getVersionId())));

        List<Long> requestPriceComponentIds = request.getPriceComponentsList()
                .stream()
                .map(BasePriceComponentGroupPriceComponentRequest::getPriceComponentId)
                .toList();

        checkAgainstDuplicatePriceComponentValues(requestPriceComponentIds);

        List<EditPriceComponentGroupPriceComponentRequest> requestPriceComponents = request.getPriceComponentsList();

        if (request.getUpdateExistingVersion()) {
            currVersion.setName(request.getName().trim());

            // retrieve active price components attached to this version
            List<PriceComponentGroupPriceComponent> dbActivePCGPCs = pcgPriceComponentsRepository
                    .findByPriceComponentGroupVersionAndStatusIn(
                            groupId,
                            request.getVersionId(),
                            List.of(PriceComponentGroupPriceComponentStatus.ACTIVE)
                    );

            // such order is important as, for example, the user can delete price component A (which was already persisted)
            // and add the same price component A again (ID will be null). So, first the deleted price components should be released
            // and only then should be fetched the available price components.
            removeDeletedPriceComponentGroupPriceComponents(
                    dbActivePCGPCs,
                    request.getPriceComponentsList().stream()
                            .map(EditPriceComponentGroupPriceComponentRequest::getId)
                            .filter(Objects::nonNull).toList()
            );

            // retrieve available price components filtered by the IDs provided in the request
            List<Long> availablePriceComponentIds = priceComponentRepository
                    .getAvailablePriceComponentsIn(requestPriceComponentIds)
                    .stream()
                    .map(PriceComponent::getId)
                    .toList();

            // process new price components only
            for (int i = 0; i < requestPriceComponents.size(); i++) {
                EditPriceComponentGroupPriceComponentRequest req = requestPriceComponents.get(i);
                if (req.getId() == null) {
                    createPriceComponentGroupPriceComponent(availablePriceComponentIds, req.getPriceComponentId(), currVersion.getId(), i);
                }
            }
        } else {
            validateStartDateUniqueness(groupId, request.getStartDate());

            PriceComponentGroupDetails newVersion = createPriceComponentGroupNewVersion(
                    groupId,
                    request.getName().trim(),
                    pcgDetailsRepository.findLastVersionByPriceComponentGroupId(groupId) + 1,
                    request.getStartDate()
            );

            // retrieve available price components filtered by the IDs provided in the request
            List<Long> availablePriceComponentIds = priceComponentRepository
                    .getAvailablePriceComponentsIn(requestPriceComponentIds)
                    .stream()
                    .map(PriceComponent::getId)
                    .toList();

            for (int i = 0; i < requestPriceComponents.size(); i++) {
                EditPriceComponentGroupPriceComponentRequest req = requestPriceComponents.get(i);
                if (req.getId() == null) {
                    // if the newly added price component is same as in the parent version (user deleted persisted one but added the same again anew),
                    // it should be cloned as the price component is still active in parent version and new PCGPC should be created
                    if (pcgPriceComponentsRepository.existsByPriceComponentIdAndStatus(req.getPriceComponentId(), PriceComponentGroupPriceComponentStatus.ACTIVE)) {
                        PriceComponent clonedPriceComponent = priceComponentService.clonePriceComponent(req.getPriceComponentId());
                        createPriceComponentGroupPriceComponent(List.of(clonedPriceComponent.getId()), clonedPriceComponent.getId(), newVersion.getId(), i);
                    } else {
                        createPriceComponentGroupPriceComponent(availablePriceComponentIds, req.getPriceComponentId(), newVersion.getId(), i);
                    }
                } else {
                    // if the price component is the same that is in the parent version (in that case ID will not be null), should clone price component
                    PriceComponent clonedPriceComponent = priceComponentService.clonePriceComponent(req.getPriceComponentId());
                    createPriceComponentGroupPriceComponent(List.of(clonedPriceComponent.getId()), clonedPriceComponent.getId(), newVersion.getId(), i);
                }
            }
        }

        updateVersionStartAndEndDates(groupId, currVersion);

        return groupId;
    }

    private void updateVersionStartAndEndDates(Long priceComponentGroupId, PriceComponentGroupDetails priceComponentGroupDetails) {
        List<PriceComponentGroupDetails> priceComponentVersions = priceComponentGroupRepository.findPriceComponentGroupDetailsByPriceComponentGroup(priceComponentGroupId);
        List<VersionWithDatesModel> versionWithDatesModels = priceComponentVersions.stream().map(VersionWithDatesModel::new).collect(Collectors.toList());

        List<VersionWithDatesModel> updatedVersionWithDatesModels = CalculateVersionDates.calculateVersionEndDates(versionWithDatesModels, priceComponentGroupDetails.getStartDate(), Math.toIntExact(priceComponentGroupDetails.getVersionId()));

        priceComponentVersions
                .forEach(pcv -> updatedVersionWithDatesModels.stream()
                        .filter(v -> Objects.equals(v.getVersionId(), Math.toIntExact(pcv.getVersionId())))
                        .findFirst()
                        .ifPresent(model -> {
                            pcv.setEndDate(model.getEndDate());
                            pcv.setStartDate(model.getStartDate());
                        }));
    }

    /**
     * Loops over persisted list of ACTIVE {@link PriceComponentGroupPriceComponent} entities and sets DELETED status
     * if attached price component ID not found in the price component ID list provided in the request.
     * Updates released {@link PriceComponent} as available.
     */
    private void removeDeletedPriceComponentGroupPriceComponents(List<PriceComponentGroupPriceComponent> dbActivePCGPCs,
                                                                 List<Long> providedPCGPCIds) {
        for (PriceComponentGroupPriceComponent pcgpc : dbActivePCGPCs) {
            if (!providedPCGPCIds.contains(pcgpc.getId())) {
                pcgpc.setStatus(PriceComponentGroupPriceComponentStatus.DELETED);
                pcgPriceComponentsRepository.saveAndFlush(pcgpc);

                PriceComponent priceComponent = priceComponentRepository
                        .findById(pcgpc.getPriceComponentId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("priceComponentsList[%s].priceComponentId-Price component not found by ID ".formatted(pcgpc.getPriceComponentId())));

                priceComponent.setPriceComponentGroupDetailId(null);
                priceComponentRepository.saveAndFlush(priceComponent);
            }
        }
    }


    /**
     * Checks if the provided list of {@link PriceComponent} ids contains duplicate values.
     *
     * @param priceComponentIds list of {@link PriceComponent} ids that needs to be validated.
     */
    private void checkAgainstDuplicatePriceComponentValues(List<Long> priceComponentIds) {
        if (EPBListUtils.notAllUnique(priceComponentIds)) {
            log.error("Price components list contains duplicate values");
            throw new ClientException("priceComponentsList-Price components list contains duplicate values", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    /**
     * Validates if the provided start date is unique among the versions of the provided {@link PriceComponent}
     *
     * @param priceComponentGroupId ID of a {@link PriceComponent} among the versions of which the start date should be unique
     * @param startDate             Date that should be checked for uniqueness
     */
    private void validateStartDateUniqueness(Long priceComponentGroupId, LocalDate startDate) {
        if (pcgDetailsRepository.existsByPriceComponentGroupIdAndStartDate(priceComponentGroupId, startDate)) {
            log.error("Price component group version with start date %s already exists".formatted(startDate));
            throw new ClientException("startDate-Selected date can’t be start date for new version!;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    /**
     * Deletes the price component group with the provided ID if all the validations are passed.
     *
     * @param id the ID of the price component group to be deleted.
     * @throws DomainEntityNotFoundException if the active price component with the specified ID is not found.
     * @throws ClientException               if the price component group is connected to a product/service or has already been deleted
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting price component group with ID %s".formatted(id));

        PriceComponentGroup group = pcgRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Price component group not found by ID %s".formatted(id)));

        if (group.getStatus().equals(PriceComponentGroupStatus.DELETED)) {
            log.error("Price component group with ID %s is already deleted".formatted(id));
            throw new ClientException("id-Price component group with ID %s is already deleted".formatted(id), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (pcgRepository.hasConnectionToProduct(id, List.of(ProductStatus.ACTIVE), List.of(ProductSubObjectStatus.ACTIVE))) {
            log.error("You can’t delete the group of price component because it is connected to the product");
            throw new ClientException("id-You can’t delete the group of price component with ID [%s] because it is connected to the product".formatted(id), CONFLICT);
        }

        if (pcgRepository.hasConnectionToService(id, List.of(ServiceStatus.ACTIVE), List.of(ServiceSubobjectStatus.ACTIVE))) {
            log.error("You can't delete the group of price component because it is connected to the service");
            throw new ClientException("id-You can't delete the group of price component with ID [%s] because it is connected to the service".formatted(id), CONFLICT);
        }

        group.setStatus(PriceComponentGroupStatus.DELETED);
        pcgRepository.saveAndFlush(group);

        List<PriceComponent> connectedPriceComponents = priceComponentRepository.getConnectedActivePriceComponentsByPriceComponentGroupId(id);
        connectedPriceComponents.forEach(pc -> pc.setPriceComponentGroupDetailId(null));
        priceComponentRepository.saveAll(connectedPriceComponents);

        return id;
    }


    /**
     * Retrieves a preview of a price component group based on the given group ID and version.
     * If version is not specified, current price component group details are returned.
     *
     * @param groupId the ID of the price component group details to retrieve
     * @param version the version of the price component group to retrieve, or null to retrieve the current version
     * @return a {@link PriceComponentGroupResponse} object containing details of the requested price component group
     * @throws DomainEntityNotFoundException if the price component group or its details cannot be found based on the given parameters
     */
    public PriceComponentGroupResponse preview(Long groupId, Long version) {
        log.info("Retrieving preview of price component group with ID %s and version %s".formatted(groupId, version));

        PriceComponentGroupDetails details;
        if (version == null) {
            details = pcgDetailsRepository
                    .findFirstByPriceComponentGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(groupId, LocalDate.now())
                    .orElseThrow(() -> new DomainEntityNotFoundException("groupId-Current details version not found by group ID %s;".formatted(groupId)));
        } else {
            details = pcgDetailsRepository
                    .findByPriceComponentGroupIdAndVersionId(groupId, version)
                    .orElseThrow(() -> new DomainEntityNotFoundException("groupId-Version %s details not found by group ID %s;".formatted(groupId, version)));
        }

        PriceComponentGroup group = pcgRepository
                .findByIdAndStatusIn(groupId, getStatusesByPermission())
                .orElseThrow(() -> new DomainEntityNotFoundException("groupId-Price component group not found by ID %s;".formatted(groupId)));


        List<PriceComponentGroupPriceComponentResponse> groupPriceComponents = pcgPriceComponentsRepository
                .findByPriceComponentGroupDetailIdAndStatusIn(details.getId(), List.of(PriceComponentGroupPriceComponentStatus.ACTIVE));

        PriceComponentGroupResponse priceComponentGroupResponse = priceComponentGroupMapper.priceComponentGroupResponseFromEntity(
                group,
                details,
                pcgDetailsRepository.getPriceComponentGroupVersions(groupId),
                groupPriceComponents);
        priceComponentGroupResponse.setIsLocked(priceComponentGroupRepository.hasLockedConnection(groupId));
        if (details.getStartDate().isAfter(LocalDate.now())) {
            priceComponentGroupResponse.setIsLocked(false);
        }
        return priceComponentGroupResponse;
    }


    /**
     * Returns a page of {@link PriceComponentGroupListResponse} based on the provided search criteria.
     *
     * @param request the search criteria to use when fetching the price component group details
     * @return a page of {@link PriceComponentGroupListResponse} that match the search criteria
     */
    public Page<PriceComponentGroupListResponse> list(PriceComponentGroupListRequest request) {
        log.debug("Retrieving price component group list with search criteria %s".formatted(request));

        String sortBy = PriceComponentGroupTableColumn.PCG_DATE_OF_CREATION.getValue();
        if (request.getSortBy() != null && StringUtils.isNotEmpty(request.getSortBy().getValue())) {
            sortBy = request.getSortBy().getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String searchBy = PriceComponentGroupSearchField.ALL.getValue();
        if (request.getSearchBy() != null && StringUtils.isNotEmpty(request.getSearchBy().getValue())) {
            searchBy = request.getSearchBy().getValue();
        }

        Boolean excludeOldVersion = Boolean.TRUE.equals(request.getExcludeOldVersions());
        Boolean excludeFutureVersion = Boolean.TRUE.equals(request.getExcludeFutureVersions());

        String excludeVersion = (excludeOldVersion && excludeFutureVersion)
                ? "excludeOldAndFuture"
                : (excludeOldVersion ? "excludeOld"
                : (excludeFutureVersion ? "excludeFuture"
                : null));

        return pcgRepository
                .list(
                        searchBy,
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        getStatusesByPermission().stream().map(Enum::name).toList(),
                        excludeVersion,
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(new Sort.Order(sortDirection, sortBy))
                        )
                );
    }


    /**
     * @return a list of {@link PriceComponentGroupStatus} based on the user's permissions
     */
    private List<PriceComponentGroupStatus> getStatusesByPermission() {
        List<PriceComponentGroupStatus> priceComponentGroupStatuses = new ArrayList<>();
        if (permissionService.getPermissionsFromContext(PRICE_COMPONENT_GROUP).contains(PRICE_COMPONENT_GROUP_VIEW_BASIC.getId())) {
            priceComponentGroupStatuses.add(PriceComponentGroupStatus.ACTIVE);
        }

        if (permissionService.getPermissionsFromContext(PRICE_COMPONENT_GROUP).contains(PRICE_COMPONENT_GROUP_VIEW_DELETED.getId())) {
            priceComponentGroupStatuses.add(PriceComponentGroupStatus.DELETED);
        }
        return priceComponentGroupStatuses;
    }


    public PriceComponentGroupResponse viewWithCopy(Long id, Long version) {
        log.debug("Copying price component groups with id: {} and version {}", id, version);
        PriceComponentGroup priceComponentGroup = pcgRepository.findByIdAndStatusIn(id, List.of(PriceComponentGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Price Component group not found with this id: %s Or you dont have permission;".formatted(id)));
        PriceComponentGroupDetails priceComponentGroupDetails = pcgDetailsRepository.findByPriceComponentGroupIdAndVersionId(id, version)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Price Component group not found with this id: %s and version: %s Or you dont have permission;".formatted(id, version)));
        List<PriceComponentGroupPriceComponent> priceComponentGroupPriceComponents =
                pcgPriceComponentsRepository.findByPriceComponentGroupDetailIdAndStatus(priceComponentGroupDetails.getId(), PriceComponentGroupPriceComponentStatus.ACTIVE);

        List<PriceComponentGroupVersion> versions = pcgDetailsRepository.getPriceComponentGroupVersions(priceComponentGroup.getId());

        List<ApplicationModel> applicationModelsToCopy = new ArrayList<>();
        List<ApplicationModel> newApplicationModels = new ArrayList<>();

        if (!CollectionUtils.isEmpty(priceComponentGroupPriceComponents)) {
            for (PriceComponentGroupPriceComponent item : priceComponentGroupPriceComponents) {
                Optional<ApplicationModel> applicationModel = applicationModelRepository.findByPriceComponentIdAndStatusIn(item.getPriceComponentId(), List.of(ApplicationModelStatus.ACTIVE));
                applicationModel.ifPresent(applicationModelsToCopy::add);
            }
        }

//
//        for (ApplicationModel item : applicationModelsToCopy) {
//            ApplicationModel copied = priceComponentService.copyApplicationModels(item);
//            if (copied != null) {
//                newApplicationModels.add(copied);
//            }
//        }

        newApplicationModels = priceComponentService.copyPriceComponentsWithResponse(applicationModelsToCopy);


        PriceComponentGroup copiedPriceComponentGroup = copyPriceComponentGroup(priceComponentGroup);
        PriceComponentGroupDetails copiedPriceComponentGroupDetails = copyPriceComponentGroupDetails(copiedPriceComponentGroup, priceComponentGroupDetails);


        List<PriceComponentGroupPriceComponentResponse> groupPriceComponents = new ArrayList<>();
        for (ApplicationModel item : newApplicationModels) {
            PriceComponentGroupPriceComponentResponse priceComponentGroupPriceComponentResponse = new PriceComponentGroupPriceComponentResponse();
            priceComponentGroupPriceComponentResponse.setPriceComponentId(item.getPriceComponent().getId());
            priceComponentGroupPriceComponentResponse.setPriceComponentName(item.getPriceComponent().getName());
            groupPriceComponents.add(priceComponentGroupPriceComponentResponse);
        }
        return mapPriceComponentGroupResponse(copiedPriceComponentGroup, copiedPriceComponentGroupDetails, versions, groupPriceComponents);
    }

    private PriceComponentGroupResponse mapPriceComponentGroupResponse(PriceComponentGroup copiedPriceComponentGroup,
                                                                       PriceComponentGroupDetails copiedPriceComponentGroupDetails,
                                                                       List<PriceComponentGroupVersion> versions,
                                                                       List<PriceComponentGroupPriceComponentResponse> priceComponents) {
        return PriceComponentGroupResponse.builder()
                .status(copiedPriceComponentGroup.getStatus())
                .versionId(1L)
                .name(copiedPriceComponentGroupDetails.getName())
                .versions(versions)
                .priceComponentsList(priceComponents)
                .build();
    }

    private PriceComponentGroupDetails copyPriceComponentGroupDetails(PriceComponentGroup copiedPriceComponentGroup,
                                                                      PriceComponentGroupDetails originalPriceComponentGroupDetails) {
        PriceComponentGroupDetails priceComponentGroupDetails = new PriceComponentGroupDetails();
        priceComponentGroupDetails.setName(originalPriceComponentGroupDetails.getName());
        priceComponentGroupDetails.setStartDate(originalPriceComponentGroupDetails.getStartDate());
        priceComponentGroupDetails.setVersionId(1L);
        return priceComponentGroupDetails;
    }

    private PriceComponentGroup copyPriceComponentGroup(PriceComponentGroup original) {
        PriceComponentGroup priceComponentGroup = new PriceComponentGroup();
        priceComponentGroup.setStatus(PriceComponentGroupStatus.ACTIVE);
        return priceComponentGroup;
    }

    @Override
    public CopyDomainWithVersion getGroupType() {
        return CopyDomainWithVersion.PRICE_COMPONENT_GROUP;
    }

    @Override
    public Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request) {
        log.debug("Finding price component groups with request: {}", request);
        return pcgRepository.findByCopyGroupBaseRequest(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                LocalDate.now(),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    @Override
    public List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long groupId) {
        return pcgDetailsRepository.findByCopyGroupBaseRequest(groupId);
    }


    /**
     * Adds price component groups to service
     *
     * @param priceComponentGroupIds ids of price component groups to be added
     * @param serviceDetails         service details to which price component groups will be added
     * @param exceptionMessages      list of exception messages to be filled in case of errors
     */
    @Transactional
    public void addPriceComponentGroupsToService(List<Long> priceComponentGroupIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(priceComponentGroupIds)) {
            List<ServicePriceComponentGroup> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentGroupIds.size(); i++) {
                Long pcgId = priceComponentGroupIds.get(i);
                // being an active group is a sufficient condition for adding it to the service
                Optional<PriceComponentGroup> priceComponentGroupOptional = priceComponentGroupRepository.findByIdAndStatusIn(pcgId, List.of(PriceComponentGroupStatus.ACTIVE));
                if (priceComponentGroupOptional.isEmpty()) {
                    log.error("priceComponentGroups[%s]-Can't find Price Component Group with id: %s;".formatted(i, pcgId));
                    exceptionMessages.add("priceSettings.priceComponentGroups[%s]-Can't find Price Component Group with id: %s;".formatted(i, pcgId));
                    continue;
                }

                ServicePriceComponentGroup spcg = new ServicePriceComponentGroup();
                spcg.setServiceDetails(serviceDetails);
                spcg.setPriceComponentGroup(priceComponentGroupOptional.get());
                spcg.setStatus(ServiceSubobjectStatus.ACTIVE);
                tempList.add(spcg);
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all price component groups
            servicePriceComponentGroupRepository.saveAll(tempList);
        }
    }


    /**
     * Updates price component groups for service existing version
     *
     * @param priceComponentGroupIds ids of price component groups to be added
     * @param serviceDetails         service details to which price component groups will be added
     * @param exceptionMessages      list of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateServicePriceComponentGroupsForExistingVersion(List<Long> priceComponentGroupIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        // fetch all active price component groups for the service
        List<ServicePriceComponentGroup> dbPriceComponentGroups = servicePriceComponentGroupRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(priceComponentGroupIds)) {
            List<Long> dbPriceComponentGroupIds = dbPriceComponentGroups.stream().map(ServicePriceComponentGroup::getPriceComponentGroup).map(PriceComponentGroup::getId).toList();
            List<ServicePriceComponentGroup> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentGroupIds.size(); i++) {
                Long priceComponentGroupId = priceComponentGroupIds.get(i);
                if (!dbPriceComponentGroupIds.contains(priceComponentGroupId)) { // if price component group is new, its availability should be checked
                    Optional<PriceComponentGroup> priceComponentGroup = priceComponentGroupRepository.findByIdAndStatusIn(priceComponentGroupId, List.of(PriceComponentGroupStatus.ACTIVE));
                    if (priceComponentGroup.isEmpty()) {
                        log.error("priceComponentGroups[%s]-can't find Price Component Group with id: %s;".formatted(i, priceComponentGroupId));
                        exceptionMessages.add("priceSettings.priceComponentGroups[%s]-can't find Price Component Group with id: %s;".formatted(i, priceComponentGroupId));
                        continue;
                    }

                    ServicePriceComponentGroup spcg = new ServicePriceComponentGroup();
                    spcg.setServiceDetails(serviceDetails);
                    spcg.setPriceComponentGroup(priceComponentGroup.get());
                    spcg.setStatus(ServiceSubobjectStatus.ACTIVE);
                    tempList.add(spcg);
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all price component groups
            servicePriceComponentGroupRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbPriceComponentGroups)) {
            for (ServicePriceComponentGroup dbPriceComponentGroup : dbPriceComponentGroups) {
                // if user has removed the IAPG, set its status to DELETED
                if (!priceComponentGroupIds.contains(dbPriceComponentGroup.getPriceComponentGroup().getId())) {
                    dbPriceComponentGroup.setStatus(ServiceSubobjectStatus.DELETED);
                    servicePriceComponentGroupRepository.save(dbPriceComponentGroup);
                }
            }
        }
    }

    /**
     * Adds price component groups to product
     *
     * @param priceComponentGroupIdsSet ids of price component groups to be added
     * @param productDetails            product details to which price component groups will be added
     * @param exceptionMessages         list of exception messages to be filled in case of errors
     */
    @Transactional
    public void addPriceComponentGroupsToProduct(List<Long> priceComponentGroupIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(priceComponentGroupIdsSet)) {
            List<Long> priceComponentGroupIds = new ArrayList<>(priceComponentGroupIdsSet); // this is for the sake of getting index of element when handling errors
            List<ProductPriceComponentGroups> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentGroupIds.size(); i++) {
                Long pcgId = priceComponentGroupIds.get(i);
                // being an active group is a sufficient condition for adding it to the product
                Optional<PriceComponentGroup> priceComponentGroupOptional = priceComponentGroupRepository.findByIdAndStatusIn(pcgId, List.of(PriceComponentGroupStatus.ACTIVE));
                if (priceComponentGroupOptional.isEmpty()) {
                    log.error("priceComponentGroupIds[%s]-Can't find Price Component Group with id: %s;".formatted(i, pcgId));
                    exceptionMessages.add("priceSettings.priceComponentGroupIds[%s]-Can't find Price Component Group with id: %s;".formatted(i, pcgId));
                    continue;
                }

                ProductPriceComponentGroups productPriceComponentGroups = new ProductPriceComponentGroups();
                productPriceComponentGroups.setProductDetails(productDetails);
                productPriceComponentGroups.setPriceComponentGroup(priceComponentGroupOptional.get());
                productPriceComponentGroups.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                tempList.add(productPriceComponentGroups);
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all price component groups
            productPriceComponentGroupRepository.saveAll(tempList);
        }
    }


    /**
     * Updates price component groups for product existing version
     *
     * @param priceComponentGroupIdsSet ids of price component groups to be added
     * @param productDetails            product details to which price component groups will be added
     * @param exceptionMessages         list of exception messages to be filled in case of errors
     */
    @Transactional
    public void updateProductPriceComponentGroupsForExistingVersion(List<Long> priceComponentGroupIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        // fetch all active price component groups for the product
        List<ProductPriceComponentGroups> dbPriceComponentGroups = productPriceComponentGroupRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(productDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(priceComponentGroupIdsSet)) {
            List<Long> priceComponentGroupIds = new ArrayList<>(priceComponentGroupIdsSet); // this is for the sake of getting index of element when handling errors
            List<Long> dbPriceComponentGroupIds = dbPriceComponentGroups.stream().map(ProductPriceComponentGroups::getPriceComponentGroup).map(PriceComponentGroup::getId).toList();
            List<ProductPriceComponentGroups> tempList = new ArrayList<>();

            for (int i = 0; i < priceComponentGroupIds.size(); i++) {
                Long priceComponentGroupId = priceComponentGroupIds.get(i);
                if (!dbPriceComponentGroupIds.contains(priceComponentGroupId)) { // if price component group is new, its availability should be checked
                    Optional<PriceComponentGroup> priceComponentGroup = priceComponentGroupRepository.findByIdAndStatusIn(priceComponentGroupId, List.of(PriceComponentGroupStatus.ACTIVE));
                    if (priceComponentGroup.isEmpty()) {
                        log.error("priceComponentGroupIds[%s]-can't find Price Component Group with id: %s;".formatted(i, priceComponentGroupId));
                        exceptionMessages.add("priceSettings.priceComponentGroupIds[%s]-can't find Price Component Group with id: %s;".formatted(i, priceComponentGroupId));
                        continue;
                    }

                    ProductPriceComponentGroups productPriceComponentGroups = new ProductPriceComponentGroups();
                    productPriceComponentGroups.setProductDetails(productDetails);
                    productPriceComponentGroups.setPriceComponentGroup(priceComponentGroup.get());
                    productPriceComponentGroups.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    tempList.add(productPriceComponentGroups);
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all price component groups
            productPriceComponentGroupRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbPriceComponentGroups)) {
            for (ProductPriceComponentGroups ppcg : dbPriceComponentGroups) {
                // if user has removed the ppcg, set its status to DELETED
                if (!priceComponentGroupIdsSet.contains(ppcg.getPriceComponentGroup().getId())) {
                    ppcg.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
                    productPriceComponentGroupRepository.save(ppcg);
                }
            }
        }
    }

    private boolean hasEditLockedPermission() {
        return permissionService.permissionContextContainsPermissions(PRICE_COMPONENT_GROUP, List.of(PermissionEnum.PRICE_COMPONENT_GROUP_EDIT_LOCKED));
    }

}

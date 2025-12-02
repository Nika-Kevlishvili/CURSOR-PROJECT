package bg.energo.phoenix.service.product.penalty.penaltyGroups;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.penalty.penalty.Penalty;
import bg.energo.phoenix.model.entity.product.penalty.penaltyGroups.*;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductPenaltyGroups;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServicePenaltyGroup;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.enums.product.penalty.PenaltyGroupExcludeVersion;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.request.product.penalty.penaltyGroup.PenaltyGroupCreateRequest;
import bg.energo.phoenix.model.request.product.penalty.penaltyGroup.PenaltyGroupListRequest;
import bg.energo.phoenix.model.request.product.penalty.penaltyGroup.PenaltyGroupUpdateRequest;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupListResponse;
import bg.energo.phoenix.model.response.penaltyGroup.PenaltyGroupResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyPaymentTermRepository;
import bg.energo.phoenix.repository.product.penalty.penalty.PenaltyRepository;
import bg.energo.phoenix.repository.product.penalty.penaltyGroup.PenaltyGroupDetailsRepository;
import bg.energo.phoenix.repository.product.penalty.penaltyGroup.PenaltyGroupPenaltyRepository;
import bg.energo.phoenix.repository.product.penalty.penaltyGroup.PenaltyGroupRepository;
import bg.energo.phoenix.repository.product.product.ProductPenaltyGroupRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServicePenaltyGroupRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.group.CopyDomainWithVersionBaseService;
import bg.energo.phoenix.service.product.penalty.penalty.PenaltyService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.AllArgsConstructor;
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

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.exception.ErrorCode.OPERATION_NOT_ALLOWED;
import static bg.energo.phoenix.model.entity.EntityStatus.ACTIVE;
import static bg.energo.phoenix.model.entity.EntityStatus.DELETED;
import static bg.energo.phoenix.permissions.PermissionContextEnum.PENALTY_GROUP;
import static bg.energo.phoenix.permissions.PermissionEnum.PENALTY_GROUP_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.PENALTY_GROUP_VIEW_DELETED;

@Service
@Slf4j
@AllArgsConstructor
public class PenaltyGroupService implements CopyDomainWithVersionBaseService {
    private final ServicePenaltyGroupRepository servicePenaltyGroupRepository;
    private final PenaltyGroupRepository penaltyGroupRepository;
    private final PenaltyGroupDetailsRepository penaltyGroupDetailsRepository;
    private final PenaltyGroupPenaltyRepository penaltyGroupPenaltyRepository;
    private final PenaltyRepository penaltyRepository;
    private final PenaltyGroupMapper penaltyGroupMapper;
    private final PenaltyService penaltyService;
    private final CurrencyRepository currencyRepository;
    private final PenaltyPaymentTermRepository penaltyPaymentTermRepository;
    private final PermissionService permissionService;
    private final ProductPenaltyGroupRepository productPenaltyGroupRepository;
    private final CalendarRepository calendarRepository;

    /**
     * Returns the penalty group info with the given id
     *
     * @param id      of the penalty group to be returned
     * @param version of the penalty group to be returned, if not specified will take the version for current date
     * @return Response dto for created penalty group entity
     * @throws DomainEntityNotFoundException if penalty group or penalty group details not found for given id
     */

    public PenaltyGroupResponse getById(Long id, Integer version) {
        List<EntityStatus> penaltyGroupStatuses = getEntityStatuses();

        var penaltyGroup = penaltyGroupRepository.findByIdAndStatusIn(id, penaltyGroupStatuses)
                .orElseThrow(() -> new DomainEntityNotFoundException("Unable to find penalty group with given id : %s".formatted(id)));

        var penaltyGroupDetails = getPenaltyGroupDetails(id, version, penaltyGroup);
        var versions = penaltyGroupDetailsRepository.findAllByPenaltyGroupIdOrderByStartDateDesc(penaltyGroup.getId());

        var penaltyGroupPenalties = penaltyGroupPenaltyRepository.findAllGroupPenalties(penaltyGroupDetails.getId(), List.of(ACTIVE));

        PenaltyGroupResponse penaltyGroupResponse = penaltyGroupMapper.toResponse(penaltyGroup, penaltyGroupDetails, penaltyGroupPenalties, versions);
        penaltyGroupResponse.setIsLocked(penaltyGroupRepository.hasLockedConnection(id));
        if (penaltyGroupDetails.getStartDate().isAfter(LocalDate.now())) {
            penaltyGroupResponse.setIsLocked(false);
        }
        return penaltyGroupResponse;
    }

    private List<EntityStatus> getEntityStatuses() {
        List<EntityStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PENALTY_GROUP, List.of(PENALTY_GROUP_VIEW_DELETED))) {
            statuses.add(DELETED);
        }
        if (permissionService.permissionContextContainsPermissions(PENALTY_GROUP, List.of(PENALTY_GROUP_VIEW_BASIC))) {
            statuses.add(ACTIVE);
        }
        return statuses;
    }

    private PenaltyGroupDetails getPenaltyGroupDetails(Long id, Integer version, PenaltyGroup penaltyGroup) {
        return Optional.ofNullable(version)
                .map(v -> penaltyGroupDetailsRepository.findByPenaltyGroupIdAndVersionId(penaltyGroup.getId(), v))
                .orElse(penaltyGroupDetailsRepository.findFirstByPenaltyGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(penaltyGroup.getId(), LocalDate.now()))
                .orElseThrow(() -> new DomainEntityNotFoundException("Unable to find penalty group details for group with given id : %s".formatted(id)));
    }

    /**
     * Deletes the penalty with the given id if all the validations are passed
     *
     * @param id of the penalty to be updated
     * @throws DomainEntityNotFoundException if penalty or found for given id
     */
    @Transactional
    public Long delete(Long id) {
        var penaltyGroup = penaltyGroupRepository
                .findById(id)
                .orElseThrow(() ->
                        new DomainEntityNotFoundException("id-Unable to find penalty group with Id : %s".formatted(id)));

        if (penaltyGroupRepository.hasConnectionToProduct(id, List.of(ProductStatus.ACTIVE), List.of(ProductSubObjectStatus.ACTIVE))) {
            log.error("id-You can’t delete the group of penalty with ID [%s] because it is connected to the product".formatted(id));
            throw new ClientException("id-You can’t delete the group of penalty with ID [%s] because it is connected to the product".formatted(id), OPERATION_NOT_ALLOWED);
        }

        if (penaltyGroupRepository.hasConnectionToService(id, List.of(ServiceStatus.ACTIVE), List.of(ServiceSubobjectStatus.ACTIVE))) {
            log.error("id-You can’t delete the group of penalty with ID [%s] because it is connected to the service".formatted(id));
            throw new ClientException("id-You can’t delete the group of penalty with ID [%s] because it is connected to the service".formatted(id), OPERATION_NOT_ALLOWED);
        }

        var penalties = penaltyRepository.findAllActiveByPenaltyGroupId(id);
        penalties.forEach(penalty -> penalty.setPenaltyGroupDetailId(null));
        penaltyGroup.setStatus(EntityStatus.DELETED);
        penaltyGroupRepository.save(penaltyGroup);
        return penaltyGroup.getId();
    }


    @Transactional
    public PenaltyGroupResponse create(PenaltyGroupCreateRequest request) {
        var penaltyGroup = new PenaltyGroup(ACTIVE);
        penaltyGroupRepository.save(penaltyGroup);
        var penaltyGroupDetails = penaltyGroupMapper.createPenaltyGroupDetails(request, penaltyGroup);
        penaltyGroupDetailsRepository.save(penaltyGroupDetails);

        Map<Long, String> penaltyNameMap = new HashMap<>();
        List<PenaltyGroupPenalty> penaltyGroupPenalties = new ArrayList<>();
        List<Long> penaltyIds = request.getPenalties();
        if (CollectionUtils.isNotEmpty(penaltyIds)) {
            checkDuplicates(penaltyIds);

            List<Penalty> availablePenalties = penaltyRepository.getAvailablePenalties(penaltyIds);
            penaltyNameMap = availablePenalties.stream().collect(Collectors.toMap(Penalty::getId, Penalty::getName));

            for (int i = 0; i < penaltyIds.size(); i++) {
                //to use in the orElseThrow method
                var penaltyId = penaltyIds.get(i);
                PenaltyGroupPenalty pg = createPenaltyGroupPenalty(penaltyGroupDetails, availablePenalties, i, penaltyId);
                penaltyGroupPenalties.add(pg);
            }

            penaltyGroupPenaltyRepository.saveAll(penaltyGroupPenalties);
            penaltyRepository.saveAll(availablePenalties);
        }

        return createPenaltyGroupResponse(penaltyGroup, penaltyGroupDetails, penaltyNameMap, penaltyGroupPenalties);
    }

    private PenaltyGroupPenalty createPenaltyGroupPenalty(PenaltyGroupDetails penaltyGroupDetails, List<Penalty> availablePenalties, int index, Long penaltyId) {
        var penalty = availablePenalties.stream().filter(p -> p.getId().equals(penaltyId)).findAny().orElseThrow(() ->
                new ClientException("penalties[%s]-penalty id %s is not available".formatted(index, penaltyId), ILLEGAL_ARGUMENTS_PROVIDED));
        penalty.setPenaltyGroupDetailId(penaltyGroupDetails.getId());
        return penaltyGroupMapper.createPenaltyGroupPenalties(penaltyGroupDetails, penaltyId);
    }

    private void checkDuplicates(List<Long> penaltyIds) {
        if (new HashSet<>(penaltyIds).size() != penaltyIds.size()) {
            throw new ClientException("penalties-list contains duplicate values", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    private PenaltyGroupResponse createPenaltyGroupResponse(PenaltyGroup penaltyGroup, PenaltyGroupDetails penaltyGroupDetails, Map<Long, String> penaltyNameMap, List<PenaltyGroupPenalty> penaltyGroupPenalties) {
        var penaltyGroupsPenaltyData = penaltyGroupPenalties.stream()
                .map(pgp -> new PenaltyGroupPenaltyDataProvider(pgp.getId(), pgp.getPenaltyId(), penaltyNameMap.get(pgp.getPenaltyId())))
                .toList();
        return penaltyGroupMapper.toResponse(penaltyGroup, penaltyGroupDetails, penaltyGroupsPenaltyData, List.of(penaltyGroupDetails));
    }

    @Transactional
    public Long update(Long id, PenaltyGroupUpdateRequest request) {

        if (!request.getUpdateExistingVersion() && request.getStartDate().isBefore(LocalDate.now())) {
            if (penaltyGroupRepository.hasLockedConnection(id) && !hasEditLockedPermission()) {
                throw new OperationNotAllowedException(String.format("id-You can’t edit the Group of penalties with id: %s because it is connected to the product contract version , Service contract version or Service order", id));
            }
        }

        var penaltyGroup = penaltyGroupRepository
                .findByIdAndStatusIn(id, List.of(ACTIVE)).orElseThrow(() ->
                        new DomainEntityNotFoundException("id-Unable to find penalty group with Id : %s".formatted(id)));

        var penaltyGroupDetails = penaltyGroupDetailsRepository
                .findByPenaltyGroupIdAndVersionId(id, request.getVersionId()).orElseThrow(() ->
                        new DomainEntityNotFoundException("id-Unable to find penalty group version with version : %s".formatted(request.getVersionId())));

        var penaltyGroupDetailsPenalties = penaltyGroupPenaltyRepository
                .findAllByPenaltyGroupDetailIdAndStatus(penaltyGroupDetails.getId(), ACTIVE);

        if (request.getUpdateExistingVersion()) {
            updateExistingVersion(penaltyGroupDetails, request, penaltyGroupDetailsPenalties);
        } else {
            createNewVersion(penaltyGroup, penaltyGroupDetails, request, penaltyGroupDetailsPenalties);
        }

        return penaltyGroup.getId();
    }

    private void createNewVersion(PenaltyGroup penaltyGroup, PenaltyGroupDetails penaltyGroupDetails, PenaltyGroupUpdateRequest request, List<PenaltyGroupPenalty> penalties) {
        validateStartDateUniqueness(penaltyGroupDetails.getPenaltyGroupId(), request.getStartDate());
        var lastVersion = penaltyGroupDetailsRepository.findLastVersionByPenaltyGroupId(penaltyGroupDetails.getPenaltyGroupId());

        var newPenaltyGroupDetails = penaltyGroupMapper.createPenaltyGroupDetails(request, penaltyGroup, lastVersion + 1);
        penaltyGroupDetailsRepository.save(newPenaltyGroupDetails);

        List<Long> penaltyIds = request.getPenalties();
        Set<Long> currentPenaltySet = penalties.stream().map(PenaltyGroupPenalty::getPenaltyId).collect(Collectors.toSet());
        List<Long> penaltiesToClone = new ArrayList<>();
        List<Long> penaltiesToCreate = new ArrayList<>();

        penaltyIds.forEach(penaltyId -> {
            if (currentPenaltySet.contains(penaltyId)) {
                penaltiesToClone.add(penaltyId);
            } else {
                penaltiesToCreate.add(penaltyId);
            }
        });

        List<PenaltyGroupPenalty> newPenaltyList = new ArrayList<>();
        cloneExistingPenalties(newPenaltyGroupDetails, penaltiesToClone, newPenaltyList);
        createNewPenalties(newPenaltyGroupDetails, penaltyIds, penaltiesToCreate, newPenaltyList);
        penaltyGroupPenaltyRepository.saveAll(newPenaltyList);
    }

    private void createNewPenalties(PenaltyGroupDetails newPenaltyGroupDetails, List<Long> penaltyIds, List<Long> penaltiesToCreate, List<PenaltyGroupPenalty> newPenaltyList) {
        List<Penalty> availablePenalties = penaltyRepository.getAvailablePenalties(penaltiesToCreate);
        for (Long penaltyId : penaltiesToCreate) {
            int index = penaltyIds.indexOf(penaltyId);
            newPenaltyList.add(createPenaltyGroupPenalty(newPenaltyGroupDetails, availablePenalties, index, penaltyId));
        }
    }

    private void cloneExistingPenalties(PenaltyGroupDetails newPenaltyGroupDetails, List<Long> penaltiesToCopy, List<PenaltyGroupPenalty> newPenaltyList) {
        penaltiesToCopy.forEach(penaltyId -> {
            var newPenalty = penaltyService.clonePenalty(penaltyId);
            newPenalty.setPenaltyGroupDetailId(newPenaltyGroupDetails.getId());
            newPenaltyList.add(penaltyGroupMapper.createPenaltyGroupPenalties(newPenaltyGroupDetails, newPenalty.getId()));
        });
    }


    private void validateStartDateUniqueness(Long penaltyGroupId, LocalDate startDate) {
        if (penaltyGroupDetailsRepository.existsByPenaltyGroupIdAndStartDate(penaltyGroupId, startDate)) {
            log.error("startDate-Start date should be unique among versions;");
            throw new ClientException("startDate-Start date should be unique among versions;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    private void updateExistingVersion(PenaltyGroupDetails penaltyGroupDetails, PenaltyGroupUpdateRequest request, List<PenaltyGroupPenalty> penalties) {
        penaltyGroupDetails.setName(request.getName());
        List<Long> penaltyIds = request.getPenalties();
        List<Penalty> availablePenalties = penaltyRepository.getAvailablePenaltiesForGroupDetail(penaltyIds, penaltyGroupDetails.getId());

        penalties.forEach(penaltyGroupPenalty -> {
            if (!penaltyIds.contains(penaltyGroupPenalty.getPenaltyId())) {
                penaltyGroupPenalty.setStatus(EntityStatus.DELETED);
                var penaltyToFree = penaltyRepository.findByIdAndStatus(penaltyGroupPenalty.getPenaltyId(), ACTIVE);
                penaltyToFree.ifPresent(penalty -> {
                    penalty.setPenaltyGroupDetailId(null);
                    penaltyRepository.save(penalty);
                });

            }
        });

        for (int i = 0; i < penaltyIds.size(); i++) {
            Long penaltyId = penaltyIds.get(i);
            if (penalties.stream().noneMatch(penaltyGroupPenalty -> penaltyGroupPenalty.getPenaltyId().equals(penaltyId))) {
                penalties.add(createPenaltyGroupPenalty(penaltyGroupDetails, availablePenalties, i, penaltyId));
            }
        }

        penaltyRepository.saveAll(availablePenalties);
        penaltyGroupPenaltyRepository.saveAll(penalties);
    }


    /**
     * Retrieves a list of penalty groups based on the search criteria
     *
     * @param request the search criteria
     * @return a page of penalty groups
     */
    public Page<PenaltyGroupListResponse> list(PenaltyGroupListRequest request) {
        String sortBy = PenaltyGroupTableColumn.GROUP_DATE_OF_CREATION.getValue();
        if (request.getSortBy() != null && StringUtils.isNotEmpty(request.getSortBy().getValue())) {
            sortBy = request.getSortBy().getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String searchBy = PenaltyGroupSearchField.ALL.getValue();
        if (request.getSearchBy() != null && StringUtils.isNotEmpty(request.getSearchBy().getValue())) {
            searchBy = request.getSearchBy().getValue();
        }

        List<String> statuses = getEntityStatuses()  // this type casting is needed for the native query to work
                .stream()
                .map(Enum::name)
                .toList();

        String excludeVersion = PenaltyGroupExcludeVersion.getExcludeVersionFromCheckBoxes(
                request.isExcludeOldVersions(), request.isExcludeFutureVersions()).getValue();

        return penaltyGroupRepository
                .list(
                        searchBy,
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        statuses,
                        excludeVersion,
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(new Sort.Order(sortDirection, sortBy))
                        )
                );

    }

    @Override
    public CopyDomainWithVersion getGroupType() {
        return CopyDomainWithVersion.PENALTY_GROUPS;
    }

    @Override
    public Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request) {
        log.debug("Finding penalty groups with prompt : %s".formatted(request.getPrompt()));
        return penaltyGroupRepository.findByCopyGroupBaseRequest(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                LocalDate.now(),
                PageRequest.of(request.getPage(), request.getSize())
        );
    }

    @Override
    public List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long groupId) {
        if (!penaltyGroupRepository.existsByIdAndStatus(groupId, ACTIVE)) {
            throw new DomainEntityNotFoundException("Unable to find active group by id : %s".formatted(groupId));
        }
        List<CopyDomainWithVersionMiddleResponse> copyGroupVersions = penaltyGroupDetailsRepository.findByCopyGroupBaseRequest(groupId).stream()
                .map(penaltyGroupDetails -> new CopyDomainWithVersionMiddleResponse(penaltyGroupDetails.getId(), penaltyGroupDetails.getVersionId().longValue(), penaltyGroupDetails.getStartDate()))
                .toList();
        return copyGroupVersions;
    }

    public PenaltyGroupResponse viewForCopy(Long id, Integer version) {
        List<EntityStatus> entityStatuses = List.of(ACTIVE);
        PenaltyGroup penaltyGroup = penaltyGroupRepository.findByIdAndStatusIn(id, entityStatuses)
                .orElseThrow(() -> new DomainEntityNotFoundException("Unable to find penalty group with given id : %s".formatted(id)));

        PenaltyGroupDetails penaltyGroupDetails = getPenaltyGroupDetails(id, version, penaltyGroup);
        List<PenaltyGroupDetails> versions = penaltyGroupDetailsRepository.findAllByPenaltyGroupIdOrderByStartDateDesc(penaltyGroup.getId());

        List<Penalty> penaltiesToCopy = penaltyRepository.findAllByPenaltyGroupDetailIdAndStatusIn(penaltyGroupDetails.getId(), entityStatuses);
        List<PenaltyGroupPenaltyDataProvider> newPenalties =
                penaltyService
                        .copyPenalties(penaltiesToCopy)
                        .stream()
                        .map(penalty -> new PenaltyGroupPenaltyDataProvider(null, penalty.getId(), penalty.getName()))
                        .toList();
        return penaltyGroupMapper.toResponse(penaltyGroup, penaltyGroupDetails, newPenalties, versions);
    }


    /**
     * Adds penalty groups to a service details
     *
     * @param penaltyGroupIds   ids of the penalty groups to be added
     * @param serviceDetails    service details to which the penalty groups will be added
     * @param exceptionMessages list of exception messages to be populated in case of errors
     */
    @Transactional
    public void addPenaltyGroupsToService(List<Long> penaltyGroupIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(penaltyGroupIds)) {
            List<ServicePenaltyGroup> tempList = new ArrayList<>();

            for (int i = 0; i < penaltyGroupIds.size(); i++) {
                Long penaltyGroupId = penaltyGroupIds.get(i);
                // being an active group is a sufficient condition for being added to a service
                Optional<PenaltyGroup> penaltyGroupOptional = penaltyGroupRepository.findByIdAndStatusIn(penaltyGroupId, List.of(ACTIVE));
                if (penaltyGroupOptional.isEmpty()) {
                    log.error("penaltyGroups[%s]-can't find Penalty Group with id: %s;".formatted(i, penaltyGroupId));
                    exceptionMessages.add("additionalSettings.penaltyGroups[%s]-can't find Penalty Group with id: %s;".formatted(i, penaltyGroupId));
                    continue;
                }

                ServicePenaltyGroup servicePenaltyGroup = new ServicePenaltyGroup();
                servicePenaltyGroup.setPenaltyGroup(penaltyGroupOptional.get());
                servicePenaltyGroup.setServiceDetails(serviceDetails);
                servicePenaltyGroup.setStatus(ServiceSubobjectStatus.ACTIVE);
                tempList.add(servicePenaltyGroup);
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalty groups
            servicePenaltyGroupRepository.saveAll(tempList);
        }
    }


    /**
     * Updates penalty groups for an existing service details
     *
     * @param requestPenaltyGroupIds ids of the penalty groups to be added
     * @param serviceDetails         service details to which the penalty groups will be added
     * @param exceptionMessages      list of exception messages to be populated in case of errors
     */
    @Transactional
    public void updateServicePenaltyGroupsForExistingVersion(List<Long> requestPenaltyGroupIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        // fetch all active penalty groups for the service
        List<ServicePenaltyGroup> dbPenaltyGroups = servicePenaltyGroupRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestPenaltyGroupIds)) {
            List<Long> dbPenaltyGroupIds = dbPenaltyGroups.stream().map(ServicePenaltyGroup::getPenaltyGroup).map(PenaltyGroup::getId).toList();
            List<ServicePenaltyGroup> tempList = new ArrayList<>();

            for (int i = 0; i < requestPenaltyGroupIds.size(); i++) {
                Long penaltyGroupId = requestPenaltyGroupIds.get(i);
                if (!dbPenaltyGroupIds.contains(penaltyGroupId)) { // if penalty group is new, its availability should be checked
                    Optional<PenaltyGroup> penaltyGroupOptional = penaltyGroupRepository.findByIdAndStatusIn(penaltyGroupId, List.of(ACTIVE));
                    if (penaltyGroupOptional.isEmpty()) {
                        log.error("penaltyGroups[%s]-can't find Penalty Group with id: %s;".formatted(i, penaltyGroupId));
                        exceptionMessages.add("additionalSettings.penaltyGroups[%s]-can't find Penalty Group with id: %s;".formatted(i, penaltyGroupId));
                        continue;
                    }

                    ServicePenaltyGroup servicePenaltyGroup = new ServicePenaltyGroup();
                    servicePenaltyGroup.setPenaltyGroup(penaltyGroupOptional.get());
                    servicePenaltyGroup.setServiceDetails(serviceDetails);
                    servicePenaltyGroup.setStatus(ServiceSubobjectStatus.ACTIVE);
                    tempList.add(servicePenaltyGroup);
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalty groups
            servicePenaltyGroupRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbPenaltyGroups)) {
            for (ServicePenaltyGroup spg : dbPenaltyGroups) {
                // if user has removed any of the penalty groups, set their status to deleted
                if (!requestPenaltyGroupIds.contains(spg.getPenaltyGroup().getId())) {
                    spg.setStatus(ServiceSubobjectStatus.DELETED);
                    servicePenaltyGroupRepository.save(spg);
                }
            }
        }
    }

    /**
     * Adds penalty groups to a product details
     *
     * @param penaltyGroupIdsSet ids of the penalty groups to be added
     * @param productDetails     product details to which the penalty groups will be added
     * @param exceptionMessages  list of exception messages to be populated in case of errors
     */
    @Transactional
    public void addPenaltyGroupsToProduct(List<Long> penaltyGroupIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(penaltyGroupIdsSet)) {
            List<Long> penaltyGroupIds = new ArrayList<>(penaltyGroupIdsSet); // this is for the sake of getting index of element when handling errors
            List<ProductPenaltyGroups> tempList = new ArrayList<>();

            for (int i = 0; i < penaltyGroupIds.size(); i++) {
                Long penaltyGroupId = penaltyGroupIds.get(i);
                // being an active group is a sufficient condition for being added to a product
                Optional<PenaltyGroup> penaltyGroupOptional = penaltyGroupRepository.findByIdAndStatusIn(penaltyGroupId, List.of(ACTIVE));
                if (penaltyGroupOptional.isEmpty()) {
                    log.error("penaltyGroupIds[%s]-can't find Penalty Group with id: %s;".formatted(i, penaltyGroupId));
                    exceptionMessages.add("additionalSettings.penaltyGroupIds[%s]-can't find Penalty Group with id: %s;".formatted(i, penaltyGroupId));
                    continue;
                }

                ProductPenaltyGroups productPenaltyGroup = new ProductPenaltyGroups();
                productPenaltyGroup.setPenaltyGroup(penaltyGroupOptional.get());
                productPenaltyGroup.setProductDetails(productDetails);
                productPenaltyGroup.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                tempList.add(productPenaltyGroup);
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalty groups
            productPenaltyGroupRepository.saveAll(tempList);
        }
    }


    /**
     * Updates penalty groups for an existing product details
     *
     * @param requestPenaltyGroupIdsSet ids of the penalty groups to be added
     * @param productDetails            product details to which the penalty groups will be added
     * @param exceptionMessages         list of exception messages to be populated in case of errors
     */
    @Transactional
    public void updateProductPenaltyGroupsForExistingVersion(List<Long> requestPenaltyGroupIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        // fetch all active penalty groups for the product
        List<ProductPenaltyGroups> dbPenaltyGroups = productPenaltyGroupRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(productDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestPenaltyGroupIdsSet)) {
            List<Long> requestPenaltyGroupIds = new ArrayList<>(requestPenaltyGroupIdsSet); // this is for the sake of getting index of element when handling errors

            List<Long> dbPenaltyGroupIds = dbPenaltyGroups.stream().map(ProductPenaltyGroups::getPenaltyGroup).map(PenaltyGroup::getId).toList();
            List<ProductPenaltyGroups> tempList = new ArrayList<>();

            for (int i = 0; i < requestPenaltyGroupIds.size(); i++) {
                Long penaltyGroupId = requestPenaltyGroupIds.get(i);
                if (!dbPenaltyGroupIds.contains(penaltyGroupId)) { // if penalty group is new, its availability should be checked
                    Optional<PenaltyGroup> penaltyGroupOptional = penaltyGroupRepository.findByIdAndStatusIn(penaltyGroupId, List.of(ACTIVE));
                    if (penaltyGroupOptional.isEmpty()) {
                        log.error("penaltyGroupIds[%s]-can't find Penalty Group with id: %s;".formatted(i, penaltyGroupId));
                        exceptionMessages.add("additionalSettings.penaltyGroupIds[%s]-can't find Penalty Group with id: %s;".formatted(i, penaltyGroupId));
                        continue;
                    }

                    ProductPenaltyGroups productPenaltyGroups = new ProductPenaltyGroups();
                    productPenaltyGroups.setPenaltyGroup(penaltyGroupOptional.get());
                    productPenaltyGroups.setProductDetails(productDetails);
                    productPenaltyGroups.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    tempList.add(productPenaltyGroups);
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalty groups
            productPenaltyGroupRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbPenaltyGroups)) {
            for (ProductPenaltyGroups productPenaltyGroups : dbPenaltyGroups) {
                // if user has removed any of the penalty groups, set their status to deleted
                if (!requestPenaltyGroupIdsSet.contains(productPenaltyGroups.getPenaltyGroup().getId())) {
                    productPenaltyGroups.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
                    productPenaltyGroupRepository.save(productPenaltyGroups);
                }
            }
        }
    }

    private boolean hasEditLockedPermission() {
        return permissionService.permissionContextContainsPermissions(PENALTY_GROUP, List.of(PermissionEnum.PENALTY_GROUP_EDIT_LOCKED));
    }

}

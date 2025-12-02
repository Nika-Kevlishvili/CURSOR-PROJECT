package bg.energo.phoenix.service.product.termination.terminationGroup;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductTerminationGroups;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceTerminationGroup;
import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroup;
import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroupDetails;
import bg.energo.phoenix.model.entity.product.termination.terminationGroup.TerminationGroupTermination;
import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.termination.terminationGroup.*;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationStatus;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.request.product.termination.terminationGroup.CreateTerminationGroupRequest;
import bg.energo.phoenix.model.request.product.termination.terminationGroup.EditTerminationGroupRequest;
import bg.energo.phoenix.model.request.product.termination.terminationGroup.TerminationGroupsListRequest;
import bg.energo.phoenix.model.request.product.termination.terminationGroup.termination.BaseTerminationGroupTerminationRequest;
import bg.energo.phoenix.model.request.product.termination.terminationGroup.termination.CreateTerminationGroupTerminationRequest;
import bg.energo.phoenix.model.request.product.termination.terminationGroup.termination.EditTerminationGroupTerminationRequest;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupListResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupResponse;
import bg.energo.phoenix.model.response.terminationGroup.TerminationGroupTerminationResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.product.product.ProductTerminationGroupsRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceTerminationGroupRepository;
import bg.energo.phoenix.repository.product.termination.terminationGroup.TerminationGroupDetailsRepository;
import bg.energo.phoenix.repository.product.termination.terminationGroup.TerminationGroupRepository;
import bg.energo.phoenix.repository.product.termination.terminationGroup.TerminationGroupTerminationsRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.group.CopyDomainWithVersionBaseService;
import bg.energo.phoenix.service.product.termination.terminations.TerminationsService;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
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

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.exception.ErrorCode.OPERATION_NOT_ALLOWED;
import static bg.energo.phoenix.permissions.PermissionContextEnum.TERMINATION_GROUP;
import static bg.energo.phoenix.permissions.PermissionEnum.TERMINATION_GROUP_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.TERMINATION_GROUP_VIEW_DELETED;

@Service
@Slf4j
@RequiredArgsConstructor
public class TerminationGroupService implements CopyDomainWithVersionBaseService {

    private final PermissionService permissionService;
    private final TerminationGroupMapper terminationGroupMapper;
    private final TerminationRepository terminationRepository;
    private final TerminationsService terminationsService;
    private final TerminationGroupRepository terminationGroupRepository;
    private final TerminationGroupDetailsRepository terminationGroupDetailsRepository;
    private final TerminationGroupTerminationsRepository terminationGroupTerminationsRepository;
    private final ServiceTerminationGroupRepository serviceTerminationGroupRepository;
    private final ProductTerminationGroupsRepository productTerminationGroupsRepository;

    /**
     * Creates a new termination group with the provided request.
     *
     * @param request the {@link CreateTerminationGroupRequest} object containing the details of the termination group to be created.
     * @return the ID of the created {@link TerminationGroup}.
     * @throws ClientException               if any of the arguments provided are invalid.
     * @throws DomainEntityNotFoundException if the active termination with the specified ID is not found.
     */
    @Transactional
    public Long create(CreateTerminationGroupRequest request) {
        log.debug("Creating termination group with request: {}", request);

        TerminationGroup group = new TerminationGroup();
        group.setStatus(TerminationGroupStatus.ACTIVE);
        terminationGroupRepository.saveAndFlush(group);

        TerminationGroupDetails details = createTerminationGroupNewVersion(
                group.getId(),
                request.getName().trim(),
                1L,
                LocalDate.now()
        );

        if (CollectionUtils.isNotEmpty(request.getTerminationsList())) {
            List<CreateTerminationGroupTerminationRequest> terminationsList = request.getTerminationsList();
            if (EPBListUtils.notAllUnique(terminationsList)) {
                log.error("Terminations list contains duplicate values");
                throw new ClientException("terminationsList-Terminations list contains duplicate values", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            List<Long> terminationIds = terminationsList
                    .stream()
                    .map(BaseTerminationGroupTerminationRequest::getTerminationId)
                    .toList();

            List<Long> availableTerminationsIds = terminationRepository
                    .getAvailableTerminationsIn(terminationIds)
                    .stream()
                    .map(Termination::getId)
                    .toList();

            for (int i = 0; i < terminationsList.size(); i++) {
                CreateTerminationGroupTerminationRequest req = terminationsList.get(i);
                createTerminationGroupTermination(availableTerminationsIds, req.getTerminationId(), details.getId(), i);
            }
        }

        return group.getId();
    }


    /**
     * Edits existing version of a termination group with the provided request and ID, or creates a new version.
     *
     * @param request the edit request containing the updated termination group details
     * @param groupId the ID of the {@link TerminationGroup} to be edited
     * @return the ID of the edited {@link TerminationGroup}
     * @throws DomainEntityNotFoundException if the termination group or its details are not found
     * @throws ClientException               if the provided arguments are invalid
     */
    @Transactional
    public Long edit(EditTerminationGroupRequest request, Long groupId) {
        log.info("Editing termination group with request: {}", request);

        if (!request.getUpdateExistingVersion() && request.getStartDate().isBefore(LocalDate.now())) {
            if (terminationGroupRepository.hasLockedConnection(groupId) && !hasEditLockedPermission()) {
                throw new OperationNotAllowedException(String.format("id-You can’t edit the Group of terminations with id: %s because it is connected to the product contract version , Service contract version or Service order", groupId));
            }
        }

        // not to produce null pointer exceptions
        if (CollectionUtils.isEmpty(request.getTerminationsList())) {
            request.setTerminationsList(Collections.emptyList());
        }

        if (!terminationGroupRepository.existsByIdAndStatusIn(groupId, List.of(TerminationGroupStatus.ACTIVE))) {
            log.error("Active termination group not found by ID %s".formatted(groupId));
            throw new DomainEntityNotFoundException("groupId-Active termination group not found by ID %s".formatted(groupId));
        }

        TerminationGroupDetails currVersion = terminationGroupDetailsRepository
                .findByTerminationGroupIdAndVersionId(groupId, request.getVersionId())
                .orElseThrow(() -> new DomainEntityNotFoundException("versionId-Termination group details not found by version ID %s".formatted(request.getVersionId())));

        List<Long> requestTerminationIds = request.getTerminationsList()
                .stream()
                .map(EditTerminationGroupTerminationRequest::getTerminationId)
                .toList();

        checkAgainstDuplicateTerminationValues(requestTerminationIds);

        List<EditTerminationGroupTerminationRequest> requestTerminations = request.getTerminationsList();

        if (request.getUpdateExistingVersion()) {
            currVersion.setName(request.getName().trim());

            // retrieve active terminations attached to this version
            List<TerminationGroupTermination> dbActiveTGTs = terminationGroupTerminationsRepository
                    .findByTerminationGroupVersionAndStatusIn(
                            groupId,
                            request.getVersionId(),
                            List.of(TerminationGroupTerminationStatus.ACTIVE)
                    );

            // such order is important as, for example, the user can delete termination A (which was already persisted)
            // and add the same termination A again (ID will be null). So, first the deleted terminations should be released
            // and only then should be fetched the available terminations.
            removeDeletedTerminationGroupTerminations(
                    dbActiveTGTs,
                    request.getTerminationsList().stream()
                            .map(EditTerminationGroupTerminationRequest::getId)
                            .filter(Objects::nonNull).toList()
            );

            // retrieve available terminations filtered by the IDs provided in the request
            List<Long> availableTerminationIds = terminationRepository
                    .getAvailableTerminationsIn(requestTerminationIds)
                    .stream()
                    .map(Termination::getId)
                    .toList();

            // process new terminations only
            for (int i = 0; i < requestTerminations.size(); i++) {
                EditTerminationGroupTerminationRequest req = requestTerminations.get(i);
                if (req.getId() == null) {
                    createTerminationGroupTermination(availableTerminationIds, req.getTerminationId(), currVersion.getId(), i);
                }
            }
        } else {
            validateStartDateUniqueness(groupId, request.getStartDate());
            TerminationGroupDetails newVersion = createTerminationGroupNewVersion(
                    groupId,
                    request.getName().trim(),
                    terminationGroupDetailsRepository.findLastVersionByTerminationGroupId(groupId) + 1,
                    request.getStartDate()
            );

            // retrieve available terminations filtered by the IDs provided in the request
            List<Long> availableTerminationIds = terminationRepository
                    .getAvailableTerminationsIn(requestTerminationIds)
                    .stream()
                    .map(Termination::getId)
                    .toList();

            for (int i = 0; i < requestTerminations.size(); i++) {
                EditTerminationGroupTerminationRequest req = requestTerminations.get(i);
                if (req.getId() == null) {
                    // if the newly added termination is same as in the parent version (user deleted persisted one but added the same again anew),
                    // it should be cloned as the termination is still active in parent version and new TGT should be created
                    if (terminationGroupTerminationsRepository.existsByTerminationIdAndStatus(req.getTerminationId(), TerminationGroupTerminationStatus.ACTIVE)) {
                        Termination clonedTermination = terminationsService.cloneTermination(req.getTerminationId());
                        createTerminationGroupTermination(List.of(clonedTermination.getId()), clonedTermination.getId(), newVersion.getId(), i);
                    } else {
                        createTerminationGroupTermination(availableTerminationIds, req.getTerminationId(), newVersion.getId(), i);
                    }
                } else {
                    // if the termination is the same that is in the parent version (in that case ID will not be null), should clone termination
                    Termination clonedTermination = terminationsService.cloneTermination(req.getTerminationId());
                    createTerminationGroupTermination(List.of(clonedTermination.getId()), clonedTermination.getId(), newVersion.getId(), i);
                }
            }
        }

        return groupId;
    }


    /**
     * Checks if the provided list of {@link Termination} ids contains duplicate values.
     *
     * @param terminationIds list of {@link Termination} ids that needs to be validated.
     */
    private void checkAgainstDuplicateTerminationValues(List<Long> terminationIds) {
        if (EPBListUtils.notAllUnique(terminationIds)) {
            log.error("Terminations list contains duplicate values");
            throw new ClientException("terminationsList-Terminations list contains duplicate values", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    /**
     * Creates a new {@link TerminationGroupTermination} for the provided {@link TerminationGroupDetails}
     *
     * @param availableTerminationIds  list of available {@link Termination} ids
     * @param requestedTerminationId   requested {@link Termination} id
     * @param terminationGroupDetailId {@link TerminationGroupDetails} id
     * @param index                    index of the termination in the termination group terminations list
     */
    private void createTerminationGroupTermination(List<Long> availableTerminationIds,
                                                   Long requestedTerminationId,
                                                   Long terminationGroupDetailId,
                                                   int index) {
        // check if requested termination is available
        if (!availableTerminationIds.contains(requestedTerminationId)) {
            log.error("Termination ID {} is not available", requestedTerminationId);
            throw new ClientException("terminationsList[%s].terminationId-Termination ID %s is not available"
                    .formatted(index, requestedTerminationId), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        // create new TerminationGroupTermination for this version
        TerminationGroupTermination tgt = new TerminationGroupTermination();
        tgt.setStatus(TerminationGroupTerminationStatus.ACTIVE);
        tgt.setTerminationId(requestedTerminationId);
        tgt.setTerminationGroupDetailId(terminationGroupDetailId);
        terminationGroupTerminationsRepository.saveAndFlush(tgt);

        markTerminationAsUnavailable(index, requestedTerminationId, terminationGroupDetailId);
    }


    /**
     * Sets provided {@link TerminationGroupDetails} ID to the {@link Termination}, thus marking it as unavailable
     *
     * @param terminationId            ID of a {@link Termination} that should be updated
     * @param terminationGroupDetailId ID of a {@link TerminationGroupDetails} to which should be linked the {@link Termination}
     */
    private void markTerminationAsUnavailable(int index, Long terminationId, Long terminationGroupDetailId) {
        Termination termination = terminationRepository
                .findById(terminationId)
                .orElseThrow(() -> new DomainEntityNotFoundException("terminationsList[%s].terminationId-Termination not found by ID %s".formatted(index, terminationId)));
        termination.setTerminationGroupDetailId(terminationGroupDetailId);
        terminationRepository.saveAndFlush(termination);
    }


    /**
     * Validates if the provided start date is unique among the versions of the provided {@link TerminationGroup}
     *
     * @param terminationGroupId ID of a {@link TerminationGroup} among the versions of which the start date should be unique
     * @param startDate          Date that should be checked for uniqueness
     */
    private void validateStartDateUniqueness(Long terminationGroupId, LocalDate startDate) {
        Optional<TerminationGroupDetails> detailsOptional = terminationGroupDetailsRepository
                .findByTerminationGroupIdAndStartDate(terminationGroupId, startDate);
        if (detailsOptional.isPresent()) {
            log.error("startDate-Start date should be unique among versions;");
            throw new ClientException("startDate-Start date should be unique among versions;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    /**
     * Creates and saves a new version of a {@link TerminationGroup}
     *
     * @param terminationGroupId {@link TerminationGroup} to which belongs the new version
     * @param name               Name of the new version
     * @param versionId          Version number
     * @param startDate          Date from which the version becomes 'current'
     * @return created {@link TerminationGroupDetails}
     */
    private TerminationGroupDetails createTerminationGroupNewVersion(Long terminationGroupId,
                                                                     String name,
                                                                     Long versionId,
                                                                     LocalDate startDate) {
        TerminationGroupDetails newVersion = new TerminationGroupDetails();
        newVersion.setName(name);
        newVersion.setStartDate(startDate);
        newVersion.setTerminationGroupId(terminationGroupId);
        newVersion.setVersionId(versionId);
        return terminationGroupDetailsRepository.saveAndFlush(newVersion);
    }


    /**
     * Loops over persisted list of ACTIVE {@link TerminationGroupTermination} entities and sets DELETED status
     * if attached termination ID not found in the termination ID list provided in the request.
     * Updates released {@link Termination} as available.
     */
    private void removeDeletedTerminationGroupTerminations(List<TerminationGroupTermination> dbActiveTGTs,
                                                           List<Long> providedTGTIds) {
        for (TerminationGroupTermination tgt : dbActiveTGTs) {
            if (!providedTGTIds.contains(tgt.getId())) {
                tgt.setStatus(TerminationGroupTerminationStatus.DELETED);
                terminationGroupTerminationsRepository.saveAndFlush(tgt);

                Termination termination = terminationRepository
                        .findById(tgt.getTerminationId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("terminationsList[%s].terminationId-Termination not found by ID ".formatted(tgt.getTerminationId())));
                termination.setTerminationGroupDetailId(null);
                terminationRepository.saveAndFlush(termination);
            }
        }
    }


    /**
     * Retrieves a preview of a termination group based on the given group ID and version.
     * If version is not specified, current termination details are returned.
     *
     * @param groupId the ID of the termination group details to retrieve
     * @param version the version of the termination group to retrieve, or null to retrieve the current version
     * @return a {@link TerminationGroupResponse} object containing details of the requested termination group
     * @throws DomainEntityNotFoundException if the termination group or its details cannot be found based on the given parameters
     */
    public TerminationGroupResponse preview(Long groupId, Long version) {
        log.info("Fetching group details by ID: {}", groupId);

        TerminationGroupDetails details;
        if (version == null) {
            details = terminationGroupDetailsRepository
                    .findFirstByTerminationGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(groupId, LocalDate.now())
                    .orElseThrow(() -> new DomainEntityNotFoundException("groupId-Current details version not found by group ID %s;".formatted(groupId)));
        } else {
            details = terminationGroupDetailsRepository
                    .findByTerminationGroupIdAndVersionId(groupId, version)
                    .orElseThrow(() -> new DomainEntityNotFoundException("groupId-Version %s details not found by group ID %s;".formatted(groupId, version)));
        }

        TerminationGroup terminationGroup = terminationGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new DomainEntityNotFoundException("status-Group not found by ID: " + details.getTerminationGroupId()));

        List<TerminationGroupTerminationResponse> groupTerminations = terminationGroupTerminationsRepository
                .findByTerminationGroupDetailIdAndStatusIn(details.getId(), List.of(TerminationGroupTerminationStatus.ACTIVE));

        groupTerminations.forEach(r -> r.setTerminationName(r.getTerminationName() + " (" + r.getTerminationId() + ")"));

        TerminationGroupResponse terminationGroupResponse = terminationGroupMapper.terminationGroupResponseFromEntity(
                terminationGroup,
                details,
                terminationGroupDetailsRepository.getTerminationGroupVersions(details.getTerminationGroupId()),
                groupTerminations);
        terminationGroupResponse.setIsLocked(terminationGroupRepository.hasLockedConnection(groupId));
        if (details.getStartDate().isAfter(LocalDate.now())) {
            terminationGroupResponse.setIsLocked(false);
        }
        return terminationGroupResponse;
    }


    /**
     * Deletes a termination group with the provided ID if all the conditions are met.
     *
     * @param id the ID of the termination group to be deleted
     * @return the ID of the deleted termination group
     * @throws DomainEntityNotFoundException if the termination group is not found
     * @throws ClientException               if the termination group is already deleted or connected to a product
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting group of termination by ID {}", id);

        TerminationGroup group = terminationGroupRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Termination group not found by ID %s".formatted(id)));

        if (group.getStatus().equals(TerminationGroupStatus.DELETED)) {
            log.error("Termination group is already deleted, ID {}", id);
            throw new ClientException("id-Termination group is already deleted", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (terminationGroupRepository.hasConnectionToProduct(id, List.of(ProductStatus.ACTIVE), List.of(ProductSubObjectStatus.ACTIVE))) {
            log.error("id-You can’t delete the group of termination with ID [%s] because it is connected to the product".formatted(id));
            throw new ClientException("id-You can’t delete the group of termination with ID [%s] because it is connected to the product".formatted(id), OPERATION_NOT_ALLOWED);
        }

        if (terminationGroupRepository.hasConnectionToService(id, List.of(ServiceStatus.ACTIVE), List.of(ServiceSubobjectStatus.ACTIVE))) {
            log.error("id-You can’t delete the group of termination with ID [%s] because it is connected to the service".formatted(id));
            throw new ClientException("id-You can’t delete the group of termination with ID [%s] because it is connected to the service".formatted(id), OPERATION_NOT_ALLOWED);
        }

        group.setStatus(TerminationGroupStatus.DELETED);
        terminationGroupRepository.save(group);

        List<Termination> connectedTerminations = terminationRepository.getConnectedActiveTerminationsByTerminationGroupId(group.getId());
        connectedTerminations.forEach(t -> t.setTerminationGroupDetailId(null));
        terminationRepository.saveAll(connectedTerminations);

        return group.getId();
    }


    /**
     * Returns a page of {@link TerminationGroupListResponse} based on the provided search criteria.
     *
     * @param request the search criteria to use when fetching the termination group details
     * @return a page of {@link TerminationGroupListResponse} that match the search criteria
     */
    public Page<TerminationGroupListResponse> list(TerminationGroupsListRequest request) {
        log.debug("Fetching termination groups list for the following request: {}", request);

        String sortBy = TerminationGroupTableColumn.TG_DATE_OF_CREATION.getValue();
        if (request.getSortBy() != null && StringUtils.isNotEmpty(request.getSortBy().getValue())) {
            sortBy = request.getSortBy().getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String searchBy = TerminationGroupSearchField.ALL.getValue();
        if (request.getSearchBy() != null && StringUtils.isNotEmpty(request.getSearchBy().getValue())) {
            searchBy = request.getSearchBy().getValue();
        }

        List<TerminationGroupStatus> terminationGroupStatuses = new ArrayList<>();
        if (permissionService.getPermissionsFromContext(TERMINATION_GROUP).contains(TERMINATION_GROUP_VIEW_DELETED.getId())) {
            terminationGroupStatuses.add(TerminationGroupStatus.DELETED);
        }

        if (permissionService.getPermissionsFromContext(TERMINATION_GROUP).contains(TERMINATION_GROUP_VIEW_BASIC.getId())) {
            terminationGroupStatuses.add(TerminationGroupStatus.ACTIVE);
        }

        List<String> statuses = terminationGroupStatuses  // this type casting is needed for the native query to work
                .stream()
                .map(Enum::name)
                .toList();

        String excludeVersion = TerminationGroupExcludeVersion.getExcludeVersionFromCheckBoxes(
                request.isExcludeOldVersions(), request.isExcludeFutureVersions()).getValue();

        return terminationGroupRepository
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
        return CopyDomainWithVersion.TERMINATION_GROUPS;
    }


    @Override
    public Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request) {
        log.debug("Fetching termination groups list for the following request: {}", request);
        return terminationGroupRepository
                .findByCopyGroupBaseRequest(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        LocalDate.now(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(Sort.Direction.DESC, "id")
                        )
                );
    }


    @Override
    public List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long groupId) {
        return terminationGroupDetailsRepository.findByCopyGroupBaseRequest(groupId);
    }


    @Transactional
    public TerminationGroupResponse viewWithCopy(Long id, Long version) {
        TerminationGroup terminationGroups = terminationGroupRepository
                .findByIdAndStatusIn(id, List.of(TerminationGroupStatus.ACTIVE))
                .orElseThrow(() -> new ClientException("Termination not found with this id: " + id + " Or you dont have permission", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        TerminationGroupDetails details = terminationGroupDetailsRepository
                .findByVersionIdAndTerminationGroupId(version, id)
                .orElseThrow(() -> new ClientException("Termination group with id: " + id + " and version: " + version + " not found", ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        // terminations belonging to the version being copied, should be cloned
        List<Termination> persistedTermination = terminationRepository
                .findByTerminationGroupDetailIdAndStatusIn(details.getId(), List.of(TerminationStatus.ACTIVE));

        List<Termination> clonedTerminations = persistedTermination
                .stream()
                .map(t -> terminationsService.cloneTermination(t.getId()))
                .toList();

        List<TerminationGroupTerminationResponse> responseList = clonedTerminations
                .stream()
                .map(x -> new TerminationGroupTerminationResponse(
                        null,
                        x.getId(),
                        x.getName() + " (" + x.getId() + ")")
                )
                .toList();

        return terminationGroupMapper.terminationGroupResponseFromEntity(
                terminationGroups,
                details,
                terminationGroupDetailsRepository.getTerminationGroupVersions(details.getTerminationGroupId()),
                responseList
        );
    }


    /**
     * Adds termination groups to a service details
     *
     * @param terminationGroupIds termination group ids to create
     * @param serviceDetails      service details to add termination groups
     * @param exceptionMessages   exception messages to be populated in case of errors
     */
    public void addTerminationGroupsToService(List<Long> terminationGroupIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(terminationGroupIds)) {
            List<ServiceTerminationGroup> tempList = new ArrayList<>();

            for (int i = 0; i < terminationGroupIds.size(); i++) {
                Long terminationGroupId = terminationGroupIds.get(i);
                // being an active group is a sufficient condition for adding it to the service
                Optional<TerminationGroup> terminationGroupOptional = terminationGroupRepository.findByIdAndStatusIn(terminationGroupId, List.of(TerminationGroupStatus.ACTIVE));
                if (terminationGroupOptional.isEmpty()) {
                    log.error("terminationGroups[%s]-Termination group with ID %s not found;".formatted(i, terminationGroupId));
                    exceptionMessages.add("additionalSettings.terminationGroups[%s]-Termination group with ID %s not found;".formatted(i, terminationGroupId));
                    continue;
                }

                ServiceTerminationGroup stg = new ServiceTerminationGroup();
                stg.setTerminationGroup(terminationGroupOptional.get());
                stg.setServiceDetails(serviceDetails);
                stg.setStatus(ServiceSubobjectStatus.ACTIVE);
                tempList.add(stg);
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all termination groups
            serviceTerminationGroupRepository.saveAll(tempList);
        }
    }


    /**
     * Updates the termination groups of a service for an existing version
     *
     * @param requestTerminationGroupIds the termination group ids to update
     * @param details                    the service details
     * @param exceptionMessages          the exception messages to be populated in case of errors
     */
    @Transactional
    public void updateServiceTerminationGroupsForExistingVersion(List<Long> requestTerminationGroupIds, ServiceDetails details, List<String> exceptionMessages) {
        // fetch all active termination groups for service
        List<ServiceTerminationGroup> dbTerminationGroups = serviceTerminationGroupRepository
                .findByServiceDetailsIdAndStatusIn(details.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestTerminationGroupIds)) {
            List<Long> dbTerminationGroupIds = dbTerminationGroups.stream().map(ServiceTerminationGroup::getTerminationGroup).map(TerminationGroup::getId).toList();
            List<ServiceTerminationGroup> tempList = new ArrayList<>();

            for (int i = 0; i < requestTerminationGroupIds.size(); i++) {
                Long terminationGroupId = requestTerminationGroupIds.get(i);
                if (!dbTerminationGroupIds.contains(terminationGroupId)) { // if termination group is new, its availability should be checked
                    Optional<TerminationGroup> terminationGroupOptional = terminationGroupRepository.findByIdAndStatusIn(terminationGroupId, List.of(TerminationGroupStatus.ACTIVE));
                    if (terminationGroupOptional.isEmpty()) {
                        log.error("terminationGroups[%s]-Termination group with ID %s not found;".formatted(i, terminationGroupId));
                        exceptionMessages.add("additionalSettings.terminationGroups[%s]-Termination group with ID %s not found;".formatted(i, terminationGroupId));
                        continue;
                    }

                    ServiceTerminationGroup stg = new ServiceTerminationGroup();
                    stg.setTerminationGroup(terminationGroupOptional.get());
                    stg.setServiceDetails(details);
                    stg.setStatus(ServiceSubobjectStatus.ACTIVE);
                    tempList.add(stg);
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new termination groups
            serviceTerminationGroupRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbTerminationGroups)) {
            for (ServiceTerminationGroup dbTerminationGroup : dbTerminationGroups) {
                // if user has removed a termination group, it should be deleted
                if (!requestTerminationGroupIds.contains(dbTerminationGroup.getTerminationGroup().getId())) {
                    dbTerminationGroup.setStatus(ServiceSubobjectStatus.DELETED);
                    serviceTerminationGroupRepository.save(dbTerminationGroup);
                }
            }
        }
    }

    /**
     * Adds termination groups to a product details
     *
     * @param terminationGroupIdsSet termination group ids to create
     * @param productDetails         product details to add termination groups
     * @param exceptionMessages      exception messages to be populated in case of errors
     */
    public void addTerminationGroupsToProduct(List<Long> terminationGroupIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(terminationGroupIdsSet)) {
            List<Long> terminationGroupIds = new ArrayList<>(terminationGroupIdsSet); // this is for the sake of getting index of element when handling errors
            List<ProductTerminationGroups> tempList = new ArrayList<>();

            for (int i = 0; i < terminationGroupIds.size(); i++) {
                Long terminationGroupId = terminationGroupIds.get(i);
                // being an active group is a sufficient condition for adding it to the product
                Optional<TerminationGroup> terminationGroupOptional = terminationGroupRepository.findByIdAndStatusIn(terminationGroupId, List.of(TerminationGroupStatus.ACTIVE));
                if (terminationGroupOptional.isEmpty()) {
                    log.error("terminationGroupIds[%s]-Termination group with ID %s not found;".formatted(i, terminationGroupId));
                    exceptionMessages.add("additionalSettings.terminationGroupIds[%s]-Termination group with ID %s not found;".formatted(i, terminationGroupId));
                    continue;
                }

                ProductTerminationGroups productTerminationGroups = new ProductTerminationGroups();
                productTerminationGroups.setTerminationGroup(terminationGroupOptional.get());
                productTerminationGroups.setProductDetails(productDetails);
                productTerminationGroups.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                tempList.add(productTerminationGroups);
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all termination groups
            productTerminationGroupsRepository.saveAll(tempList);
        }
    }


    /**
     * Updates the termination groups of a product for an existing version
     *
     * @param requestTerminationGroupIdsSet the termination group ids to update
     * @param details                       the product details
     * @param exceptionMessages             the exception messages to be populated in case of errors
     */
    @Transactional
    public void updateProductTerminationGroupsForExistingVersion(List<Long> requestTerminationGroupIdsSet, ProductDetails details, List<String> exceptionMessages) {
        // fetch all active termination groups for product
        List<ProductTerminationGroups> dbTerminationGroups = productTerminationGroupsRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(details.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestTerminationGroupIdsSet)) {
            List<Long> requestTerminationGroupIds = new ArrayList<>(requestTerminationGroupIdsSet); // this is for the sake of getting index of element when handling errors
            List<Long> dbTerminationGroupIds = dbTerminationGroups.stream().map(ProductTerminationGroups::getTerminationGroup).map(TerminationGroup::getId).toList();
            List<ProductTerminationGroups> tempList = new ArrayList<>();

            for (int i = 0; i < requestTerminationGroupIds.size(); i++) {
                Long terminationGroupId = requestTerminationGroupIds.get(i);
                if (!dbTerminationGroupIds.contains(terminationGroupId)) { // if termination group is new, its availability should be checked
                    Optional<TerminationGroup> terminationGroupOptional = terminationGroupRepository.findByIdAndStatusIn(terminationGroupId, List.of(TerminationGroupStatus.ACTIVE));
                    if (terminationGroupOptional.isEmpty()) {
                        log.error("terminationGroupIds[%s]-Termination group with ID %s not found;".formatted(i, terminationGroupId));
                        exceptionMessages.add("additionalSettings.terminationGroupIds[%s]-Termination group with ID %s not found;".formatted(i, terminationGroupId));
                        continue;
                    }

                    ProductTerminationGroups productTerminationGroups = new ProductTerminationGroups();
                    productTerminationGroups.setTerminationGroup(terminationGroupOptional.get());
                    productTerminationGroups.setProductDetails(details);
                    productTerminationGroups.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    tempList.add(productTerminationGroups);
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new termination groups
            productTerminationGroupsRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbTerminationGroups)) {
            for (ProductTerminationGroups dbTerminationGroup : dbTerminationGroups) {
                // if user has removed a termination group, it should be deleted
                if (!requestTerminationGroupIdsSet.contains(dbTerminationGroup.getTerminationGroup().getId())) {
                    dbTerminationGroup.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
                    productTerminationGroupsRepository.save(dbTerminationGroup);
                }
            }
        }
    }

    private boolean hasEditLockedPermission() {
        return permissionService.permissionContextContainsPermissions(TERMINATION_GROUP, List.of(PermissionEnum.TERMINATION_GROUP_EDIT_LOCKED));
    }

}

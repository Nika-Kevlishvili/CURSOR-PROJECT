package bg.energo.phoenix.service.product.term.termsGroup;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.product.term.terms.Terms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermGroupDetails;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroupTerms;
import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroups;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.term.terms.ContractEntryIntoForce;
import bg.energo.phoenix.model.enums.product.term.terms.PaymentTermStatus;
import bg.energo.phoenix.model.enums.product.term.terms.StartOfContractInitialTerm;
import bg.energo.phoenix.model.enums.product.term.terms.TermStatus;
import bg.energo.phoenix.model.enums.product.term.terms.filter.TermsParameterFilterField;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermsGroupListColumns;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.request.product.term.termsGroup.TermsGroupCreateRequest;
import bg.energo.phoenix.model.request.product.term.termsGroup.TermsGroupEditRequest;
import bg.energo.phoenix.model.request.product.term.termsGroup.TermsGroupListRequest;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupListingResponse;
import bg.energo.phoenix.model.response.termsGroup.TermsGroupViewResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.repository.product.term.terms.InvoicePaymentTermsRepository;
import bg.energo.phoenix.repository.product.term.terms.TermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupDetailsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupTermsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.TermsGroupsRepository;
import bg.energo.phoenix.repository.product.term.termsGroups.mappers.TermsGroupMapper;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.group.CopyDomainWithVersionBaseService;
import bg.energo.phoenix.service.product.term.terms.InvoicePaymentTermsService;
import bg.energo.phoenix.service.product.term.terms.TermsService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.TERMS_GROUP;
import static bg.energo.phoenix.permissions.PermissionEnum.TERMS_GROUP_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.TERMS_GROUP_VIEW_DELETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermsGroupService implements CopyDomainWithVersionBaseService {
    private final TermsGroupsRepository termsGroupsRepository;
    private final TermsGroupDetailsRepository termsGroupDetailsRepository;
    private final TermsService termsService;
    private final TermsRepository termsRepository;
    private final InvoicePaymentTermsService invoicePaymentTermsService;
    private final PermissionService permissionService;
    private final TermsGroupMapper termsGroupMapper;
    private final TermsGroupTermsRepository termsGroupTermsRepository;
    private final CalendarRepository calendarRepository;
    private final InvoicePaymentTermsRepository invoicePaymentTermsRepository;


    /**
     * Creates a new terms group and returns its id
     *
     * @param request request object containing the terms group data
     * @return the id of the newly created terms group
     */
    @Transactional
    public Long create(TermsGroupCreateRequest request) {
        log.debug("Creating terms group with request: {}", request);
        Terms term = termsRepository
                .findById(request.getTermsId())
                .orElseThrow(() -> new DomainEntityNotFoundException("termId-Term with ID [%s] not found".formatted(request.getTermsId())));

        validateTerms(term.getId());

        TermsGroups termsGroups = new TermsGroups();
        termsGroups.setStatus(TermGroupStatus.ACTIVE);
        termsGroupsRepository.saveAndFlush(termsGroups);

        TermGroupDetails details = createTermGroupDetails(termsGroups.getId(), 1L, request.getName(), LocalDateTime.now());
        termsGroups.setLastGroupDetailsId(details.getId());
        termsGroupsRepository.saveAndFlush(termsGroups);

        attachTermToVersion(term, details);

        return termsGroups.getId();
    }


    /**
     * Validates the Terms provided in the request to be qualified for a new terms group
     *
     * @param termsId the id of the terms object to be validated
     */
    private void validateTerms(Long termsId) {
        Terms terms = termsRepository
                .findByIdAndStatusIn(termsId, List.of(TermStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException(String.format("termsId-Active term not found with id: %s ;", termsId)));

        List<InvoicePaymentTermsResponse> invoicePaymentTerms = invoicePaymentTermsService
                .findDetailedByTermIdAndStatuses(terms.getId(), List.of(PaymentTermStatus.ACTIVE));

        if (terms.getGroupDetailId() != null) {
            log.error("id-{} {} is Already assigned to the group detail with id {}", termsId, terms.getName(), terms.getGroupDetailId());
            throw new ClientException(String.format("id-%s %s is Already assigned to the group detail with id %s;", termsId, terms.getName(), terms.getGroupDetailId()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (invoicePaymentTerms.size() != 1) {
            log.error("id-Terms with ID [%s] and name [%s] should have only one Invoice Payment Term;".formatted(termsId, terms.getName()));
            throw new ClientException("id-Terms with ID [%s] and name [%s] should have only one Invoice Payment Term;".formatted(termsId, terms.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        InvoicePaymentTermsResponse invoicePaymentTermsResponse = invoicePaymentTerms.get(0);
        if (invoicePaymentTermsResponse.getValueTo() != null || invoicePaymentTermsResponse.getValueFrom() != null) {
            log.error("id-Terms with ID [%s] and name [%s] with value ranges can’t be added to group;".formatted(termsId, terms.getName()));
            throw new ClientException("id-Terms with ID [%s] and name [%s] with value ranges can’t be added to group;".formatted(termsId, terms.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (invoicePaymentTermsResponse.getValue() == null) {
            log.error("id-Term with ID [%s] and name [%s] without value can’t be added to group;".formatted(termsId, terms.getName()));
            throw new ClientException("id-Term with ID [%s] and name [%s] without value can’t be added to group;".formatted(termsId, terms.getName()), ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    /**
     * Returns the requested terms group with the latest version if not specified otherwise
     *
     * @param id      the terms group id
     * @param version the terms group version
     * @return the terms group view response
     */
    public TermsGroupViewResponse view(Long id, Long version) {
        log.debug("Viewing Terms groups with id: {}", id);

        TermsGroups termsGroups = termsGroupsRepository
                .findByIdAndStatusIn(id, getStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Term not found with this id: " + id + " Or you dont have permission"));

        TermGroupDetails termGroupDetails;
        if (version != null) {
            termGroupDetails = termsGroupDetailsRepository
                    .findByGroupIdAndVersionId(id, version)
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Terms group with id: %s and version: %s not found".formatted(id, version)));
        } else { // if version not specified, fetch the current version
            termGroupDetails = termsGroupDetailsRepository
                    .findFirstByGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(id, LocalDateTime.now())
                    .orElseThrow(() -> new DomainEntityNotFoundException("id-Current version of terms group with id: %s not found".formatted(id)));
        }

        List<TermGroupDetails> versions = termsGroupDetailsRepository
                .findByGroupIdOrderByStartDateAsc(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Terms group versions with id: " + id + " not found"));

        TermsGroupTerms termsGroupTerms = termsGroupTermsRepository
                .findByTermGroupDetailIdAndTermGroupStatusIn(termGroupDetails.getId(), List.of(TermGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Cannot find active term connection for detail ID [%s]".formatted(termGroupDetails.getId())));

        Terms term = termsRepository
                .findById(termsGroupTerms.getTermId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Can't find Term by id: " + termsGroupTerms.getTermId()));

        TermsGroupViewResponse termsGroupViewResponse = termsGroupMapper.termsGroupViewResponseMap(
                termsGroups,
                termGroupDetails,
                term,
                versions.stream().map(termsGroupMapper::termsVersionsMap).collect(Collectors.toList()));
        termsGroupViewResponse.setIsLocked(termsGroupsRepository.hasLockedConnection(id));
        if (termGroupDetails.getStartDate().isAfter(LocalDateTime.now())) {
            termsGroupViewResponse.setIsLocked(false);
        }
        return termsGroupViewResponse;
    }


    /**
     * Returns the list of statuses that the user has permission to view
     *
     * @return the list of statuses
     */
    private List<TermGroupStatus> getStatuses() {
        List<String> context = permissionService.getPermissionsFromContext(TERMS_GROUP);
        List<TermGroupStatus> permissionList = new ArrayList<>();
        if (context.contains(TERMS_GROUP_VIEW_DELETED.getId())) {
            permissionList.add(TermGroupStatus.DELETED);
        }
        if (context.contains(TERMS_GROUP_VIEW_BASIC.getId())) {
            permissionList.add(TermGroupStatus.ACTIVE);
        }
        return permissionList;
    }


    /**
     * Deletes entity if all the validations are passed and releases terms.
     *
     * @param id ID of the {@link TermsGroups} that needs to be deleted
     * @return ID of the deleted {@link TermsGroups}
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting terms group with id: {}", id);

        TermsGroups termsGroups = termsGroupsRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Terms Group with ID %s not found;".formatted(id)));

        if (termsGroups.getStatus().equals(TermGroupStatus.DELETED)) {
            log.error("id-Terms Group with ID [%s] is already deleted;".formatted(id));
            throw new ClientException("id-Terms Group with ID [%s] is already deleted;".formatted(id), ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (termsGroupsRepository.hasConnectionToProduct(id, List.of(ProductStatus.ACTIVE))) {
            log.error("id-You can’t delete the group of term with ID [%s] because it is connected to the product".formatted(id));
            throw new ClientException("id-You can’t delete the group of term with ID [%s] because it is connected to the product".formatted(id), CONFLICT);
        }

        if (termsGroupsRepository.hasConnectionToService(id, List.of(ServiceStatus.ACTIVE))) {
            log.error("id-You can’t delete the group of term with ID [%s] because it is connected to the service".formatted(id));
            throw new ClientException("id-You can’t delete the group of term with ID [%s] because it is connected to the service".formatted(id), CONFLICT);
        }

        termsGroups.setStatus(TermGroupStatus.DELETED);
        termsGroupsRepository.save(termsGroups);

        // fetch terms that are connected to the group and "release" them
        List<Terms> connectedTerms = termsRepository.getConnectedActiveTermsByTermsGroupId(id);
        connectedTerms.forEach(t -> t.setGroupDetailId(null));
        termsRepository.saveAll(connectedTerms);

        return termsGroups.getId();
    }


    /**
     * Filters the terms groups by the given request parameters
     *
     * @param request the terms group list request that contains the filter parameters
     * @return the page of {@link TermsGroupListingResponse}
     */
    public Page<TermsGroupListingResponse> list(TermsGroupListRequest request) {
        log.debug("Listing terms groups with request: {}", request);
        Boolean excludeOldVersion = Boolean.TRUE.equals(request.getExcludeOldVersions());
        Boolean excludeFutureVersion = Boolean.TRUE.equals(request.getExcludeFutureVersions());

        String excludeVersion = (excludeOldVersion && excludeFutureVersion)
                ? "excludeOldAndFuture"
                : (excludeOldVersion ? "excludeOld"
                : (excludeFutureVersion ? "excludeFuture"
                : null));

        Sort.Order order = new Sort.Order(checkColumnDirection(request), checkSortField(request));
        return termsGroupsRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchField(request),
                getStatuses(),
                excludeVersion,
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(order)
                )
        );
    }


    /**
     * Returns the search field from the request or the default value
     *
     * @param request the terms group list request that contains the filter parameters
     * @return the search field
     */
    private String getSearchField(TermsGroupListRequest request) {
        if (request.getTermsGroupParameterFilterField() != null) {
            return request.getTermsGroupParameterFilterField().getValue();
        } else {
            return TermsParameterFilterField.ALL.getValue();
        }
    }


    /**
     * Returns the sort direction from the request or the default value
     *
     * @param request the terms group list request that contains the filter parameters
     * @return the sort direction
     */
    private Sort.Direction checkColumnDirection(TermsGroupListRequest request) {
        if (request.getColumnDirection() == null) {
            return Sort.Direction.ASC;
        }
        return request.getColumnDirection();
    }


    /**
     * Returns the sort field from the request or the default value
     *
     * @param request the terms group list request that contains the filter parameters
     * @return the sort field
     */
    private String checkSortField(TermsGroupListRequest request) {
        if (request.getTermsGroupListColumns() == null) {
            return TermsGroupListColumns.ID.getValue();
        }
        return request.getTermsGroupListColumns().getValue();
    }


    /**
     * Updates the terms group with the given request parameters
     *
     * @param id      the ID of the terms group that needs to be updated
     * @param request the terms group edit request that contains the update parameters
     * @return the ID of the updated terms group
     */
    @Transactional
    public Long update(Long id, TermsGroupEditRequest request) {
        log.debug("Updating terms group with id: {} with request: {}", id, request);

        if (!request.getUpdateExistingVersion() && request.getVersionStartDate() != null && request.getVersionStartDate().isBefore(LocalDate.now().atStartOfDay())) {
            if (termsGroupsRepository.hasLockedConnection(id) && !hasEditLockedPermission()) {
                throw new OperationNotAllowedException(String.format("id-You can’t edit the Group of terms with id: %s because it is connected to the product contract version , Service contract version or Service order", id));
            }
        }

        TermsGroups termsGroups = termsGroupsRepository
                .findByIdAndStatusIn(id, List.of(TermGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active terms group with ID %s not found;".formatted(id)));

        Terms terms = termsRepository
                .findByIdAndStatusIn(request.getTermsId(), List.of(TermStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("termsId-Active term with ID %s not found;".formatted(request.getTermsId())));

        Optional<Long> termsGroupId = termsGroupsRepository.findTermsGroupByIdWhichIsPartOfTheService(termsGroups.getId());

        if (termsGroupId.isPresent() && checkTermsContractEntryIntoForcesAndStartsOfContractInitialTerms(
                terms.getContractEntryIntoForces(),
                terms.getStartsOfContractInitialTerms())
        ) {
            log.error("Terms has ‘From first delivery' or ‘From date of change of CBG’ option selected and is added directly to service or added to group of terms which is added in service;");
            throw new ClientException("id-Terms has ‘From first delivery' or ‘From date of change of CBG’ option selected and is added directly to service or added to group of terms which is added in service", CONFLICT);
        }

        TermGroupDetails currVersion = termsGroupDetailsRepository
                .findByGroupIdAndVersionId(termsGroups.getId(), request.getDetailsVersion())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Terms group details not found by version ID %s;".formatted(request.getDetailsVersion())));

        // retrieve active term attached to this version (one should always exist)
        TermsGroupTerms dbActiveTermGroupTerm = termsGroupTermsRepository
                .findByTermGroupDetailIdAndTermGroupStatusIn(currVersion.getId(), List.of(TermGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Active term connection not found by term group details ID %s;".formatted(currVersion.getId())));

        if (request.getUpdateExistingVersion()) {
            updateExistingVersion(currVersion, dbActiveTermGroupTerm, terms, request);
        } else {
            createNewVersion(termsGroups, dbActiveTermGroupTerm, terms, request);
        }
        return termsGroups.getId();
    }


    /**
     * Updates an existing version of the terms group with the given request parameters
     *
     * @param currVersion the current version of the terms group
     * @param terms       terms to attach to the version
     * @param request     the terms group edit request that contains the update parameters
     */
    private void updateExistingVersion(TermGroupDetails currVersion, TermsGroupTerms dbActiveTermGroupTerm, Terms terms, TermsGroupEditRequest request) {
        currVersion.setName(request.getName());
        termsGroupDetailsRepository.save(currVersion);

        // if the same term is attached to the version, do nothing. Otherwise, validate the new term and attach it to the version
        if (!dbActiveTermGroupTerm.getTermId().equals(terms.getId())) {
            validateTerms(terms.getId());

            // create new connection between the term and the version
            attachTermToVersion(terms, currVersion);

            // delete the previous connection between the term and the version
            dbActiveTermGroupTerm.setTermGroupStatus(TermGroupStatus.DELETED);
            termsGroupTermsRepository.save(dbActiveTermGroupTerm);

            // "release" the term from the previous version
            Terms previousTerm = termsRepository
                    .findById(dbActiveTermGroupTerm.getTermId())
                    .orElseThrow(() -> new DomainEntityNotFoundException("termsId-Term with ID %s not found;".formatted(dbActiveTermGroupTerm.getTermId())));

            previousTerm.setGroupDetailId(null);
            termsRepository.save(previousTerm);
        }
    }


    /**
     * Attaches the given term to the given version of the terms group, marks terms as "unavailable"
     *
     * @param terms   term to attach to the version
     * @param details version to attach the term to
     */
    private void attachTermToVersion(Terms terms, TermGroupDetails details) {
        createTermsGroupTerms(terms.getId(), details.getId());
        terms.setGroupDetailId(details.getId());
        termsRepository.save(terms);
    }


    /**
     * Creates a new version of the terms group with the given request parameters
     *
     * @param termsGroups terms group to create the version of
     * @param terms       terms to attach to the version
     * @param request     the terms group edit request that contains the update parameters
     */
    private void createNewVersion(TermsGroups termsGroups, TermsGroupTerms dbActiveTermGroupTerm, Terms terms, TermsGroupEditRequest request) {
        validateVersionStartDateUniqueness(termsGroups.getId(), request.getVersionStartDate());

        TermGroupDetails newVersion = createTermGroupDetails(
                termsGroups.getId(),
                getLastVersionByGroupId(termsGroups.getId()) + 1,
                request.getName(),
                request.getVersionStartDate()
        );

        termsGroups.setLastGroupDetailsId(newVersion.getId());
        termsGroupsRepository.saveAndFlush(termsGroups);

        if (terms.getGroupDetailId() == null) {
            // if the term was not previously attached to any version, it should be validated and attached to the new version
            validateTerms(terms.getId());
            attachTermToVersion(terms, newVersion);
        } else if (terms.getGroupDetailId().equals(dbActiveTermGroupTerm.getTermGroupDetailId())) {
            // if the term from the previous version is provided, term should be "cloned" and attached to the new version
            Terms clonedTerm = termsService.cloneTerms(terms.getId());
            attachTermToVersion(clonedTerm, newVersion);
        } else {
            // if term is neither available nor attached to the previous version, it should be considered as an illegal argument
            log.error("termsId-Term with ID %s is already attached to another version of the terms group;".formatted(terms.getId()));
            throw new ClientException("termsId-Term with ID %s is already attached to another version of the terms group;".formatted(terms.getId()), ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    /**
     * Validates the terms group details version start date uniqueness in case of creating a new version of the group
     *
     * @param groupId          the ID of the terms group that needs to be updated
     * @param versionStartDate the version start date of the terms group that needs to be updated
     */
    private void validateVersionStartDateUniqueness(Long groupId, LocalDateTime versionStartDate) {
        if (versionStartDate == null) {
            log.error("versionStartDate-Version start date should be present when creating a new version of the group");
            throw new ClientException("versionStartDate-Version start date should be present when creating a new version of the group", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Optional<TermGroupDetails> groupDetailsOptional = termsGroupDetailsRepository.findByGroupIdAndStartDate(groupId, versionStartDate);
        if (groupDetailsOptional.isPresent()) {
            log.error("versionStartDate-Version with the same start date already exists, version N [%s];".formatted(groupDetailsOptional.get().getVersionId()));
            throw new ClientException("versionStartDate-Version with the same start date already exists, version N [%s];".formatted(groupDetailsOptional.get().getVersionId()), ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    /**
     * Retrieves the last version of the terms group with the provided ID
     *
     * @param groupId the ID of the terms group
     * @return the last version of the terms group
     */
    private Long getLastVersionByGroupId(Long groupId) {
        return termsGroupDetailsRepository
                .findMaxVersionByGroupId(groupId)
                .orElseThrow(() -> new ClientException("Can't find term group version ", APPLICATION_ERROR));
    }


    /**
     * Creates a new terms group details with the provided parameters
     *
     * @param groupId   the ID of the group
     * @param version   the version of the group
     * @param name      the name of the group
     * @param startDate the start date of the group
     * @return the created terms group details
     */
    private TermGroupDetails createTermGroupDetails(Long groupId, Long version, String name, LocalDateTime startDate) {
        TermGroupDetails termGroupDetails = new TermGroupDetails();
        termGroupDetails.setGroupId(groupId);
        termGroupDetails.setVersionId(version);
        termGroupDetails.setName(name);
        termGroupDetails.setStartDate(startDate);
        return termsGroupDetailsRepository.saveAndFlush(termGroupDetails);
    }


    /**
     * Creates a new terms group terms connection with the provided parameters
     *
     * @param termId            the ID of the term
     * @param termGroupDetailId the ID of the term group details
     */
    private void createTermsGroupTerms(Long termId, Long termGroupDetailId) {
        TermsGroupTerms termsGroupTerms = new TermsGroupTerms();
        termsGroupTerms.setTermId(termId);
        termsGroupTerms.setTermGroupDetailId(termGroupDetailId);
        termsGroupTerms.setTermGroupStatus(TermGroupStatus.ACTIVE);
        termsGroupTermsRepository.save(termsGroupTerms);
    }


    @Override
    public CopyDomainWithVersion getGroupType() {
        return CopyDomainWithVersion.TERMS_GROUP;
    }


    @Override
    public Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request) {
        log.debug("Finding terms groups with request: {}", request);
        return termsGroupsRepository
                .findByCopyDomainWithVersionBaseRequest(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );
    }


    @Override
    public List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long groupId) {
        return termsGroupDetailsRepository.findByCopyDomainWithVersionBaseRequest(groupId);
    }

    /**
     * Returns {@link TermsGroupViewResponse} for copying purposes.
     *
     * @param id      the id of the terms group to be copied
     * @param version the version of the terms group to be copied
     * @return the {@link TermsGroupViewResponse} object
     */
    @Transactional
    public TermsGroupViewResponse viewWithCopy(Long id, Long version) {
        log.debug("Viewing Terms groups with id: {} and version {}", id, version);

        TermsGroups termsGroups = termsGroupsRepository
                .findByIdAndStatusIn(id, List.of(TermGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Term not found with this id: " + id + " Or you dont have permission"));

        TermGroupDetails termGroupDetails = termsGroupDetailsRepository
                .findByVersionIdAndGroupId(version, id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Terms group with id: " + id + " and version: " + version + " not found"));

        List<TermGroupDetails> versions = termsGroupDetailsRepository
                .findByGroupIdOrderByStartDateAsc(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Terms group versions with id: " + id + " not found"));

        TermsGroupTerms termsGroupTerms = termsGroupTermsRepository
                .findByTermGroupDetailIdAndTermGroupStatusIn(termGroupDetails.getId(), List.of(TermGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Term by bound to the groupDetail by id: " + termGroupDetails.getId()));

        Terms term = termsRepository
                .findByIdAndStatusIn(termsGroupTerms.getTermId(), List.of(TermStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Term by id: " + termsGroupTerms.getTermId()));

        return termsGroupMapper.termsGroupViewResponseMap(
                termsGroups,
                termGroupDetails,
                termsService.copyTerms(term),
                versions.stream().map(termsGroupMapper::termsVersionsMap).collect(Collectors.toList())
        );

    }

    private boolean checkTermsContractEntryIntoForcesAndStartsOfContractInitialTerms(List<ContractEntryIntoForce> contractEntryIntoForces,
                                                                                     List<StartOfContractInitialTerm> startOfContractInitialTerms) {
        return contractEntryIntoForces.contains(ContractEntryIntoForce.DATE_CHANGE_OF_CBG) ||
                contractEntryIntoForces.contains(ContractEntryIntoForce.FIRST_DELIVERY) ||
                startOfContractInitialTerms.contains(StartOfContractInitialTerm.DATE_OF_CHANGE_OF_CBG) ||
                startOfContractInitialTerms.contains(StartOfContractInitialTerm.FIRST_DELIVERY);
    }

    private boolean hasEditLockedPermission() {
        return permissionService.permissionContextContainsPermissions(TERMS_GROUP, List.of(PermissionEnum.TERMS_GROUP_EDIT_LOCKED));
    }

}

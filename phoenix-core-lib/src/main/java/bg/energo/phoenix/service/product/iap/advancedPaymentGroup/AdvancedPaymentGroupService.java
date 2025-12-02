package bg.energo.phoenix.service.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroup;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupAdvancedPayments;
import bg.energo.phoenix.model.entity.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetails;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductGroupOfInterimAndAdvancePayments;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceInterimAndAdvancePaymentGroup;
import bg.energo.phoenix.model.enums.copy.group.CopyDomainWithVersion;
import bg.energo.phoenix.model.enums.product.ExcludeVersions;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupFilterField;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentListColumns;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.request.copy.group.CopyDomainWithVersionBaseRequest;
import bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup.AdvancedPaymentGroupCreateRequest;
import bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup.AdvancedPaymentGroupEditRequest;
import bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup.AdvancedPaymentGroupListRequest;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.AdvancedPaymentGroupListResponse;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.AdvancedPaymentGroupViewResponse;
import bg.energo.phoenix.model.response.AdvancedPaymentGroup.AdvancedPaymentSimpleInfoResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.AdvancedPaymentGroupAdvancedPaymentsRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.AdvancedPaymentGroupDetailsRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.AdvancedPaymentGroupRepository;
import bg.energo.phoenix.repository.product.iap.advancedPaymentGroup.mappers.AdvancePaymentGroupMapper;
import bg.energo.phoenix.repository.product.iap.interimAdvancePayment.InterimAdvancePaymentRepository;
import bg.energo.phoenix.repository.product.product.ProductGroupOfInterimAndAdvancePaymentsRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceInterimAndAdvancePaymentGroupRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.group.CopyDomainWithVersionBaseService;
import bg.energo.phoenix.service.product.iap.interimAdvancePayment.InterimAdvancePaymentService;
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

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.InterimAdvancePaymentStatus.ACTIVE;
import static bg.energo.phoenix.permissions.PermissionContextEnum.ADVANCED_PAYMENT_GROUP;
import static bg.energo.phoenix.permissions.PermissionEnum.ADVANCED_PAYMENT_GROUP_VIEW_BASIC;
import static bg.energo.phoenix.permissions.PermissionEnum.ADVANCED_PAYMENT_GROUP_VIEW_DELETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedPaymentGroupService implements CopyDomainWithVersionBaseService {
    private final ServiceInterimAndAdvancePaymentGroupRepository serviceInterimAndAdvancePaymentGroupRepository;
    private final ProductGroupOfInterimAndAdvancePaymentsRepository productGroupOfInterimAndAdvancePaymentsRepository;

    private final AdvancedPaymentGroupRepository advancedPaymentGroupRepository;
    private final AdvancedPaymentGroupDetailsRepository advancedPaymentGroupDetailsRepository;
    private final AdvancedPaymentGroupAdvancedPaymentsRepository advancedPaymentGroupAdvancedPaymentsRepository;
    private final AdvancePaymentGroupMapper advancePaymentGroupMapper;
    private final PermissionService permissionService;
    private final InterimAdvancePaymentRepository advancePaymentRepository;
    private final InterimAdvancePaymentService advancePaymentService;

    /**
     * <h1>Creates Interim Advanced Payment group </h1>
     *
     * @param request request with data for creating Interim Advanced Payment group
     * @return created Interim Advanced Payment group id
     */
    @Transactional
    public Long create(AdvancedPaymentGroupCreateRequest request) {
        log.debug("Creating Advanced Payment groups with request {}", request);
        checkIdsOfAdvancePayment(request);
        return createAdvancedPaymentGroupId(request);
    }

    /**
     * <h1>checkIdsOfAdvancePayment</h1>
     *
     * @param request request with data for creating Interim Advanced Payment group
     */
    private void checkIdsOfAdvancePayment(AdvancedPaymentGroupCreateRequest request) {
        if (!CollectionUtils.isEmpty(request.getAdvancedPayments())) {
            for (Long id : request.getAdvancedPayments()) {
                InterimAdvancePayment interimAdvancePayment = advancePaymentRepository.findByIdAndStatusIn(id, List.of(ACTIVE))
                        .orElseThrow(() -> new DomainEntityNotFoundException("ids-Interim Advanced Payment not found by Id: " + id + ";"));
            }
        }
    }

    /**
     * <h1>createAdvancedPaymentGroupId</h1>
     *
     * @param request request with data for creating Interim Advanced Payment group
     * @return created interim advanced payment group id
     */
    private Long createAdvancedPaymentGroupId(AdvancedPaymentGroupCreateRequest request) {
        List<Long> advancedPaymentIds = request.getAdvancedPayments();
        checkAdvancedPaymentIdsForUniqueness(advancedPaymentIds);
        if (checkIfAdvancedPaymentIsBound(request.getAdvancedPayments(), List.of(AdvancedPaymentGroupStatus.ACTIVE))) {
            throw new ClientException("id- AdvancedPaymentId is already bound to the another group", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }
        checkAdvancePaymentAvailability(advancedPaymentIds);
        AdvancedPaymentGroup advancedPaymentGroup = new AdvancedPaymentGroup();
        advancedPaymentGroup.setStatus(AdvancedPaymentGroupStatus.ACTIVE);
        AdvancedPaymentGroup savedAdvancedPaymentGroup = advancedPaymentGroupRepository.saveAndFlush(advancedPaymentGroup);

        AdvancedPaymentGroupDetails advancedPaymentGroupDetails = new AdvancedPaymentGroupDetails();
        advancedPaymentGroupDetails.setName(request.getName());
        advancedPaymentGroupDetails.setVersionId(1L);
        advancedPaymentGroupDetails.setStartDate(LocalDate.now());
        advancedPaymentGroupDetails.setAdvancedPaymentGroupId(savedAdvancedPaymentGroup.getId());
        advancedPaymentGroupDetailsRepository.saveAndFlush(advancedPaymentGroupDetails);
        if (!CollectionUtils.isEmpty(advancedPaymentIds)) {
            List<AdvancedPaymentGroupAdvancedPayments> advancedPaymentGroupAdvancedPayments = new ArrayList<>();
            List<InterimAdvancePayment> advancePaymentsToSave = new ArrayList<>();
            for (int i = 0; i < advancedPaymentIds.size(); i++) {
                Long advancePaymentId = advancedPaymentIds.get(i);
                AdvancedPaymentGroupAdvancedPayments advPaymentAdv = new AdvancedPaymentGroupAdvancedPayments();
                advPaymentAdv.setAdvancePaymentId(advancePaymentId);
                advPaymentAdv.setAdvancePaymentGroupDetailId(advancedPaymentGroupDetails.getId());
                advPaymentAdv.setStatus(AdvancedPaymentGroupStatus.ACTIVE);
                advancedPaymentGroupAdvancedPayments.add(advPaymentAdv);
                InterimAdvancePayment interimAdvancePayment = advancePaymentRepository.findByIdAndStatus(advancePaymentId, ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("id-Cant find Active Advance Payment"));
                interimAdvancePayment.setGroupDetailId(advancedPaymentGroupDetails.getId());
                advancePaymentsToSave.add(interimAdvancePayment);
            }
            advancedPaymentGroupAdvancedPaymentsRepository.saveAllAndFlush(advancedPaymentGroupAdvancedPayments);
            advancePaymentRepository.saveAll(advancePaymentsToSave);
        }
        return savedAdvancedPaymentGroup.getId();
    }

    /**
     * <h1>checkAdvancedPaymentIdsForUniqueness</h1>
     *
     * @param advancedPaymentIds list of advanced payment ids
     */
    private void checkAdvancedPaymentIdsForUniqueness(List<Long> advancedPaymentIds) {
        if (!CollectionUtils.isEmpty(advancedPaymentIds)) {
            if (new HashSet<>(advancedPaymentIds).size() != advancedPaymentIds.size()) {
                throw new ClientException("ids-list contains duplicate values", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
    }

    /**
     * <h1>Interim Advanced Payment Delete</h1>
     *
     * @param id id of Interim Advanced Payment group
     * @return deleted item id
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting Advanced Payment groups with id: {}", id);
        AdvancedPaymentGroup savedAdvancedPaymentGroup;
        AdvancedPaymentGroup advancedPaymentGroup = advancedPaymentGroupRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Advanced Payment group not found by Id: " + id + ";"));

        if (advancedPaymentGroupRepository.hasConnectionToProduct(id)) {
            log.error("id-You can’t delete the interim and advance payment group with ID [%s] because it is connected to the product.".formatted(id));
            throw new ClientException("id-You can’t delete the interim and advance payment group with ID [%s] because it is connected to the product.".formatted(id), ErrorCode.CONFLICT);
        }

        if (advancedPaymentGroupRepository.hasConnectionToService(id)) {
            log.error("id-You can’t delete the interim and advance payment group with ID [%s] because it is connected to the service.".formatted(id));
            throw new ClientException("id-You can’t delete the interim and advance payment group with ID [%s] because it is connected to the service.".formatted(id), ErrorCode.CONFLICT);
        }

        if (!advancedPaymentGroup.getStatus().equals(AdvancedPaymentGroupStatus.DELETED)) {
            advancedPaymentGroup.setStatus(AdvancedPaymentGroupStatus.DELETED);
            savedAdvancedPaymentGroup = advancedPaymentGroupRepository.save(advancedPaymentGroup);
        } else {
            log.error("id-Advanced Payment group is already deleted;");
            throw new ClientException("id-Advanced Payment group is already deleted;", ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        // "release" all interim and advance payments from the group
        List<InterimAdvancePayment> connectedInterimAdvancePayments = advancedPaymentGroupRepository.getConnectedActiveIAPsByGroupId(id);
        connectedInterimAdvancePayments.forEach(iap -> iap.setGroupDetailId(null));
        advancePaymentRepository.saveAll(connectedInterimAdvancePayments);

        return savedAdvancedPaymentGroup.getId();
    }


    /**
     * <h1>Interim Advanced Payment Group View</h1>
     *
     * @param id      id of the group
     * @param version version of the group
     * @return {@link AdvancedPaymentGroupViewResponse}
     */
    public AdvancedPaymentGroupViewResponse view(Long id, Long version) {
        log.debug("Viewing Advanced Payment groups with id: {} and version {}", id, version);

        AdvancedPaymentGroup advancedPaymentGroup = advancedPaymentGroupRepository
                .findByIdAndStatusIn(id, getStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("Advanced payment group not found with this id: " + id + " Or you dont have permission;"));

        AdvancedPaymentGroupDetails advancedPaymentGroupDetails;
        if (version != null) {
            advancedPaymentGroupDetails = advancedPaymentGroupDetailsRepository
                    .findByAdvancedPaymentGroupIdAndVersionId(id, version)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Advanced payment group with id: " + id + " and version: " + version + " not found;"));
        } else {
            advancedPaymentGroupDetails = advancedPaymentGroupDetailsRepository
                    .findFirstByAdvancedPaymentGroupIdAndStartDateLessThanEqualOrderByStartDateDesc(id, LocalDate.now())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Advanced payment group with id: " + id + " not found;"));
        }

        List<AdvancedPaymentGroupDetails> versions = advancedPaymentGroupDetailsRepository
                .findByAdvancedPaymentGroupIdOrderByStartDateAsc(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Advanced payment group versions with id: " + id + " not found;"));

        Optional<List<AdvancedPaymentGroupAdvancedPayments>> advancedPaymentGroupAdvancedPaymentsOptional = advancedPaymentGroupAdvancedPaymentsRepository
                .findByAdvancePaymentGroupDetailIdAndStatusIn(advancedPaymentGroupDetails.getId(), List.of(AdvancedPaymentGroupStatus.ACTIVE)/*getStatuses()*/);
        //.orElseThrow(() -> new ClientException("Can't find Term by bound to the groupDetail by id: " + advancedPaymentGroupDetails.getId(), ErrorCode.DOMAIN_ENTITY_NOT_FOUND));

        List<AdvancedPaymentGroupAdvancedPayments> advancedPaymentGroupAdvancedPayments = advancedPaymentGroupAdvancedPaymentsOptional.get();

        List<AdvancedPaymentSimpleInfoResponse> advancedPaymentSimpleInfoResponse = new ArrayList<>();
        if (!CollectionUtils.isEmpty(advancedPaymentGroupAdvancedPayments)) {
            for (AdvancedPaymentGroupAdvancedPayments item : advancedPaymentGroupAdvancedPayments) {
                InterimAdvancePayment advPay = advancePaymentRepository.findById(item.getAdvancePaymentId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("Advanced payment with id: " + id + " not found"));
                advancedPaymentSimpleInfoResponse.add(new AdvancedPaymentSimpleInfoResponse(advPay.getId(), advPay.getName(), advPay.getName() + " (" + advPay.getId() + ")"));
            }
        }
        AdvancedPaymentGroupViewResponse advancedPaymentGroupViewResponse = advancePaymentGroupMapper
                .toAdvancedPaymentGroupViewResponse(
                        advancedPaymentGroup,
                        advancedPaymentGroupDetails,
                        versions.stream().map(advancePaymentGroupMapper::versionsMap).toList(),
                        advancedPaymentSimpleInfoResponse);
        advancedPaymentGroupViewResponse.setIsLocked(advancedPaymentGroupRepository.hasLockedConnection(id));
        if (advancedPaymentGroupDetails.getStartDate().isAfter(LocalDate.now())) {
            advancedPaymentGroupViewResponse.setIsLocked(false);
        }
        return advancedPaymentGroupViewResponse;
    }

    private List<AdvancedPaymentGroupStatus> getStatuses() {
        List<AdvancedPaymentGroupStatus> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(ADVANCED_PAYMENT_GROUP);

        if (context.contains(ADVANCED_PAYMENT_GROUP_VIEW_DELETED.getId())) {
            statuses.add(AdvancedPaymentGroupStatus.DELETED);
        }

        if (context.contains(ADVANCED_PAYMENT_GROUP_VIEW_BASIC.getId())) {
            statuses.add(AdvancedPaymentGroupStatus.ACTIVE);
        }

        return statuses;
    }

    /**
     * <h1>Interim Advanced Payment Group edit</h1>
     *
     * @param id      id of the group
     * @param request {@link AdvancedPaymentGroupEditRequest}
     * @return long id of edited interim advanced payment group
     */
    @Transactional
    public Long edit(Long id, AdvancedPaymentGroupEditRequest request) {
        log.debug("Editing Advanced Payment groups with id: {}", id);

        if (!request.getUpdateExistingVersion() && request.getStartDate().isBefore(LocalDate.now())) {
            if (advancedPaymentGroupRepository.hasLockedConnection(id) && !hasEditLockedPermission()) {
                throw new OperationNotAllowedException(String.format("id-You can’t edit the Group of interim and advance payment with id: %s because it is connected to the product contract version , Service contract version or Service order", id));
            }
        }

        trimRequestVariables(request);
        checkAdvancePaymentAvailabilityForEdit(request.getAdvancedPayments());
        AdvancedPaymentGroup advancedPaymentGroup = advancedPaymentGroupRepository.findByIdAndStatusIn(id, List.of(AdvancedPaymentGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-can't find AdvancedPaymentGroup"));
        AdvancedPaymentGroupDetails advancedPaymentGroupDetails =
                advancedPaymentGroupDetailsRepository.findByAdvancedPaymentGroupIdAndVersionId(advancedPaymentGroup.getId(), request.getVersionId())
                        .orElseThrow(() -> new DomainEntityNotFoundException("id-can't find AdvancedPaymentGroupDetails"));
        var advancedPaymentGroupDetailAdvancedPayments =
                advancedPaymentGroupAdvancedPaymentsRepository.findAllByAdvancePaymentGroupDetailIdAndStatus(advancedPaymentGroupDetails.getId(),
                        AdvancedPaymentGroupStatus.ACTIVE);
        if (request.getUpdateExistingVersion()) {
            updateExistingVersion(advancedPaymentGroupDetails, request, advancedPaymentGroupDetailAdvancedPayments);
            advancedPaymentGroupDetails.setName(request.getName());
            advancedPaymentGroupDetailsRepository.save(advancedPaymentGroupDetails);
        } else {
            AdvancedPaymentGroupDetails newVersion = createNewVersion(advancedPaymentGroup, advancedPaymentGroupDetails, request, advancedPaymentGroupDetailAdvancedPayments);
        }

        updateVersionStartAndEndDates(id, advancedPaymentGroupDetails);
        return advancedPaymentGroup.getId();
    }

    private void updateVersionStartAndEndDates(Long id, AdvancedPaymentGroupDetails advancedPaymentGroupDetails) {
        List<AdvancedPaymentGroupDetails> advancePaymentVersions = advancedPaymentGroupRepository.findAdvancedPaymentGroupDetailsByAdvancePaymentGroup(id);
        List<VersionWithDatesModel> versionWithDatesModels = advancePaymentVersions.stream().map(VersionWithDatesModel::new).collect(Collectors.toList());

        List<VersionWithDatesModel> updatedVersionWithDatesModels = CalculateVersionDates.calculateVersionEndDates(versionWithDatesModels, advancedPaymentGroupDetails.getStartDate(), Math.toIntExact(advancedPaymentGroupDetails.getVersionId()));

        advancePaymentVersions
                .forEach(pcv -> updatedVersionWithDatesModels.stream()
                        .filter(v -> Objects.equals(v.getVersionId(), Math.toIntExact(pcv.getVersionId())))
                        .findFirst()
                        .ifPresent(model -> {
                            pcv.setEndDate(model.getEndDate());
                            pcv.setStartDate(model.getStartDate());
                        }));
    }

    private void checkAdvancePaymentAvailabilityForEdit(List<Long> advancedPayments) {
        if (advancedPayments != null) {
            List<Long> advancePaymentsNotBoundToGroup = advancedPayments.stream().map(id -> advancePaymentRepository.findByIdAndStatus(id, ACTIVE)
                            .orElseThrow(() -> new DomainEntityNotFoundException("id-Can't find ACTIVE advance Payment with id: %s;".formatted(id))))
                    .filter(interimAdvancePayment -> interimAdvancePayment.getGroupDetailId() == null)
                    .map(InterimAdvancePayment::getId).toList();
            checkAdvancePaymentAvailability(advancePaymentsNotBoundToGroup);
        }
    }

    private void checkAdvancePaymentAvailability(List<Long> advancedPayments) {
        if (CollectionUtils.isNotEmpty(advancedPayments)) {
            for (Long id : advancedPayments) {
                Optional<InterimAdvancePayment> advancePayments = advancePaymentRepository.checkAvailableIap(id);
                if (advancePayments.isEmpty()) {
                    throw new ClientException("id-Advance payment with id: %s is not available;".formatted(id), ErrorCode.CONFLICT);
                }
            }
        }

    }

    /**
     * <h1>Interim Advanced Payment group createNewVersion</h1>
     *
     * @param advancedPaymentGroup                 advanced payment group
     * @param advancedPaymentGroupDetails          advanced payment group details
     * @param request                              advanced payment group edit request
     * @param advancedPaymentGroupAdvancedPayments advanced payment group advanced payments
     */
    private AdvancedPaymentGroupDetails createNewVersion(AdvancedPaymentGroup advancedPaymentGroup,
                                                         AdvancedPaymentGroupDetails advancedPaymentGroupDetails,
                                                         AdvancedPaymentGroupEditRequest request,
                                                         List<AdvancedPaymentGroupAdvancedPayments> advancedPaymentGroupAdvancedPayments) {
        checkVersionDate(advancedPaymentGroup.getId(), request);
        var version = advancedPaymentGroupDetailsRepository.findLastVersionByAdvancedPaymentGroupId(advancedPaymentGroupDetails.getAdvancedPaymentGroupId());
        var newAdvancedPaymentGroupDetails = advancePaymentGroupMapper.advancedGroupDetailsFromRequest(request, advancedPaymentGroup, version + 1);
        advancedPaymentGroupDetailsRepository.save(newAdvancedPaymentGroupDetails);

        List<Long> advancedPaymentIds = request.getAdvancedPayments();
        Set<Long> currentAdvPaymentSet = advancedPaymentGroupAdvancedPayments.stream().map(AdvancedPaymentGroupAdvancedPayments::getAdvancePaymentId).collect(Collectors.toSet());
        List<Long> advancePaymentsToClone = new ArrayList<>();
        List<Long> advancePaymentsToCreate = new ArrayList<>();

        if (advancedPaymentIds != null) {
            advancedPaymentIds.forEach(advPaymentId -> {
                if (currentAdvPaymentSet.contains(advPaymentId)) {
                    advancePaymentsToClone.add(advPaymentId);
                } else {
                    advancePaymentsToCreate.add(advPaymentId);
                }
            });
            List<AdvancedPaymentGroupAdvancedPayments> newAdvPaymentList = new ArrayList<>();
            cloneExistingAdvPayments(newAdvancedPaymentGroupDetails, advancePaymentsToClone, newAdvPaymentList);
            createNewAdvancePayments(newAdvancedPaymentGroupDetails, advancedPaymentIds, advancePaymentsToCreate, newAdvPaymentList);
            advancedPaymentGroupAdvancedPaymentsRepository.saveAll(newAdvPaymentList);
        } else {
            Optional<List<AdvancedPaymentGroupAdvancedPayments>> advancedPaymentsGroupAdvancedPayments = advancedPaymentGroupAdvancedPaymentsRepository
                    .findByAdvancePaymentGroupDetailIdAndStatusIn(advancedPaymentGroupDetails.getId(), List.of(AdvancedPaymentGroupStatus.ACTIVE));

            if (advancedPaymentsGroupAdvancedPayments.isPresent()) {
                for (AdvancedPaymentGroupAdvancedPayments item : advancedPaymentsGroupAdvancedPayments.get()) {
                    var advPayToFree = advancePaymentRepository.findByIdAndStatus(item.getId(), ACTIVE);
                    advPayToFree.ifPresent(adv -> {
                        adv.setGroupDetailId(null);
                        advancePaymentRepository.save(adv);
                    });
                }
            }
        }

        return newAdvancedPaymentGroupDetails;
    }

    private void createNewAdvancePayments(AdvancedPaymentGroupDetails newAdvancedPaymentGroupDetails, List<Long> advancedPaymentIds, List<Long> advancePaymentsToCreate, List<AdvancedPaymentGroupAdvancedPayments> newAdvPaymentList) {
        List<InterimAdvancePayment> availableAdvancePayments = advancePaymentRepository.getAvailableAdvancePayments(advancePaymentsToCreate);
        for (Long advancePaymentId : advancePaymentsToCreate) {
            int index = advancedPaymentIds.indexOf(advancePaymentId);
            newAdvPaymentList.add(createAdvancedPaymentGroupAdvancedPayment(newAdvancedPaymentGroupDetails, availableAdvancePayments, index, advancePaymentId));
        }
    }

    private void cloneExistingAdvPayments(AdvancedPaymentGroupDetails newAdvancedPaymentGroupDetails, List<Long> advancePaymentsToClone, List<AdvancedPaymentGroupAdvancedPayments> newAdvPaymentList) {
        advancePaymentsToClone.forEach(advPaymentId -> {
            var advPayment = advancePaymentService.cloneInterimAdvancePayment(advPaymentId);
            advPayment.setGroupDetailId(newAdvancedPaymentGroupDetails.getId());
            newAdvPaymentList.add(advancePaymentGroupMapper.createAdvancedPaymentGroupAdvancedPayments(newAdvancedPaymentGroupDetails, advPayment.getId()));
        });
    }


    /**
     * <h1>updateExistingVersion</h1>
     *
     * @param advancedPaymentGroupDetails advanced payment group details
     * @param request                     advanced payment group edit request
     */
    private void updateExistingVersion(AdvancedPaymentGroupDetails advancedPaymentGroupDetails,
                                       AdvancedPaymentGroupEditRequest request,
                                       List<AdvancedPaymentGroupAdvancedPayments> advancedPaymentGroupAdvancedPayments) {

        List<Long> advancedPaymentsIds = request.getAdvancedPayments();
        List<InterimAdvancePayment> availableAdvPayments = advancePaymentRepository
                .getAvailableAdvancePaymentForGroupDetail(advancedPaymentsIds, advancedPaymentGroupDetails.getId());
        if (advancedPaymentsIds != null) {
            advancedPaymentGroupAdvancedPayments.forEach(item -> {
                if (!advancedPaymentsIds.contains(item.getAdvancePaymentId())) {
                    item.setStatus(AdvancedPaymentGroupStatus.DELETED);
                    var advPaymentsToFree = advancePaymentRepository.findByIdAndStatus(item.getAdvancePaymentId(), InterimAdvancePaymentStatus.ACTIVE);
                    advPaymentsToFree.ifPresent(penalty -> {
                        penalty.setGroupDetailId(null);
                        advancePaymentRepository.save(penalty);
                    });
                }
            });

            for (int i = 0; i < advancedPaymentsIds.size(); i++) {
                Long penaltyId = advancedPaymentsIds.get(i);
                if (advancedPaymentGroupAdvancedPayments.stream().noneMatch(penaltyGroupPenalty -> penaltyGroupPenalty.getAdvancePaymentId().equals(penaltyId))) {
                    advancedPaymentGroupAdvancedPayments.add(createAdvancedPaymentGroupAdvancedPayments(advancedPaymentGroupDetails, availableAdvPayments, i, penaltyId));
                }
            }

            advancePaymentRepository.saveAll(availableAdvPayments);
            advancedPaymentGroupAdvancedPaymentsRepository.saveAll(advancedPaymentGroupAdvancedPayments);
        } else {
            Optional<List<AdvancedPaymentGroupAdvancedPayments>> advancedPaymentsGroupAdvancedPayments = advancedPaymentGroupAdvancedPaymentsRepository
                    .findByAdvancePaymentGroupDetailIdAndStatusIn(advancedPaymentGroupDetails.getId(), List.of(AdvancedPaymentGroupStatus.ACTIVE));

            if (advancedPaymentsGroupAdvancedPayments.isPresent()) {
                for (AdvancedPaymentGroupAdvancedPayments item : advancedPaymentsGroupAdvancedPayments.get()) {
                    var advPayToFree = advancePaymentRepository.findByIdAndStatus(item.getId(), ACTIVE);
                    advPayToFree.ifPresent(adv -> {
                        adv.setGroupDetailId(null);
                        advancePaymentRepository.save(adv);
                    });
                }
            }
        }

    }

    private AdvancedPaymentGroupAdvancedPayments createAdvancedPaymentGroupAdvancedPayments(AdvancedPaymentGroupDetails advancedPaymentGroupDetails, List<InterimAdvancePayment> availableInterimAdvancePayments, int index, Long advancePaymentId) {
        var advancePayment = availableInterimAdvancePayments.stream().filter(p -> p.getId().equals(advancePaymentId)).findAny().orElseThrow(() ->
                new ClientException("advancePayments[%s]-Interim and Advance payment with id: %s is not available;".formatted(index, advancePaymentId), ILLEGAL_ARGUMENTS_PROVIDED));
        advancePayment.setGroupDetailId(advancedPaymentGroupDetails.getId());
        return advancePaymentGroupMapper.createAdvancedPaymentGroupAdvancedPayments(advancedPaymentGroupDetails, advancePaymentId);
    }

    /**
     * <h1>createAdvancedPaymentGroupAdvancedPayment</h1>
     *
     * @param advancedPaymentGroupDetails advanced payment group details
     * @param availableAdvancedPayments   available advanced payments
     * @param i                           index
     * @param id                          advanced payment id
     * @return advanced payment group advanced payment
     */
    private AdvancedPaymentGroupAdvancedPayments createAdvancedPaymentGroupAdvancedPayment(AdvancedPaymentGroupDetails advancedPaymentGroupDetails, List<InterimAdvancePayment> availableAdvancedPayments, int i, Long id) {
        var advPayGroupAdvPay = availableAdvancedPayments.stream().filter(ap -> ap.getId().equals(id)).findAny().orElseThrow(() ->
                new ClientException("advancePayments[%s]-Advance payment with id: %s is not available;".formatted(i, id), ILLEGAL_ARGUMENTS_PROVIDED));
        advPayGroupAdvPay.setGroupDetailId(advancedPaymentGroupDetails.getId());
        return advancePaymentGroupMapper.createAdvPayGroupAdvPay(advancedPaymentGroupDetails, id);
    }

    /**
     * <h1>checkIfAdvancedPaymentIsBound</h1>
     *
     * @param advancedPaymentIds list of advanced payment ids
     * @param statuses           list of advanced payment statuses
     * @return status of advanced payment is bound to another object or not
     */
    private boolean checkIfAdvancedPaymentIsBound(List<Long> advancedPaymentIds, List<AdvancedPaymentGroupStatus> statuses) {
        if (!CollectionUtils.isEmpty(advancedPaymentIds)) {
            for (Long id : advancedPaymentIds) {
                InterimAdvancePayment advancePayments = advancePaymentRepository.findByIdAndStatus(id, ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("id- Cant find Active Advanced Payment"));
                if (advancePayments.getGroupDetailId() != null) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * <h1>checkVersionDate</h1>
     *
     * @param id      id of advanced payment group
     * @param request {@link AdvancedPaymentGroupEditRequest}
     */
    private void checkVersionDate(Long id, AdvancedPaymentGroupEditRequest request) {
        if (advancedPaymentGroupDetailsRepository.existsByAdvancedPaymentGroupIdAndStartDate(id, request.getStartDate())) {
            log.error("startDate-Start date should be unique among versions;");
            throw new ClientException("startDate-Start date should be unique among versions;", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    /**
     * <h1>trimRequestVariables</h1>
     *
     * @param request {@link AdvancedPaymentGroupEditRequest}
     */
    private void trimRequestVariables(AdvancedPaymentGroupEditRequest request) {
        request.setName(request.getName().trim());
    }

    /**
     * <h1>Interim Advanced Payment list</h1>
     *
     * @param request {@link AdvancedPaymentGroupListRequest}
     * @return {@link AdvancedPaymentGroupListResponse}
     */
    public Page<AdvancedPaymentGroupListResponse> list(AdvancedPaymentGroupListRequest request) {
        log.debug("Listing Advanced Payment groups with id: {}", request);

        String sortBy = AdvancedPaymentListColumns.ID.getValue();
        if (request.getSortBy() != null && StringUtils.isNotEmpty(request.getSortBy().getValue())) {
            sortBy = request.getSortBy().getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String searchBy = AdvancedPaymentGroupFilterField.ALL.getValue();
        if (request.getPromptBy() != null && StringUtils.isNotEmpty(request.getPromptBy().getValue())) {
            searchBy = request.getPromptBy().getValue();
        }

        List<String> statuses = getStatuses()
                .stream()
                .map(Enum::name)
                .toList();

        String excludeVersion = ExcludeVersions.getExcludeVersionFromCheckBoxes(request.isExcludeOldVersions(), request.isExcludeFutureVersions()).getValue();

        return advancedPaymentGroupRepository.list(
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

    @Transactional
    public AdvancedPaymentGroupViewResponse viewWithCopy(Long id, Long version) {
        log.debug("Viewing Advanced payment groups with id: {} and version {}", id, version);

        AdvancedPaymentGroup advancedPaymentGroup = advancedPaymentGroupRepository
                .findByIdAndStatusIn(id, List.of(AdvancedPaymentGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Advanced payment group not found with this id: %s Or you dont have permission;".formatted(id)));

        AdvancedPaymentGroupDetails advancedPaymentGroupDetails = advancedPaymentGroupDetailsRepository
                .findByAdvancedPaymentGroupIdAndVersionId(id, version)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Advanced payment group not found with this id: %s and version: %s Or you dont have permission;".formatted(id, version)));

        List<AdvancedPaymentGroupDetails> versions = advancedPaymentGroupDetailsRepository
                .findByAdvancedPaymentGroupIdOrderByStartDateAsc(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Advanced payment group versions with id: " + id + " not found;"));

        List<AdvancedPaymentGroupAdvancedPayments> advancedPaymentGroupAdvancedPayments = advancedPaymentGroupAdvancedPaymentsRepository
                .findByAdvancePaymentGroupDetailIdAndStatusIn(advancedPaymentGroupDetails.getId(), List.of(AdvancedPaymentGroupStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Advanced payment by bound to the groupDetail by id: " + advancedPaymentGroupDetails.getId()));

        List<InterimAdvancePayment> advancePayments = new ArrayList<>();
        for (AdvancedPaymentGroupAdvancedPayments item : advancedPaymentGroupAdvancedPayments) {
            InterimAdvancePayment interimAdvancePayment = advancePaymentRepository
                    .findByIdAndStatusIn(item.getAdvancePaymentId(), List.of(ACTIVE))
                    .orElseThrow(() -> new DomainEntityNotFoundException("Can't find Interim advance payment by id: " + item.getAdvancePaymentId()));
            advancePayments.add(interimAdvancePayment);
        }

        List<AdvancedPaymentSimpleInfoResponse> advancedPaymentSimpleInfoResponses = new ArrayList<>();
        if (!CollectionUtils.isEmpty(advancePayments)) {
            advancedPaymentSimpleInfoResponses = advancePaymentService.copyAdvancePaymentsWithResponse(advancePayments);
        }

        return advancePaymentGroupMapper.toAdvancedPaymentGroupViewResponse(
                advancedPaymentGroup,
                advancedPaymentGroupDetails,
                versions.stream().map(advancePaymentGroupMapper::versionsMap).toList(),
                advancedPaymentSimpleInfoResponses
        );
    }


    /**
     * Adds interim advance payment groups to the service details
     *
     * @param interimAdvancePaymentGroupIds ids of the interim advance payment groups to be added
     * @param serviceDetails                service details to which the interim advance payment groups will be added
     * @param exceptionMessages             list of exception messages to be populated in case of errors
     */
    @Transactional
    public void addInterimAdvancePaymentGroupsToService(List<Long> interimAdvancePaymentGroupIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(interimAdvancePaymentGroupIds)) {
            List<ServiceInterimAndAdvancePaymentGroup> tempList = new ArrayList<>();

            for (int i = 0; i < interimAdvancePaymentGroupIds.size(); i++) {
                Long iapgId = interimAdvancePaymentGroupIds.get(i);
                // being an active group is a sufficient condition for adding it to the service
                Optional<AdvancedPaymentGroup> iapgOptional = advancedPaymentGroupRepository.findByIdAndStatusIn(iapgId, List.of(AdvancedPaymentGroupStatus.ACTIVE));
                if (iapgOptional.isEmpty()) {
                    log.error("interimAdvancePaymentGroups[%s]-can't find Interim Advance Payment Group with id: %s;".formatted(i, iapgId));
                    exceptionMessages.add("basicSettings.interimAdvancePaymentGroups[%s]-can't find Interim Advance Payment Group with id: %s;".formatted(i, iapgId));
                    continue;
                }

                ServiceInterimAndAdvancePaymentGroup serviceInterimAndAdvancePaymentGroup = new ServiceInterimAndAdvancePaymentGroup();
                serviceInterimAndAdvancePaymentGroup.setServiceDetails(serviceDetails);
                serviceInterimAndAdvancePaymentGroup.setAdvancedPaymentGroup(iapgOptional.get());
                serviceInterimAndAdvancePaymentGroup.setStatus(ServiceSubobjectStatus.ACTIVE);
                tempList.add(serviceInterimAndAdvancePaymentGroup);
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all interim advance payment groups
            serviceInterimAndAdvancePaymentGroupRepository.saveAll(tempList);
        }
    }


    /**
     * Updates interim advance payment groups for the service details existing version
     *
     * @param requestIAPGs      ids of the interim advance payment groups to be updated
     * @param serviceDetails    service details to which the interim advance payment groups will be updated
     * @param exceptionMessages list of exception messages to be populated in case of errors
     */
    @Transactional
    public void updateServiceIAPGroupsForExistingVersion(List<Long> requestIAPGs, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        // fetch all active IAPGs for the service
        List<ServiceInterimAndAdvancePaymentGroup> dbIAPGs = serviceInterimAndAdvancePaymentGroupRepository
                .findAllByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestIAPGs)) {
            List<Long> dbIAPGIds = dbIAPGs.stream().map(ServiceInterimAndAdvancePaymentGroup::getAdvancedPaymentGroup).map(AdvancedPaymentGroup::getId).toList();
            List<ServiceInterimAndAdvancePaymentGroup> tempList = new ArrayList<>();

            for (int i = 0; i < requestIAPGs.size(); i++) {
                Long iapgId = requestIAPGs.get(i);
                if (!dbIAPGIds.contains(iapgId)) { // if iapg is new, its availability should be checked
                    Optional<AdvancedPaymentGroup> iapgOptional = advancedPaymentGroupRepository.findByIdAndStatusIn(iapgId, List.of(AdvancedPaymentGroupStatus.ACTIVE));
                    if (iapgOptional.isEmpty()) {
                        log.error("interimAdvancePaymentGroups[%s]-can't find Interim Advance Payment Group with id: %s;".formatted(i, iapgId));
                        exceptionMessages.add("basicSettings.interimAdvancePaymentGroups[%s]-can't find Interim Advance Payment Group with id: %s;".formatted(i, iapgId));
                        continue;
                    }

                    ServiceInterimAndAdvancePaymentGroup serviceInterimAndAdvancePaymentGroup = new ServiceInterimAndAdvancePaymentGroup();
                    serviceInterimAndAdvancePaymentGroup.setServiceDetails(serviceDetails);
                    serviceInterimAndAdvancePaymentGroup.setAdvancedPaymentGroup(iapgOptional.get());
                    serviceInterimAndAdvancePaymentGroup.setStatus(ServiceSubobjectStatus.ACTIVE);
                    tempList.add(serviceInterimAndAdvancePaymentGroup);
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all interim advance payment groups
            serviceInterimAndAdvancePaymentGroupRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbIAPGs)) {
            for (ServiceInterimAndAdvancePaymentGroup dbIAPG : dbIAPGs) {
                // if user has removed the IAPG, set its status to DELETED
                if (!requestIAPGs.contains(dbIAPG.getAdvancedPaymentGroup().getId())) {
                    dbIAPG.setStatus(ServiceSubobjectStatus.DELETED);
                    serviceInterimAndAdvancePaymentGroupRepository.save(dbIAPG);
                }
            }
        }
    }

    /**
     * Adds interim advance payment groups to the product details
     *
     * @param interimAdvancePaymentGroupSet ids of the interim advance payment groups to be added
     * @param productDetails                product details to which the interim advance payment groups will be added
     * @param exceptionMessages             list of exception messages to be populated in case of errors
     */
    @Transactional
    public void addInterimAdvancePaymentGroupsToProduct(List<Long> interimAdvancePaymentGroupSet, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(interimAdvancePaymentGroupSet)) {
            List<Long> interimAdvancePaymentGroupIds = new ArrayList<>(interimAdvancePaymentGroupSet); // this

            List<ProductGroupOfInterimAndAdvancePayments> tempList = new ArrayList<>();

            for (int i = 0; i < interimAdvancePaymentGroupIds.size(); i++) {
                Long iapgId = interimAdvancePaymentGroupIds.get(i);
                // being an active group is a sufficient condition for adding it to the product
                Optional<AdvancedPaymentGroup> iapgOptional = advancedPaymentGroupRepository.findByIdAndStatusIn(iapgId, List.of(AdvancedPaymentGroupStatus.ACTIVE));
                if (iapgOptional.isEmpty()) {
                    log.error("interimAdvancePaymentGroups[%s]-can't find Interim Advance Payment Group with id: %s;".formatted(i, iapgId));
                    exceptionMessages.add("basicSettings.interimAdvancePaymentGroups[%s]-can't find Interim Advance Payment Group with id: %s;".formatted(i, iapgId));
                    continue;
                }

                ProductGroupOfInterimAndAdvancePayments productGroupOfInterimAndAdvancePayments = new ProductGroupOfInterimAndAdvancePayments();
                productGroupOfInterimAndAdvancePayments.setProductDetails(productDetails);
                productGroupOfInterimAndAdvancePayments.setInterimAdvancePaymentGroup(iapgOptional.get());
                productGroupOfInterimAndAdvancePayments.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                tempList.add(productGroupOfInterimAndAdvancePayments);
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all interim advance payment groups
            productGroupOfInterimAndAdvancePaymentsRepository.saveAll(tempList);
        }
    }


    /**
     * Updates interim advance payment groups for the product details existing version
     *
     * @param requestIAPGSet    ids of the interim advance payment groups to be updated
     * @param productDetails    product details to which the interim advance payment groups will be updated
     * @param exceptionMessages list of exception messages to be populated in case of errors
     */
    @Transactional
    public void updateProductIAPGroupsForExistingVersion(List<Long> requestIAPGSet, ProductDetails productDetails, List<String> exceptionMessages) {
        // fetch all active IAPGs for the product
        List<ProductGroupOfInterimAndAdvancePayments> dbIAPGs = productGroupOfInterimAndAdvancePaymentsRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(productDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestIAPGSet)) {
            List<Long> requestIAPGs = new ArrayList<>(requestIAPGSet); // this is for the sake of getting index of element when handling errors

            List<Long> dbIAPGIds = dbIAPGs.stream().map(ProductGroupOfInterimAndAdvancePayments::getInterimAdvancePaymentGroup).map(AdvancedPaymentGroup::getId).toList();
            List<ProductGroupOfInterimAndAdvancePayments> tempList = new ArrayList<>();

            for (int i = 0; i < requestIAPGs.size(); i++) {
                Long iapgId = requestIAPGs.get(i);
                if (!dbIAPGIds.contains(iapgId)) { // if iapg is new, its availability should be checked
                    Optional<AdvancedPaymentGroup> iapgOptional = advancedPaymentGroupRepository.findByIdAndStatusIn(iapgId, List.of(AdvancedPaymentGroupStatus.ACTIVE));
                    if (iapgOptional.isEmpty()) {
                        log.error("interimAdvancePaymentGroups[%s]-can't find Interim Advance Payment Group with id: %s;".formatted(i, iapgId));
                        exceptionMessages.add("basicSettings.interimAdvancePaymentGroups[%s]-can't find Interim Advance Payment Group with id: %s;".formatted(i, iapgId));
                        continue;
                    }

                    ProductGroupOfInterimAndAdvancePayments productGroupOfInterimAndAdvancePayments = new ProductGroupOfInterimAndAdvancePayments();
                    productGroupOfInterimAndAdvancePayments.setProductDetails(productDetails);
                    productGroupOfInterimAndAdvancePayments.setInterimAdvancePaymentGroup(iapgOptional.get());
                    productGroupOfInterimAndAdvancePayments.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    tempList.add(productGroupOfInterimAndAdvancePayments);
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all interim advance payment groups
            productGroupOfInterimAndAdvancePaymentsRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbIAPGs)) {
            for (ProductGroupOfInterimAndAdvancePayments dbIAPG : dbIAPGs) {
                // if user has removed the IAPG, set its status to DELETED
                if (!requestIAPGSet.contains(dbIAPG.getInterimAdvancePaymentGroup().getId())) {
                    dbIAPG.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
                    productGroupOfInterimAndAdvancePaymentsRepository.save(dbIAPG);
                }
            }
        }
    }

    @Override
    public CopyDomainWithVersion getGroupType() {
        return CopyDomainWithVersion.ADVANCE_PAYMENT_GROUP;
    }


    @Override
    public Page<CopyDomainWithVersionBaseResponse> findGroups(CopyDomainWithVersionBaseRequest request) {
        log.debug("Finding Advanced Payment groups with prompt: {}", request.getPrompt());
        return advancedPaymentGroupRepository
                .findByCopyDomainWithVersionBaseRequest(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                )
                .map(item -> new CopyDomainWithVersionBaseResponse(item.getId(), item.getDisplayName()));
    }

    @Override
    public List<CopyDomainWithVersionMiddleResponse> findGroupVersions(Long groupId) {
        return advancedPaymentGroupDetailsRepository.findByCopyDomainWithVersionBaseRequest(groupId);
    }

    private boolean hasEditLockedPermission() {
        return permissionService.permissionContextContainsPermissions(ADVANCED_PAYMENT_GROUP, List.of(PermissionEnum.ADVANCED_PAYMENT_GROUP_EDIT_LOCKED));
    }

}

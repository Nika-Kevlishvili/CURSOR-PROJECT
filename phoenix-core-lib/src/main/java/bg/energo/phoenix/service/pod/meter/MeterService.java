package bg.energo.phoenix.service.pod.meter;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.nomenclature.product.GridOperator;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.Scales;
import bg.energo.phoenix.model.entity.pod.meter.Meter;
import bg.energo.phoenix.model.entity.pod.meter.MeterScale;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDelivery;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.PodSubObjectStatus;
import bg.energo.phoenix.model.enums.pod.meter.MeterSearchField;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import bg.energo.phoenix.model.enums.pod.meter.MeterTableColumn;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.pod.meter.MeterListRequest;
import bg.energo.phoenix.model.request.pod.meter.MeterRequest;
import bg.energo.phoenix.model.response.pod.meter.MeterListResponse;
import bg.energo.phoenix.model.response.pod.meter.MeterResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.ScalesRepository;
import bg.energo.phoenix.repository.pod.meter.MeterRepository;
import bg.energo.phoenix.repository.pod.meter.MeterScaleRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBFinalFields;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.exception.ErrorCode.CONFLICT;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeterService {

    private final MeterMapper meterMapper;
    private final MeterRepository meterRepository;
    private final MeterScaleRepository meterScaleRepository;
    private final ScalesRepository scalesRepository;
    private final GridOperatorRepository gridOperatorRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;

    private final PermissionService permissionService;

    /**
     * Creates a meter installment and meter scales if all the validations pass.
     * Ranges are validated by a physical meter (number + grid operator combination), as well as by a pod (pod + grid operator combination).
     * It cannot be installed in the same range at different places (PODs) or at the same place (POD) at different times.
     *
     * @param request the request containing the meter data
     * @return the ID of the created meter
     */
    @Transactional
    public Long create(MeterRequest request) {
        log.debug("Creating meter with request: {}", request);

        GridOperator meterGridOperator = gridOperatorRepository
                .findByIdAndStatus(request.getGridOperatorId(), List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("gridOperatorId-Active grid operator with ID %s not found;".formatted(request.getGridOperatorId())));

        PointOfDelivery pod = pointOfDeliveryRepository
                .findByIdAndStatusIn(request.getPodId(), List.of(PodStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("podId-Active POD with ID %s not found;".formatted(request.getPodId())));

        List<String> exceptionMessages = new ArrayList<>();

        validateMatchingGridOperators(meterGridOperator, pod, exceptionMessages);
        validateScaleStatuses(request, List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages);
        validateInstallments(request.getNumber(), request, null, pod, meterGridOperator, exceptionMessages);

        // save meter and meter scales if validations pass
        Meter meter = meterMapper.fromRequestToMeterEntity(request);
        meterRepository.saveAndFlush(meter);
        meterScaleRepository.saveAll(
                request.getMeterScales()
                        .stream()
                        .map(scale -> meterMapper.fromRequestToMeterScaleEntity(meter.getId(), scale))
                        .toList()
        );

        return meter.getId();
    }


    /**
     * Displays an overridable warning message if there is an intermediate period (between two other periods) without data for electricity meter for this POD.
     * At this moment of execution, we already know that the requested installment range is not overlapping with any of the persisted ones.
     * Algorithm for checking if there is an intermediary period without data: filtering persisted ranges, inserting the requested range and sorting the list,
     * then comparing the range with the immediate neighbors.
     *
     * @param request               the request containing the meter data
     * @param persistedInstallments the list of persisted installments
     * @param pod                   the POD
     */
    private void validatePersistedInstallmentsIntermediaryPeriod(MeterRequest request,
                                                                 List<Meter> persistedInstallments,
                                                                 PointOfDelivery pod) {
        if (persistedInstallments.isEmpty() || request.isWarningAcceptedByUser()) {
            return;
        }

        List<List<LocalDate>> filteredRanges = persistedInstallments
                .stream()
                .filter(m -> m.getPodId().equals(pod.getId()))
                .map(m -> Arrays.asList(m.getInstallmentDate(), m.getRemoveDate()))
                .collect(Collectors.toList());

        if (filteredRanges.isEmpty()) {
            return;
        }

        filteredRanges.add(Arrays.asList(request.getInstallmentDate(), request.getRemoveDate()));
        filteredRanges.sort(Comparator.comparing(r -> r.get(0)));

        int indexOfRequestedRange = filteredRanges
                .stream()
                .map(r -> r.get(0).equals(request.getInstallmentDate()))
                .toList()
                .indexOf(true);

        if (indexOfRequestedRange == 0) {
            if (ChronoUnit.DAYS.between(request.getRemoveDate(), filteredRanges.get(1).get(0)) > 1) {
                handleIntermediaryPeriod();
            }
        } else if (indexOfRequestedRange == filteredRanges.size() - 1) {
            if (ChronoUnit.DAYS.between(filteredRanges.get(indexOfRequestedRange - 1).get(1), request.getInstallmentDate()) > 1) {
                handleIntermediaryPeriod();
            }
        } else {
            if (ChronoUnit.DAYS.between(filteredRanges.get(indexOfRequestedRange - 1).get(1), request.getInstallmentDate()) > 1
                    || ChronoUnit.DAYS.between(request.getRemoveDate(), filteredRanges.get(indexOfRequestedRange + 1).get(0)) > 1) {
                handleIntermediaryPeriod();
            }
        }
    }


    /**
     * Throws an exception if there is an intermediary period without data for electricity meter for this POD
     */
    private void handleIntermediaryPeriod() {
        log.error("There is an intermediary period without data for electricity meter for this POD;");
        throw new ClientException("%s-There is an intermediary period without data for electricity meter for this POD;".formatted(EPBFinalFields.WARNING_MESSAGE_INDICATOR), CONFLICT);
    }


    /**
     * Validates if the grid operator in the meter and in the POD match each other
     *
     * @param meterGridOperator Meter's grid operator
     * @param pod               the POD
     * @param exceptionMessages the list of exception messages
     */
    private void validateMatchingGridOperators(GridOperator meterGridOperator, PointOfDelivery pod, List<String> exceptionMessages) {
        if (!pod.getGridOperatorId().equals(meterGridOperator.getId())) {
            log.error("gridOperatorId-Grid operator ID %s does not match the grid operator ID of the POD %s;".formatted(meterGridOperator.getId(), pod.getGridOperatorId()));
            exceptionMessages.add("gridOperatorId-Grid operator ID %s does not match the grid operator ID of the POD %s;".formatted(meterGridOperator.getId(), pod.getGridOperatorId()));
        }
    }


    /**
     * Validates if the meter scales are in the provided statuses
     *
     * @param request           the request containing the meter data
     * @param statuses          the statuses to validate against
     * @param exceptionMessages the list of exception messages
     */
    private void validateScaleStatuses(MeterRequest request, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        for (int i = 0; i < request.getMeterScales().size(); i++) {
            Long scaleId = request.getMeterScales().get(i);
            Optional<Scales> scaleOptional = scalesRepository.findByIdAndStatuses(scaleId, statuses);
            if (scaleOptional.isEmpty()) {
                log.error("meterScales[%s]-Scale with ID %s not found in statuses %s;".formatted(i, scaleId, statuses));
                exceptionMessages.add("meterScales[%s]-Scale with ID %s not found in statuses %s;".formatted(i, scaleId, statuses));
                continue;
            }

            if (!Objects.equals(scaleOptional.get().getGridOperator().getId(), request.getGridOperatorId())) {
                log.error("meterScales[%s]-Scale with ID %s does not belong to the grid operator with ID %s;".formatted(i, scaleId, request.getGridOperatorId()));
                exceptionMessages.add("meterScales[%s]-Scale with ID %s does not belong to the grid operator with ID %s;".formatted(i, scaleId, request.getGridOperatorId()));
            }
        }
    }


    /**
     * Validates if the meter installment dates overlap with the dates of the already existing installments
     *
     * @param request           the request containing the meter data
     * @param exceptionMessages the list of exception messages
     */
    private void validateInstallmentRangeAgainstOverlapping(MeterRequest request, List<Meter> persistedInstallments, List<String> exceptionMessages) {
        // if this is the first installment, there is nothing to validate against
        if (persistedInstallments.isEmpty()) {
            return;
        }

        for (Meter persistedInstallment : persistedInstallments) {
            if (hasOverlap(request, persistedInstallment)) {
                logErrorAndAddException(request, persistedInstallment, exceptionMessages);
            }
        }
    }


    /**
     * Checks if the requested meter installment range overlaps with the persisted installment range.
     *
     * @param request              the request containing the meter data
     * @param persistedInstallment the persisted installment
     * @return true if the ranges overlap, false otherwise
     */
    private boolean hasOverlap(MeterRequest request, Meter persistedInstallment) {
        if (request.getRemoveDate() == null && persistedInstallment.getRemoveDate() == null) {
            // Both remove dates are empty; the range is not specific enough
            return true;
        }

        if (request.getRemoveDate() == null) {
            return persistedInstallment.getRemoveDate().equals(request.getInstallmentDate())
                    || persistedInstallment.getRemoveDate().isAfter(request.getInstallmentDate());
        }

        if (persistedInstallment.getRemoveDate() == null) {
            return !request.getRemoveDate().isBefore(persistedInstallment.getInstallmentDate());
        }

        // The new one is outside the persisted installment; no overlap
        return !persistedInstallment.getRemoveDate().isBefore(request.getInstallmentDate())
                && !persistedInstallment.getInstallmentDate().isAfter(request.getRemoveDate());
    }


    /**
     * Logs the error and adds the exception message to the list of exception messages
     *
     * @param request              the request containing the meter data
     * @param persistedInstallment the persisted installment
     * @param exceptionMessages    the list of exception messages
     */
    private void logErrorAndAddException(MeterRequest request, Meter persistedInstallment, List<String> exceptionMessages) {
        log.error(getOverlappingInstallmentRangeErrorMessage(request, persistedInstallment));
        exceptionMessages.add("installmentDate-Meter has an overlapping range and it is unable to save it;");
        exceptionMessages.add(getOverlappingInstallmentRangeErrorMessage(request, persistedInstallment));
    }


    /**
     * Returns the error message for overlapping installments
     *
     * @param request              the request containing the meter data
     * @param persistedInstallment the persisted installment
     * @return the error message
     */
    private static String getOverlappingInstallmentRangeErrorMessage(MeterRequest request, Meter persistedInstallment) {
        return "Meter installment range %s : %s overlaps/conflicts with the range of the existing meter (ID %s) installment %s : %s;".formatted(
                request.getInstallmentDate().toString().replace("-", "/"),
                request.getRemoveDate() == null ? "[without remove date]" : request.getRemoveDate().toString().replace("-", "/"),
                persistedInstallment.getId(),
                persistedInstallment.getInstallmentDate().toString().replace("-", "/"),
                persistedInstallment.getRemoveDate() == null ? "[without remove date]" : persistedInstallment.getRemoveDate().toString().replace("-", "/")
        );
    }


    /**
     * Updates a meter and its scales if the validations pass.
     * Ranges are validated by a physical meter (number + grid operator combination), as well as by a pod (pod + grid operator combination).
     * It cannot be installed in the same range at different places (PODs) or at the same place (POD) at different times.
     *
     * @param request the request containing the meter data
     * @param id      the ID of the meter to update
     * @return ID of the updated meter
     */
    @Transactional
    public Long update(MeterRequest request, Long id) {
        log.error("Updating meter with ID {} and request {}", id, request);

        Meter meter = meterRepository
                .findByIdAndStatusIn(id, List.of(MeterStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Meter with ID %s not found;".formatted(id)));

        GridOperator meterGridOperator = gridOperatorRepository
                .findByIdAndStatus(
                        request.getGridOperatorId(),
                        request.getGridOperatorId().equals(meter.getGridOperatorId())
                                ? List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE) // same inactive is okay
                                : List.of(NomenclatureItemStatus.ACTIVE) // if the grid operator is changed, its status should be active
                )
                .orElseThrow(() -> new DomainEntityNotFoundException("gridOperatorId-Grid operator with ID %s not found;".formatted(request.getGridOperatorId())));

        PointOfDelivery pod = pointOfDeliveryRepository
                .findByIdAndStatusIn(request.getPodId(), List.of(PodStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("podId-Active POD with ID %s not found;".formatted(request.getPodId())));

        List<String> exceptionMessages = new ArrayList<>();

        validateMatchingGridOperators(meterGridOperator, pod, exceptionMessages);
        processScales(request, meter, exceptionMessages);
        validateInstallments(meter.getNumber(), request, meter.getId(), pod, meterGridOperator, exceptionMessages);

        Meter updatedMeter = meterMapper.updateMeterFromRequest(request, meter);
        meterRepository.save(updatedMeter);

        return id;
    }


    /**
     * Validates the installments against overlapping conditions and intermediary periods.
     *
     * @param meterNumber       the meter number
     * @param request           the request containing the meter data
     * @param meterId           the ID of the meter (will be null when called from create method)
     * @param pod               the POD
     * @param meterGridOperator the grid operator of the meter
     * @param exceptionMessages the list of exception messages
     */
    private void validateInstallments(String meterNumber, MeterRequest request, Long meterId, PointOfDelivery pod, GridOperator meterGridOperator, List<String> exceptionMessages) {
        List<Meter> persistedInstallmentsByMeterAndGridOperator = meterRepository.findByNumberAndGridOperatorAndStatusIn(
                meterNumber,
                request.getGridOperatorId(),
                meterId,
                List.of(MeterStatus.ACTIVE)
        );

        List<Meter> persistedInstallmentsByPodAndGridOperator = meterRepository.findByPodAndGridOperatorAndStatusIn(
                pod.getId(),
                meterGridOperator.getId(),
                meterId,
                List.of(MeterStatus.ACTIVE)
        );

        validateInstallmentRangeAgainstOverlapping(
                request,
                EPBListUtils.getUniqueCombinedList(persistedInstallmentsByMeterAndGridOperator, persistedInstallmentsByPodAndGridOperator),
                exceptionMessages
        );

        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        validatePersistedInstallmentsIntermediaryPeriod(request, persistedInstallmentsByMeterAndGridOperator, pod);
    }


    /**
     * Processes edited scales for the meter
     *
     * @param request           {@link MeterRequest} containing the scale IDs
     * @param meter             {@link Meter} that is being edited
     * @param exceptionMessages list of exceptions to be filled in case of validation errors
     */
    private void processScales(MeterRequest request, Meter meter, List<String> exceptionMessages) {
        List<MeterScale> persistedMeterScales = meterScaleRepository.findByMeterIdAndStatusIn(meter.getId(), List.of(PodSubObjectStatus.ACTIVE));
        List<Long> persistedMeterScaleIds = persistedMeterScales.stream().map(MeterScale::getScaleId).toList();
        List<MeterScale> tempList = new ArrayList<>();

        for (int i = 0; i < request.getMeterScales().size(); i++) {
            Long scaleId = request.getMeterScales().get(i);
            if (!persistedMeterScaleIds.contains(scaleId)) {
                // if the scale is not in the persisted scales, it is a new one and should be fetched from active items
                Optional<Scales> scaleOptional = scalesRepository.findByIdAndStatuses(scaleId, List.of(NomenclatureItemStatus.ACTIVE));
                if (scaleOptional.isEmpty()) {
                    log.error("meterScales[%s]-Scale with ID %s not found in statuses %s;".formatted(i, scaleId, List.of(NomenclatureItemStatus.ACTIVE)));
                    exceptionMessages.add("meterScales[%s]-Scale with ID %s not found in statuses %s;".formatted(i, scaleId, List.of(NomenclatureItemStatus.ACTIVE)));
                    continue;
                }

                if (!Objects.equals(scaleOptional.get().getGridOperator().getId(), request.getGridOperatorId())) {
                    log.error("meterScales[%s]-Scale with ID %s does not belong to the grid operator with ID %s;".formatted(i, scaleId, request.getGridOperatorId()));
                    exceptionMessages.add("meterScales[%s]-Scale with ID %s does not belong to the grid operator with ID %s;".formatted(i, scaleId, request.getGridOperatorId()));
                }

                MeterScale meterScale = meterMapper.fromRequestToMeterScaleEntity(meter.getId(), scaleId);
                tempList.add(meterScale);
            }
        }

        // scales are mandatory for meter, so persisted scales won't be empty
        for (MeterScale meterScale : persistedMeterScales) {
            if (!request.getMeterScales().contains(meterScale.getScaleId())) {
                // remove deleted ones
                meterScale.setStatus(PodSubObjectStatus.DELETED);
                tempList.add(meterScale);
            }
        }

        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return;
        }

        meterScaleRepository.saveAll(tempList);
    }


    /**
     * Fetches detailed preview of a meter with its scales and sub entity by ID
     *
     * @param id requested {@link Meter} id
     * @return {@link MeterResponse}
     */
    public MeterResponse get(Long id) {
        log.debug("Fetching meter by ID {}", id);

        Meter meter = meterRepository
                .findByIdAndStatusIn(id, getMeterStatuses())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Meter with ID %s not found in statuses: %s;".formatted(id, getMeterStatuses())));

        MeterResponse meterResponse = meterRepository.viewById(meter.getId());
        meterResponse.setMeterScales(meterScaleRepository.findAllByMeterIdAndStatusIn(id, List.of(PodSubObjectStatus.ACTIVE)));

        return meterResponse;
    }


    /**
     * Sets DELETED status to request {@link Meter} if all validations pass
     *
     * @param id {@link Meter} ID to be deleted
     * @return {@link Meter} ID that is deleted
     */
    @Transactional
    public Long delete(Long id) {
        log.debug("Deleting meter with ID {}", id);

        Meter meter = meterRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Meter with ID %s not found;".formatted(id)));

        if (meter.getStatus().equals(MeterStatus.DELETED)) {
            log.error("id-Meter with ID %s is already deleted;".formatted(id));
            throw new OperationNotAllowedException("Meter with ID %s is already deleted;".formatted(id));
        }

        if (meterRepository.hasActiveConnectionToBillingByScale(id)) {
            log.error("You can’t delete the Meter with ID %s because it is connected to the Billing data by scales;".formatted(id));
            throw new OperationNotAllowedException("You can’t delete the Meter with ID %s because it is connected to the Billing data by scales;".formatted(id));
        }

        meter.setStatus(MeterStatus.DELETED);
        meterRepository.save(meter);
        return id;
    }


    /**
     * Fetches list of meters with their scales and sub entities
     *
     * @param request {@link MeterListRequest} containing the filters and pagination data
     * @return page of {@link MeterListResponse}
     */
    public Page<MeterListResponse> list(MeterListRequest request) {
        log.debug("Fetching meters with request {}", request);

        String sortBy = MeterTableColumn.DATE_OF_CREATION.getValue();
        if (request.getSortBy() != null && StringUtils.isNotEmpty(request.getSortBy().getValue())) {
            sortBy = request.getSortBy().getValue();
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (request.getSortDirection() != null) {
            sortDirection = request.getSortDirection();
        }

        String searchBy = MeterSearchField.ALL.name();
        if (request.getSearchBy() != null && StringUtils.isNotEmpty(request.getSearchBy().name())) {
            searchBy = request.getSearchBy().name();
        }

        return meterRepository
                .list(
                        searchBy,
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getGridOperatorIds(),
                        request.getInstallmentFrom(),
                        request.getInstallmentTo(),
                        request.getRemoveFrom(),
                        request.getRemoveTo(),
                        getMeterStatuses(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(sortDirection, sortBy)
                        )
                );
    }


    /**
     * Defines the statuses of the meters that should be fetched based on the permissions
     *
     * @return list of {@link MeterStatus}
     */
    private List<MeterStatus> getMeterStatuses() {
        List<MeterStatus> statuses = new ArrayList<>();
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.METERS, List.of(PermissionEnum.METERS_VIEW_BASIC))) {
            statuses.add(MeterStatus.ACTIVE);
        }

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.METERS, List.of(PermissionEnum.METERS_VIEW_DELETED))) {
            statuses.add(MeterStatus.DELETED);
        }

        return statuses;
    }

}

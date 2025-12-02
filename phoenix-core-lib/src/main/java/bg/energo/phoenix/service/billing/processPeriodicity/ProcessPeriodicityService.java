package bg.energo.phoenix.service.billing.processPeriodicity;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.*;
import bg.energo.phoenix.model.enums.billing.billings.BillingRunPeriodicity;
import bg.energo.phoenix.model.enums.billing.billings.BillingStatus;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.*;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.CreateProcessPeriodicityRequest;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.EditProcessPeriodicityRequest;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseDateOfMonthDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseDayOfWeekDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BasePeriodOfYearDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseProcessPeriodicityTimeIntervalDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.listing.ProcessPeriodicityListColumns;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.listing.ProcessPeriodicityListingRequest;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.listing.ProcessPeriodicitySearchByEnums;
import bg.energo.phoenix.model.response.billing.processPeriodicity.ProcessPeriodicityBillingProcessResponse;
import bg.energo.phoenix.model.response.billing.processPeriodicity.ProcessPeriodicityListingResponse;
import bg.energo.phoenix.model.response.billing.processPeriodicity.ProcessPeriodicityResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.processPeriodicity.*;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.permissions.PermissionContextEnum.PROCESS_PERIODICITY;
import static bg.energo.phoenix.permissions.PermissionEnum.PROCESS_PERIODICITY_VIEW;
import static bg.energo.phoenix.permissions.PermissionEnum.PROCESS_PERIODICITY_VIEW_DELETED;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessPeriodicityService {
    private final ProcessPeriodicityDayOfMonthsRepository processPeriodicityDayOfMonthsRepository;
    private final ProcessPeriodicityIncompatibleProcessesRepository processPeriodicityIncompatibleProcessesRepository;
    private final ProcessPeriodicityIssuingPeriodsRepository processPeriodicityIssuingPeriodsRepository;
    private final ProcessPeriodicityPeriodOfYearRepository processPeriodicityPeriodOfYearRepository;
    private final ProcessPeriodicityRepository processPeriodicityRepository;
    private final ProcessPeriodicityTimeIntervalsRepository processPeriodicityTimeIntervalsRepository;
    private final BillingRunRepository billingRunRepository;
    private final CalendarRepository calendarRepository;

    private final ProcessPeriodicityMapper processPeriodicityMapper;

    private final PermissionService permissionService;
    private final ProcessPeriodicityIncompatibleProcessesService processPeriodicityIncompatibleProcessesService;
    private final ProcessPeriodicityPeriodOfYearService processPeriodicityPeriodOfYearService;
    private final ProcessPeriodicityIssuingPeriodsService processPeriodicityIssuingPeriodsService;
    private final ProcessPeriodicityTimeIntervalsService processPeriodicityTimeIntervalsService;
    private final ProcessPeriodicityDayOfMonthsService processPeriodicityDayOfMonthsService;


    @Transactional
    public Long create(CreateProcessPeriodicityRequest request) {
        List<String> exceptionMessages = new ArrayList<>();
        List<ProcessPeriodicityIgnoreAtRuntime> ignoreAtRuntime = new ArrayList<>();
        if (BooleanUtils.isTrue(request.getIgnoreErrors())) {
            ignoreAtRuntime.add(ProcessPeriodicityIgnoreAtRuntime.ERRORS);
        }
        if (BooleanUtils.isTrue(request.getIgnoreErrors())) {
            ignoreAtRuntime.add(ProcessPeriodicityIgnoreAtRuntime.WARNINGS);
        }

        ProcessPeriodicity savedProcessPeriodicity;
        ProcessPeriodicityType processPeriodicityType = request.getProcessPeriodicityType();
        switch (processPeriodicityType) {
            case PERIODICAL -> {
                savedProcessPeriodicity = savePeriodicalProcessPeriod(request, ignoreAtRuntime, exceptionMessages);
                saveTimeIntervals(request, savedProcessPeriodicity.getId());

                if (request.getBaseProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType().equals(ProcessPeriodicityPeriodType.DAY_OF_MONTH)) {
                    saveDayOfMonths(request, savedProcessPeriodicity.getId());
                } else if (request.getBaseProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType().equals(ProcessPeriodicityPeriodType.PERIOD_OF_YEAR)) {
                    savePeriodOfYears(request, savedProcessPeriodicity.getId());
                    saveIssuingPeriods(request, savedProcessPeriodicity.getId());
                }
            }
            case ONE_TIME ->
                    savedProcessPeriodicity = saveOneTimeProcessPeriod(request, ignoreAtRuntime, exceptionMessages);
            default ->
                    throw new IllegalArgumentsProvidedException("ProcessPeriodicityType illegal enum type: [%s]".formatted(processPeriodicityType));
        }

        saveIncompatibleProcesses(request, savedProcessPeriodicity.getId(), exceptionMessages);

        return savedProcessPeriodicity.getId();
    }

    @Transactional
    public Boolean editProcessPeriodicity(EditProcessPeriodicityRequest editProcessPeriodicityRequest) {
        log.info("edit process periodicity with id: %s ".formatted(editProcessPeriodicityRequest.getId()));
        List<String> exceptionMessages = new ArrayList<>();
        if (!processPeriodicityRepository.existsById(editProcessPeriodicityRequest.getId())) {
            throw new OperationNotAllowedException("Process periodicity with id %s does not exist".formatted(editProcessPeriodicityRequest.getId()));
        }
        ProcessPeriodicity processPeriodicity = processPeriodicityRepository.findById(editProcessPeriodicityRequest.getId()).get();
        if (!processPeriodicity.getProcessPeriodicityType().equals(editProcessPeriodicityRequest.getProcessPeriodicityType())) {
            throw new OperationNotAllowedException("Process periodicity type cannot be changed");
        }
        if (!processPeriodicity.getStatus().equals(EntityStatus.ACTIVE)) {
            throw new OperationNotAllowedException("Process periodicity cannot be changed");
        }
        updateProcessPeriodicity(processPeriodicity, editProcessPeriodicityRequest, exceptionMessages);
        return true;
    }

    private void updateProcessPeriodicity(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest, List<String> exceptionMessages) {
        processPeriodicityIncompatibleProcessesService.validateIncompatibleBillingRunList(processPeriodicity.getId(), editProcessPeriodicityRequest.getIncompatibleProcesses(), exceptionMessages);
        validateCircularDependency(processPeriodicity,editProcessPeriodicityRequest,exceptionMessages);
        updateGeneralProcessPeriodicityOptions(processPeriodicity, editProcessPeriodicityRequest);
        switch (editProcessPeriodicityRequest.getProcessPeriodicityType()) {
            case PERIODICAL ->
                    updatePeriodicalProcessPeriodicity(processPeriodicity, editProcessPeriodicityRequest, exceptionMessages);
            case ONE_TIME ->
                    updateOneTimeProcessPeriodicity(processPeriodicity, editProcessPeriodicityRequest, exceptionMessages);
        }
    }

    private void validateCircularDependency(ProcessPeriodicity periodicity,EditProcessPeriodicityRequest editProcessPeriodicityRequest, List<String> exceptionMessages) {
        if(editProcessPeriodicityRequest.getStartAfterProcessId()!=null&&processPeriodicityRepository.validateCircularDependency(editProcessPeriodicityRequest.getStartAfterProcessId(),periodicity.getId())){
            exceptionMessages.add("startAfterProcessId-Start after process billing run will have circular dependencies;");
        }

    }


    private void updateGeneralProcessPeriodicityOptions(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest) {
        List<ProcessPeriodicityIgnoreAtRuntime> ignoreAtRuntime = new ArrayList<>();
        if (BooleanUtils.isTrue(editProcessPeriodicityRequest.getIgnoreErrors())) {
            ignoreAtRuntime.add(ProcessPeriodicityIgnoreAtRuntime.ERRORS);
        }
        if (BooleanUtils.isTrue(editProcessPeriodicityRequest.getIgnoreWarnings())) {
            ignoreAtRuntime.add(ProcessPeriodicityIgnoreAtRuntime.WARNINGS);
        }
        processPeriodicityMapper.fillUpdatedProcessPeriodicityGeneralOptions(processPeriodicity,
                editProcessPeriodicityRequest.getName(), ignoreAtRuntime);
    }

    private void updateOneTimeProcessPeriodicity(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest, List<String> exceptionMessages) {
        LocalDateTime processStartDateAndTime = null;
        Long startAfterProcessId = null;
        ProcessPeriodicityBillingProcessStart processPeriodicityBillingProcessStart = editProcessPeriodicityRequest.getOneTimeProcessPeriodicityDto().getProcessPeriodicityBillingProcessStart();
        switch (processPeriodicityBillingProcessStart) {
            case AFTER_PROCESS -> startAfterProcessId = editProcessPeriodicityRequest.getStartAfterProcessId();
            case DATE_AND_TIME ->
                    processStartDateAndTime = LocalDateTime.of(editProcessPeriodicityRequest.getOneTimeProcessPeriodicityDto().getOneTimeStartDate(), editProcessPeriodicityRequest.getOneTimeProcessPeriodicityDto().getOneTimeStartTime());
        }
        processPeriodicityIncompatibleProcessesService.validateStartAfterBillingRun(processPeriodicity.getId(), editProcessPeriodicityRequest.getStartAfterProcessId(), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        processPeriodicityMapper.fillUpdatedOneTimeProcessPeriodicity(processPeriodicity, startAfterProcessId, processPeriodicityBillingProcessStart, processStartDateAndTime);
        processPeriodicityRepository.save(processPeriodicity);
    }

    private void updatePeriodicalProcessPeriodicity(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest, List<String> exceptionMessages) {
        ProcessPeriodicityPeriodType oldProcessPeriodicityPeriodType = updatePeriodicalProcessPeriodicityGeneralOptions(processPeriodicity, editProcessPeriodicityRequest, exceptionMessages);
        switch (editProcessPeriodicityRequest.getProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType()) {
            case DAY_OF_MONTH ->
                    updateDayOfMonth(processPeriodicity, editProcessPeriodicityRequest, exceptionMessages, oldProcessPeriodicityPeriodType);
            case FORMULA ->
                    updateFormula(processPeriodicity, editProcessPeriodicityRequest, oldProcessPeriodicityPeriodType);
            case PERIOD_OF_YEAR ->
                    updatePeriodOfYear(processPeriodicity, editProcessPeriodicityRequest, exceptionMessages, oldProcessPeriodicityPeriodType);
        }
    }

    private ProcessPeriodicityPeriodType updatePeriodicalProcessPeriodicityGeneralOptions(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest, List<String> exceptionMessages) {
        ProcessPeriodicityPeriodType oldProcessPeriodicityPeriodType = updatePeriodicalProcessPeriodicityOptions(processPeriodicity, editProcessPeriodicityRequest, exceptionMessages);
        updateTimeIntervals(processPeriodicity, editProcessPeriodicityRequest, exceptionMessages);
        return oldProcessPeriodicityPeriodType;
    }

    private ProcessPeriodicityPeriodType updatePeriodicalProcessPeriodicityOptions(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest, List<String> exceptionMessages) {
        List<ProcessPeriodicityExclude> processPeriodicityExcludes = new ArrayList<>();
        ProcessPeriodicityPeriodType oldProcessPeriodicityPeriodType = processPeriodicity.getProcessPeriodicityPeriodType();
        if (BooleanUtils.isTrue(editProcessPeriodicityRequest.getIsHolidaysExcluded())) {
            processPeriodicityExcludes.add(ProcessPeriodicityExclude.HOLIDAYS);
        }
        if (BooleanUtils.isTrue(editProcessPeriodicityRequest.getIsWeekendsExcluded())) {
            processPeriodicityExcludes.add(ProcessPeriodicityExclude.WEEKENDS);
        }
        calendarNomenclatureValidation(editProcessPeriodicityRequest.getCalendarId(), exceptionMessages);
        processPeriodicityIncompatibleProcessesService.validateStartAfterBillingRun(processPeriodicity.getId(), editProcessPeriodicityRequest.getStartAfterProcessId(), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        processPeriodicityMapper.fillUpdatedPeriodicProcessPeriodicity(
                processPeriodicity,
                editProcessPeriodicityRequest.getStartAfterProcessId(),
                processPeriodicityExcludes,
                editProcessPeriodicityRequest.getCalendarId(),
                editProcessPeriodicityRequest.getChangeTo(),
                editProcessPeriodicityRequest.getProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType());
        return oldProcessPeriodicityPeriodType;
    }

    private void updateTimeIntervals(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest, List<String> exceptionMessages) {
        processPeriodicityTimeIntervalsService.validateTimeIntervals(processPeriodicity.getId(), editProcessPeriodicityRequest.getStartTimeIntervals(), exceptionMessages);
    }

    private void updateFormula(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest, ProcessPeriodicityPeriodType oldProcessPeriodicityPeriodType) {
        switch (oldProcessPeriodicityPeriodType) {
            case PERIOD_OF_YEAR -> {
                processPeriodicity.setYearRound(false);
                processPeriodicityPeriodOfYearService.deleteProcessPeriodicityPeriodOfYear(processPeriodicity.getId());
                processPeriodicityIssuingPeriodsService.deleteProcessPeriodicityIssuingPeriods(processPeriodicity.getId());
            }
            case DAY_OF_MONTH -> processPeriodicityDayOfMonthsService.deleteDayOfMonths(processPeriodicity.getId());
        }
        processPeriodicity.setRruleFormula(editProcessPeriodicityRequest.getProcessPeriodicityPeriodOptionsDto().getRRUle());
    }

    private void updateDayOfMonth(ProcessPeriodicity processPeriodicity, EditProcessPeriodicityRequest editProcessPeriodicityRequest, List<String> exceptionMessages, ProcessPeriodicityPeriodType oldProcessPeriodicityPeriodType) {
        switch (oldProcessPeriodicityPeriodType) {
            case PERIOD_OF_YEAR -> {
                processPeriodicity.setYearRound(false);
                processPeriodicityPeriodOfYearService.deleteProcessPeriodicityPeriodOfYear(processPeriodicity.getId());
                processPeriodicityIssuingPeriodsService.deleteProcessPeriodicityIssuingPeriods(processPeriodicity.getId());
            }
            case FORMULA -> processPeriodicity.setRruleFormula(null);
        }
        processPeriodicityDayOfMonthsService.validateDayOfMonth(
                processPeriodicity.getId(),
                editProcessPeriodicityRequest.getProcessPeriodicityPeriodOptionsDto().getDateOfMonths(),
                exceptionMessages);
    }

    private void updatePeriodOfYear(ProcessPeriodicity processPeriodicity,
                                    EditProcessPeriodicityRequest editProcessPeriodicityRequest,
                                    List<String> exceptionMessages,
                                    ProcessPeriodicityPeriodType oldProcessPeriodicityPeriodType) {
        switch (oldProcessPeriodicityPeriodType) {
            case DAY_OF_MONTH -> processPeriodicityDayOfMonthsService.deleteDayOfMonths(processPeriodicity.getId());
            case FORMULA -> processPeriodicity.setRruleFormula(null);
        }
        processPeriodicity.setYearRound(editProcessPeriodicityRequest.getProcessPeriodicityPeriodOptionsDto().getDayOfWeekAndPeriodOfYearDto().getYearAround());
        processPeriodicityPeriodOfYearService.validatePeriodOfYear(processPeriodicity.getId(),
                editProcessPeriodicityRequest.getProcessPeriodicityPeriodOptionsDto().getDayOfWeekAndPeriodOfYearDto().getDaysOfWeek(),
                exceptionMessages);
        processPeriodicityIssuingPeriodsService.validateIssuingPeriods(processPeriodicity.getId(), editProcessPeriodicityRequest.getProcessPeriodicityPeriodOptionsDto().getDayOfWeekAndPeriodOfYearDto().getPeriodsOfYear(), exceptionMessages);
    }

    public Page<ProcessPeriodicityListingResponse> list(ProcessPeriodicityListingRequest request) {
        Sort.Order order = new Sort.Order(Objects.requireNonNullElse(request.getDirection(), Sort.Direction.DESC), checkSortField(request));
        List<ProcessPeriodicityType> periodicity = request.getPeriodicity();
        return processPeriodicityRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                periodicity == null ? new ArrayList<>() : periodicity.stream()
                        .map(ProcessPeriodicityType::name).toList(),
                getSearchByEnum(request),
                statusesForListing(request.getStatuses()),
                PageRequest.of(request.getPage(), request.getSize(), Sort.by(order))).map(ProcessPeriodicityListingResponse::new);
    }

    private ProcessPeriodicity savePeriodicalProcessPeriod(CreateProcessPeriodicityRequest request, List<ProcessPeriodicityIgnoreAtRuntime> ignoreAtRuntime, List<String> exceptionMessages) {
        List<ProcessPeriodicityExclude> processPeriodicityExcludes = new ArrayList<>();
        ProcessPeriodicityPeriodType processPeriodicityPeriodType = request.getBaseProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType();
        String rRuleFormula = null;
        Boolean isYearAround = false;

        if (BooleanUtils.isTrue(request.getIsHolidaysExcluded())) {
            processPeriodicityExcludes.add(ProcessPeriodicityExclude.HOLIDAYS);
        }
        if (BooleanUtils.isTrue(request.getIsWeekendsExcluded())) {
            processPeriodicityExcludes.add(ProcessPeriodicityExclude.WEEKENDS);
        }

        switch (processPeriodicityPeriodType) {
            case PERIOD_OF_YEAR ->
                    isYearAround = request.getBaseProcessPeriodicityPeriodOptionsDto().getBaseDayOfWeekAndPeriodOfYearDto().getYearAround();
            case FORMULA -> rRuleFormula = request.getBaseProcessPeriodicityPeriodOptionsDto().getRRUle();
        }

        calendarNomenclatureValidation(request.getCalendarId(), exceptionMessages);
        startAfterProcessValidation(request.getStartAfterProcessId(), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        ProcessPeriodicity processPeriodicity = processPeriodicityMapper.mapToProcessPeriodicity(request.getName(),
                request.getProcessPeriodicityType(),
                request.getStartAfterProcessId(),
                ignoreAtRuntime,
                processPeriodicityExcludes,
                request.getCalendarId(),
                request.getChangeTo(),
                processPeriodicityPeriodType,
                rRuleFormula,
                isYearAround,
                null,
                null);

        return processPeriodicityRepository.saveAndFlush(processPeriodicity);
    }

    private void startAfterProcessValidation(Long startAfterProcessId, List<String> exceptionMessages) {
        if (startAfterProcessId != null && billingRunRepository.findBillingRunByIdAndRunPeriodicityAndStatusIsNot(startAfterProcessId, BillingRunPeriodicity.PERIODIC, BillingStatus.DELETED).isEmpty()) {
            exceptionMessages.add("startAfterProcessId-startAfterProcess with id [%s] cannot be selected;".formatted(startAfterProcessId));
        }
    }

    private void calendarNomenclatureValidation(Long calendarId, List<String> exceptionMessages) {
        if (calendarId != null && calendarRepository.findByIdAndStatusIsIn(calendarId, List.of(NomenclatureItemStatus.ACTIVE)).isEmpty()) {
            exceptionMessages.add("calendarId-calendar with id [%s] is not active;".formatted(calendarId));
        }
    }

    private ProcessPeriodicity saveOneTimeProcessPeriod(CreateProcessPeriodicityRequest request, List<ProcessPeriodicityIgnoreAtRuntime> ignoreAtRuntime, List<String> exceptionMessages) {
        LocalDateTime processStartDateAndTIme = null;
        Long startAfterProcessId = null;
        ProcessPeriodicityBillingProcessStart processPeriodicityBillingProcessStart = request.getOneTimeProcessPeriodicityDto().getProcessPeriodicityBillingProcessStart();
        switch (processPeriodicityBillingProcessStart) {
            case AFTER_PROCESS -> startAfterProcessId = request.getStartAfterProcessId();
            case DATE_AND_TIME ->
                    processStartDateAndTIme = LocalDateTime.of(request.getOneTimeProcessPeriodicityDto().getOneTimeStartDate(), request.getOneTimeProcessPeriodicityDto().getOneTimeStartTime());
        }

        startAfterProcessValidation(request.getStartAfterProcessId(), exceptionMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
        ProcessPeriodicity processPeriodicity = processPeriodicityMapper.mapToProcessPeriodicity(request.getName(),
                request.getProcessPeriodicityType(),
                startAfterProcessId,
                ignoreAtRuntime,
                null,
                null,
                null,
                null,
                null,
                false,
                processPeriodicityBillingProcessStart,
                processStartDateAndTIme);
        return processPeriodicityRepository.saveAndFlush(processPeriodicity);
    }

    private void saveIncompatibleProcesses(CreateProcessPeriodicityRequest request, Long savedProcessPeriodicityId, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(request.getIncompatibleProcesses())) {
            List<ProcessPeriodicityIncompatibleProcesses> temp = new ArrayList<>();
            List<Long> incompatibleProcesses = request.getIncompatibleProcesses();
            for (int i = 0; i < incompatibleProcesses.size(); i++) {
                Long incompatibleProcessId = incompatibleProcesses.get(i);
                if (incompatibleProcessId != null && billingRunRepository.findBillingRunByIdAndRunPeriodicityAndStatusIsNot(incompatibleProcessId, BillingRunPeriodicity.PERIODIC, BillingStatus.DELETED).isEmpty()
                ) {
                    exceptionMessages.add("incompatibleProcesses[%s]-incompatibleProcessId with id [%s] cannot be selected;".formatted(i, incompatibleProcessId));
                }
                ProcessPeriodicityIncompatibleProcesses incompatibleProcess = processPeriodicityMapper.mapToProcessPeriodicityIncompatibleProcesses(incompatibleProcessId);
                incompatibleProcess.setProcessPeriodicityId(savedProcessPeriodicityId);
                temp.add(incompatibleProcess);
            }
            EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(exceptionMessages, log);
            processPeriodicityIncompatibleProcessesRepository.saveAll(temp);
        }
    }

    private void saveDayOfMonths(CreateProcessPeriodicityRequest request, Long savedProcessPeriodicityId) {
        List<BaseDateOfMonthDto> baseDateOfMonthDtoSet = request.getBaseProcessPeriodicityPeriodOptionsDto().getDateOfMonths();
        List<ProcessPeriodicityDayOfMonths> tempList = new ArrayList<>();
        for (BaseDateOfMonthDto baseDateOfMonthDto : baseDateOfMonthDtoSet) {
            ProcessPeriodicityDayOfMonths processPeriodicityDayOfMonths = processPeriodicityMapper.mapToProcessPeriodicityDayOfMonths(baseDateOfMonthDto);
            processPeriodicityDayOfMonths.setProcessPeriodicityId(savedProcessPeriodicityId);
            tempList.add(processPeriodicityDayOfMonths);
        }
        processPeriodicityDayOfMonthsRepository.saveAll(tempList);
    }

    private void savePeriodOfYears(CreateProcessPeriodicityRequest request, Long savedProcessPeriodicityId) {
        List<ProcessPeriodicityPeriodOfYear> temp = new ArrayList<>();
        List<BaseDayOfWeekDto> dayOfWeekDtoList = request.getBaseProcessPeriodicityPeriodOptionsDto().getBaseDayOfWeekAndPeriodOfYearDto().getDaysOfWeek();
        for (BaseDayOfWeekDto dto : dayOfWeekDtoList) {
            ProcessPeriodicityPeriodOfYear processPeriodicityPeriodOfYear = processPeriodicityMapper.mapToProcessPeriodicityPeriodOfYear(dto);
            processPeriodicityPeriodOfYear.setProcessPeriodicityId(savedProcessPeriodicityId);
            temp.add(processPeriodicityPeriodOfYear);
        }
        processPeriodicityPeriodOfYearRepository.saveAll(temp);

    }

    private void saveIssuingPeriods(CreateProcessPeriodicityRequest request, Long savedProcessPeriodicityId) {
        List<ProcessPeriodicityIssuingPeriods> temp = new ArrayList<>();
        List<BasePeriodOfYearDto> basePeriodOfYearDtoList = request.getBaseProcessPeriodicityPeriodOptionsDto().getBaseDayOfWeekAndPeriodOfYearDto().getPeriodsOfYear();
        for (BasePeriodOfYearDto dto : basePeriodOfYearDtoList) {
            ProcessPeriodicityIssuingPeriods processPeriodicityIssuingPeriods = processPeriodicityMapper.mapToProcessPeriodicityIssuingPeriods(dto);
            processPeriodicityIssuingPeriods.setProcessPeriodicityId(savedProcessPeriodicityId);
            temp.add(processPeriodicityIssuingPeriods);
        }
        processPeriodicityIssuingPeriodsRepository.saveAll(temp);
    }

    private void saveTimeIntervals(CreateProcessPeriodicityRequest request, Long savedProcessPeriodicityId) {
        List<ProcessPeriodicityTimeIntervals> temp = new ArrayList<>();
        List<BaseProcessPeriodicityTimeIntervalDto> timeIntervals = request.getStartTimeIntervals();
        for (BaseProcessPeriodicityTimeIntervalDto dto : timeIntervals) {
            ProcessPeriodicityTimeIntervals processPeriodicityTimeIntervals = processPeriodicityMapper.mapToProcessPeriodicityTimeIntervals(dto);
            processPeriodicityTimeIntervals.setProcessPeriodicityId(savedProcessPeriodicityId);
            temp.add(processPeriodicityTimeIntervals);
        }
        processPeriodicityTimeIntervalsRepository.saveAll(temp);
    }

    public ProcessPeriodicityResponse getProcessPeriodicity(Long processPeriodicityId) {
        if (!processPeriodicityRepository.existsById(processPeriodicityId)) {
            throw new OperationNotAllowedException("Process periodicity does not exist");
        }
        if (!permissionService.permissionContextContainsPermissions(PROCESS_PERIODICITY, List.of(PROCESS_PERIODICITY_VIEW_DELETED))
                && processPeriodicityRepository.existsByIdAndStatus(processPeriodicityId, EntityStatus.DELETED)) {
            throw new OperationNotAllowedException("access denied");
        } else if (!permissionService.permissionContextContainsPermissions(PROCESS_PERIODICITY, List.of(PROCESS_PERIODICITY_VIEW))
                && processPeriodicityRepository.existsByIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE)) {
            throw new OperationNotAllowedException("access denied");
        }

        return getProcessPeriodicityResponse(processPeriodicityId);
    }


    private ProcessPeriodicityResponse getProcessPeriodicityResponse(Long processPeriodicityId) {
        Optional<ProcessPeriodicity> processPeriodicityOptional = processPeriodicityRepository.findById(processPeriodicityId);
        if (processPeriodicityOptional.isEmpty()) {
            throw new OperationNotAllowedException("ProcessPeriodicity not found");
        }
        ProcessPeriodicity processPeriodicity = processPeriodicityOptional.get();
        ProcessPeriodicityResponse processPeriodicityResponse = fillGeneralProcessPeriodicityOptions(processPeriodicity);
        switch (processPeriodicity.getProcessPeriodicityType()) {
            case PERIODICAL -> {
                fillPeriodicGeneralOptions(processPeriodicity, processPeriodicityResponse);
                if (processPeriodicity.getProcessPeriodicityPeriodType().equals(ProcessPeriodicityPeriodType.DAY_OF_MONTH)) {
                    fillDayOfMonthOptions(processPeriodicityId, processPeriodicityResponse);
                } else if (processPeriodicity.getProcessPeriodicityPeriodType().equals(ProcessPeriodicityPeriodType.FORMULA)) {
                    processPeriodicityMapper.fillFormulaOption(processPeriodicity.getRruleFormula(), processPeriodicityResponse);
                } else if (processPeriodicity.getProcessPeriodicityPeriodType().equals(ProcessPeriodicityPeriodType.PERIOD_OF_YEAR)) {
                    fillPeriodOfYear(processPeriodicityId, processPeriodicity.getYearRound(), processPeriodicityResponse);
                }
            }
            case ONE_TIME -> {
                processPeriodicityMapper.fillOneTimeDtoWithGeneralOptions(processPeriodicity.getProcessPeriodicityBillingProcessStart(), processPeriodicityResponse);
                if (processPeriodicity.getProcessPeriodicityBillingProcessStart().equals(ProcessPeriodicityBillingProcessStart.AFTER_PROCESS)) {
                    fillStartAfterOption(processPeriodicity.getStartAfterProcessBillingId(), processPeriodicityResponse);
                } else if (processPeriodicity.getProcessPeriodicityBillingProcessStart().equals(ProcessPeriodicityBillingProcessStart.DATE_AND_TIME)) {
                    fillOneTimeStartDateOption(processPeriodicity, processPeriodicityResponse);
                }
            }
        }

        return processPeriodicityResponse;
    }

    private ProcessPeriodicityResponse fillGeneralProcessPeriodicityOptions(ProcessPeriodicity processPeriodicity) {
        List<ProcessPeriodicityIncompatibleProcesses> processPeriodicityIncompatibleProcesses = processPeriodicityIncompatibleProcessesRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicity.getId(), EntityStatus.ACTIVE);
        List<ProcessPeriodicityBillingProcessResponse> processPeriodicityBillingProcessResponses = new ArrayList<>();
        for (ProcessPeriodicityIncompatibleProcesses billing : processPeriodicityIncompatibleProcesses) {
            processPeriodicityBillingProcessResponses.add(processPeriodicityMapper.mapToProcessPeriodicityBillingProcess(
                    billingRunRepository.findById(billing.getIncompatibleBillingId()).get().getBillingNumber(), billing.getIncompatibleBillingId()));
        }
        ProcessPeriodicityResponse processPeriodicityResponse = processPeriodicityMapper.mapProcessPeriodicityGeneralOptions(processPeriodicity,
                processPeriodicityIncompatibleProcesses.stream().map(ProcessPeriodicityIncompatibleProcesses::getIncompatibleBillingId).toList());
        processPeriodicityResponse.setIncompatibleProcesses(processPeriodicityBillingProcessResponses);
        return processPeriodicityResponse;
    }

    private void fillPeriodicGeneralOptions(ProcessPeriodicity processPeriodicity, ProcessPeriodicityResponse processPeriodicityResponse) {
        List<ProcessPeriodicityTimeIntervals> intervals = processPeriodicityTimeIntervalsRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicity.getId(), EntityStatus.ACTIVE);
        processPeriodicityMapper.mapPeriodicalGeneralOptions(processPeriodicity,
                processPeriodicityResponse,
                processPeriodicityMapper.mapToIntervalDtoList(intervals));
        if (processPeriodicity.getStartAfterProcessBillingId() != null) {
            fillStartAfterOption(processPeriodicity.getStartAfterProcessBillingId(), processPeriodicityResponse);
        }
    }

    private void fillStartAfterOption(Long billingId, ProcessPeriodicityResponse processPeriodicityResponse) {
        processPeriodicityMapper.fillProcessPeriodicityBillingProcess(processPeriodicityResponse,
                billingRunRepository.findById(billingId).get().getBillingNumber(),
                billingId);
    }

    private void fillDayOfMonthOptions(Long processPeriodicityId, ProcessPeriodicityResponse processPeriodicityResponse) {
        List<ProcessPeriodicityDayOfMonths> processPeriodicityDayOfMonthsList = processPeriodicityDayOfMonthsRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        processPeriodicityMapper.fillDayOfMonthOption(processPeriodicityResponse,
                processPeriodicityMapper.mapToDateOfMonthsList(processPeriodicityDayOfMonthsList));
    }

    private void fillPeriodOfYear(Long processPeriodicityId, Boolean yearAround, ProcessPeriodicityResponse processPeriodicityResponse) {
        List<ProcessPeriodicityPeriodOfYear> processPeriodicityPeriodOfYears = processPeriodicityPeriodOfYearRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        List<ProcessPeriodicityIssuingPeriods> processPeriodicityIssuingPeriods = processPeriodicityIssuingPeriodsRepository.findAllByProcessPeriodicityIdAndStatus(processPeriodicityId, EntityStatus.ACTIVE);
        processPeriodicityMapper.fillPeriodOfYearOption(processPeriodicityResponse,
                processPeriodicityMapper.mapToDayOfWeekAndPeriodOfYearDto(processPeriodicityMapper.mapToDayOfWeekDtoList(processPeriodicityPeriodOfYears),
                        yearAround,
                        processPeriodicityMapper.mapToIssuingPeriodsDtoList(processPeriodicityIssuingPeriods)));
    }

    private void fillOneTimeStartDateOption(ProcessPeriodicity processPeriodicity, ProcessPeriodicityResponse processPeriodicityResponse) {
        processPeriodicityResponse.getOneTimeProcessPeriodicityDto().setOneTimeStartDate(processPeriodicity.getBillingProcessStartDate().toLocalDate());
        processPeriodicityResponse.getOneTimeProcessPeriodicityDto().setOneTimeStartTime(processPeriodicity.getBillingProcessStartDate().toLocalTime());
    }

    public Boolean canProcessPeriodicityBeDeleted(Long processPeriodicityId) {
        if (!processPeriodicityRepository.existsById(processPeriodicityId)) {
            throw new OperationNotAllowedException("Process periodicity does not exist");
        }
        return processPeriodicityRepository.activeConnectionCount(processPeriodicityId) < 1;
    }

    public Long deleteProcessPeriodicity(Long processPeriodicityId) {
        if (!processPeriodicityRepository.existsById(processPeriodicityId)) {
            throw new OperationNotAllowedException("Process periodicity does not exist");
        }
        if (processPeriodicityRepository.activeConnectionCount(processPeriodicityId) >= 1) {
            throw new OperationNotAllowedException("You can’t delete the process periodicity object because it is connected to the Periodic Billing Run;");
        }
        ProcessPeriodicity processPeriodicity = processPeriodicityRepository.findById(processPeriodicityId).get();
        processPeriodicity.setStatus(EntityStatus.DELETED);
        return processPeriodicityRepository.saveAndFlush(processPeriodicity).getId();
    }

    private String checkSortField(ProcessPeriodicityListingRequest request) {
        if (request.getSortBy() == null) {
            return ProcessPeriodicityListColumns.CREATE_DATE.getValue();
        } else
            return request.getSortBy().getValue();
    }

    private String getSearchByEnum(ProcessPeriodicityListingRequest request) {
        String searchByField;
        if (request.getSearchBy() != null) {
            searchByField = request.getSearchBy().getValue();
        } else
            searchByField = ProcessPeriodicitySearchByEnums.ALL.getValue();
        return searchByField;
    }

    private List<String> statusesForListing(List<EntityStatus> requestStatuses) {
        List<String> statuses = new ArrayList<>();
        if ((CollectionUtils.isEmpty(requestStatuses) || requestStatuses.contains(EntityStatus.ACTIVE))
                && permissionService.permissionContextContainsPermissions(PROCESS_PERIODICITY, List.of(PROCESS_PERIODICITY_VIEW))) {
            statuses.add(EntityStatus.ACTIVE.name());
        }

        if ((CollectionUtils.isEmpty(requestStatuses) || requestStatuses.contains(EntityStatus.DELETED))
                && permissionService.permissionContextContainsPermissions(PROCESS_PERIODICITY, List.of(PROCESS_PERIODICITY_VIEW_DELETED))) {
            statuses.add(EntityStatus.DELETED.name());
        }
        return statuses;
    }

}

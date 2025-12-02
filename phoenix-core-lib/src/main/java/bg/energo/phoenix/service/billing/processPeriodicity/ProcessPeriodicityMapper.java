package bg.energo.phoenix.service.billing.processPeriodicity;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.processPeriodicity.*;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.*;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.*;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.OneTimeProcessPeriodicityDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseDateOfMonthDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseDayOfWeekDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BasePeriodOfYearDto;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.BaseProcessPeriodicityTimeIntervalDto;
import bg.energo.phoenix.model.response.billing.processPeriodicity.ProcessPeriodicityBillingProcessResponse;
import bg.energo.phoenix.model.response.billing.processPeriodicity.ProcessPeriodicityResponse;
import bg.energo.phoenix.model.response.nomenclature.terms.CalendarShortResponse;
import bg.energo.phoenix.repository.nomenclature.product.terms.CalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;

@Service
@RequiredArgsConstructor
public class ProcessPeriodicityMapper {

    private final CalendarRepository calendarRepository;


    public ProcessPeriodicity mapToProcessPeriodicity(String name,
                                                      ProcessPeriodicityType processPeriodicityType,
                                                      Long startAfterProcessBillingId,
                                                      List<ProcessPeriodicityIgnoreAtRuntime> ignoreAtRuntime,
                                                      List<ProcessPeriodicityExclude> processPeriodicityExcludes,
                                                      Long calendarId,
                                                      ProcessPeriodicityChangeTo processPeriodicityChangeTo,
                                                      ProcessPeriodicityPeriodType processPeriodicityPeriodType,
                                                      String rruleFormula,
                                                      Boolean yearRound,
                                                      ProcessPeriodicityBillingProcessStart processPeriodicityBillingProcessStart,
                                                      LocalDateTime billingProcessStartDate) {
        ProcessPeriodicity processPeriodicity = new ProcessPeriodicity();
        processPeriodicity.setName(name);
        processPeriodicity.setProcessPeriodicityType(processPeriodicityType);
        processPeriodicity.setIgnoreAtRuntime(ignoreAtRuntime);
        processPeriodicity.setStartAfterProcessBillingId(startAfterProcessBillingId);
        processPeriodicity.setProcessPeriodicityExcludes(processPeriodicityExcludes);
        processPeriodicity.setCalendarId(calendarId);
        processPeriodicity.setProcessPeriodicityChangeTo(processPeriodicityChangeTo);
        processPeriodicity.setProcessPeriodicityPeriodType(processPeriodicityPeriodType);
        processPeriodicity.setRruleFormula(rruleFormula);
        processPeriodicity.setYearRound(yearRound);
        processPeriodicity.setProcessPeriodicityBillingProcessStart(processPeriodicityBillingProcessStart);
        processPeriodicity.setBillingProcessStartDate(billingProcessStartDate);
        processPeriodicity.setStatus(EntityStatus.ACTIVE);
        return processPeriodicity;
    }

    public ProcessPeriodicityPeriodOfYear mapToProcessPeriodicityPeriodOfYear(BaseDayOfWeekDto baseDayOfWeekDto) {
        ProcessPeriodicityPeriodOfYear processPeriodicityPeriodOfYear = new ProcessPeriodicityPeriodOfYear();
        processPeriodicityPeriodOfYear.setDays(baseDayOfWeekDto.getDays());
        processPeriodicityPeriodOfYear.setWeek(baseDayOfWeekDto.getWeek());
        processPeriodicityPeriodOfYear.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityPeriodOfYear;
    }

    public ProcessPeriodicityIssuingPeriods mapToProcessPeriodicityIssuingPeriods(BasePeriodOfYearDto dto) {
        ProcessPeriodicityIssuingPeriods processPeriodicityIssuingPeriods = new ProcessPeriodicityIssuingPeriods();
        processPeriodicityIssuingPeriods.setPeriod_from(dto.getStartDate());
        processPeriodicityIssuingPeriods.setPeriod_to(dto.getEndDate());
        processPeriodicityIssuingPeriods.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityIssuingPeriods;
    }

    public ProcessPeriodicityTimeIntervals mapToProcessPeriodicityTimeIntervals(BaseProcessPeriodicityTimeIntervalDto dto) {
        ProcessPeriodicityTimeIntervals processPeriodicityTimeIntervals = new ProcessPeriodicityTimeIntervals();
        processPeriodicityTimeIntervals.setStartTime(dto.getStartTime());
        processPeriodicityTimeIntervals.setEndTime(dto.getEndTime());
        processPeriodicityTimeIntervals.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityTimeIntervals;
    }

    public ProcessPeriodicityDayOfMonths mapToProcessPeriodicityDayOfMonths(BaseDateOfMonthDto baseDateOfMonthDto) {
        ProcessPeriodicityDayOfMonths processPeriodicityDayOfMonths = new ProcessPeriodicityDayOfMonths();
        processPeriodicityDayOfMonths.setMonth(baseDateOfMonthDto.getMonth());
        processPeriodicityDayOfMonths.setMonthNumber(baseDateOfMonthDto.getMonthNumbers());
        processPeriodicityDayOfMonths.setStatus(EntityStatus.ACTIVE);

        return processPeriodicityDayOfMonths;
    }

    public ProcessPeriodicityIncompatibleProcesses mapToProcessPeriodicityIncompatibleProcesses(Long incompatibleBillingId) {
        ProcessPeriodicityIncompatibleProcesses incompatibleProcess = new ProcessPeriodicityIncompatibleProcesses();
        incompatibleProcess.setIncompatibleBillingId(incompatibleBillingId);
        incompatibleProcess.setStatus(EntityStatus.ACTIVE);

        return incompatibleProcess;
    }

    public ProcessPeriodicityResponse mapProcessPeriodicityGeneralOptions(ProcessPeriodicity processPeriodicity, List<Long> incompatibleProcesses) {
        ProcessPeriodicityResponse processPeriodicityResponse = new ProcessPeriodicityResponse();
        processPeriodicityResponse.setId(processPeriodicity.getId());
        processPeriodicityResponse.setName(processPeriodicity.getName());
        if (processPeriodicity.getIgnoreAtRuntime() != null) {
            processPeriodicityResponse.setIgnoreErrors(processPeriodicity.getIgnoreAtRuntime().contains(ProcessPeriodicityIgnoreAtRuntime.ERRORS));
            processPeriodicityResponse.setIgnoreWarnings(processPeriodicity.getIgnoreAtRuntime().contains(ProcessPeriodicityIgnoreAtRuntime.WARNINGS));
        }
        processPeriodicityResponse.setProcessPeriodicityType(processPeriodicity.getProcessPeriodicityType());
        processPeriodicityResponse.setStatus(processPeriodicity.getStatus());
        return processPeriodicityResponse;
    }

    public ProcessPeriodicityBillingProcessResponse mapToProcessPeriodicityBillingProcess(String billingName, Long billingId) {
        ProcessPeriodicityBillingProcessResponse processPeriodicityBillingProcessResponse = new ProcessPeriodicityBillingProcessResponse();
        processPeriodicityBillingProcessResponse.setName(billingName);
        processPeriodicityBillingProcessResponse.setId(billingId);
        return processPeriodicityBillingProcessResponse;
    }


    public void mapPeriodicalGeneralOptions(ProcessPeriodicity processPeriodicity,
                                            ProcessPeriodicityResponse processPeriodicityResponse,
                                            List<ProcessPeriodicityTimeIntervalDto> startTimeIntervals) {
        processPeriodicityResponse.setStartTimeIntervals(startTimeIntervals);
        if(processPeriodicity.getCalendarId() != null) {
            processPeriodicityResponse.setCalendar(getCalendar(processPeriodicity.getCalendarId()));
        }
        processPeriodicityResponse.setIsWeekendsExcluded(processPeriodicity.getProcessPeriodicityExcludes().contains(ProcessPeriodicityExclude.WEEKENDS));
        processPeriodicityResponse.setIsHolidaysExcluded(processPeriodicity.getProcessPeriodicityExcludes().contains(ProcessPeriodicityExclude.HOLIDAYS));
        processPeriodicityResponse.setChangeTo(processPeriodicity.getProcessPeriodicityChangeTo());
    }

    public void fillProcessPeriodicityBillingProcess(ProcessPeriodicityResponse processPeriodicityResponse, String billingName, Long billingId) {
        ProcessPeriodicityBillingProcessResponse processPeriodicityBillingProcessResponse = new ProcessPeriodicityBillingProcessResponse();
        processPeriodicityBillingProcessResponse.setId(billingId);
        processPeriodicityBillingProcessResponse.setName(billingName);
        processPeriodicityResponse.setStartAfterProcess(processPeriodicityBillingProcessResponse);
    }

    public List<ProcessPeriodicityTimeIntervalDto> mapToIntervalDtoList(List<ProcessPeriodicityTimeIntervals> processPeriodicityTimeIntervals) {
        List<ProcessPeriodicityTimeIntervalDto> processPeriodicityTimeIntervalDtoList = new ArrayList<>();
        for (ProcessPeriodicityTimeIntervals dbObj : processPeriodicityTimeIntervals) {
            processPeriodicityTimeIntervalDtoList.add(mapToProcessPeriodicityTimeIntervalDto(dbObj));
        }
        return processPeriodicityTimeIntervalDtoList;
    }

    private ProcessPeriodicityTimeIntervalDto mapToProcessPeriodicityTimeIntervalDto(ProcessPeriodicityTimeIntervals processPeriodicityTimeIntervals) {
        ProcessPeriodicityTimeIntervalDto processPeriodicityTimeIntervalDto = new ProcessPeriodicityTimeIntervalDto();
        processPeriodicityTimeIntervalDto.setId(processPeriodicityTimeIntervals.getId());
        processPeriodicityTimeIntervalDto.setStartTime(processPeriodicityTimeIntervals.getStartTime());
        processPeriodicityTimeIntervalDto.setEndTime(processPeriodicityTimeIntervals.getEndTime());
        return processPeriodicityTimeIntervalDto;
    }

    public void fillDayOfMonthOption(ProcessPeriodicityResponse processPeriodicityResponse, List<DateOfMonthDto> dateOfMonths) {
        ProcessPeriodicityPeriodOptionsDto processPeriodicityPeriodOptionsDto = new ProcessPeriodicityPeriodOptionsDto();
        processPeriodicityPeriodOptionsDto.setProcessPeriodicityPeriodType(ProcessPeriodicityPeriodType.DAY_OF_MONTH);
        processPeriodicityPeriodOptionsDto.setDateOfMonths(dateOfMonths);
        processPeriodicityResponse.setProcessPeriodicityPeriodOptionsDto(processPeriodicityPeriodOptionsDto);
    }

    public List<DateOfMonthDto> mapToDateOfMonthsList(List<ProcessPeriodicityDayOfMonths> processPeriodicityDayOfMonthsList) {
        List<DateOfMonthDto> dateOfMonthsDtoList = new ArrayList<>();
        for (ProcessPeriodicityDayOfMonths dbObj : processPeriodicityDayOfMonthsList) {
            dateOfMonthsDtoList.add(mapToDateOfMonthDto(dbObj));
        }
        return dateOfMonthsDtoList;
    }

    private DateOfMonthDto mapToDateOfMonthDto(ProcessPeriodicityDayOfMonths processPeriodicityDayOfMonths) {
        DateOfMonthDto dateOfMonthDto = new DateOfMonthDto();
        dateOfMonthDto.setId(processPeriodicityDayOfMonths.getId());
        dateOfMonthDto.setMonth(processPeriodicityDayOfMonths.getMonth());
        dateOfMonthDto.setMonthNumbers(processPeriodicityDayOfMonths.getMonthNumber());
        return dateOfMonthDto;
    }

    public void fillFormulaOption(String formula, ProcessPeriodicityResponse processPeriodicityResponse) {
        ProcessPeriodicityPeriodOptionsDto processPeriodicityPeriodOptionsDto = new ProcessPeriodicityPeriodOptionsDto();
        processPeriodicityPeriodOptionsDto.setProcessPeriodicityPeriodType(ProcessPeriodicityPeriodType.FORMULA);
        processPeriodicityPeriodOptionsDto.setRRUle(formula);
        processPeriodicityResponse.setProcessPeriodicityPeriodOptionsDto(processPeriodicityPeriodOptionsDto);
    }

    public void fillPeriodOfYearOption(ProcessPeriodicityResponse processPeriodicityResponse, DayOfWeekAndPeriodOfYearDto dayOfWeekAndPeriodOfYearDto) {
        ProcessPeriodicityPeriodOptionsDto processPeriodicityPeriodOptionsDto = new ProcessPeriodicityPeriodOptionsDto();
        processPeriodicityPeriodOptionsDto.setProcessPeriodicityPeriodType(ProcessPeriodicityPeriodType.PERIOD_OF_YEAR);
        processPeriodicityPeriodOptionsDto.setDayOfWeekAndPeriodOfYearDto(dayOfWeekAndPeriodOfYearDto);
        processPeriodicityResponse.setProcessPeriodicityPeriodOptionsDto(processPeriodicityPeriodOptionsDto);
    }

    public DayOfWeekAndPeriodOfYearDto mapToDayOfWeekAndPeriodOfYearDto(List<DayOfWeekDto> daysOfWeek, Boolean yearAround, List<PeriodOfYearDto> periodsOfYear) {
        DayOfWeekAndPeriodOfYearDto dayOfWeekAndPeriodOfYearDto = new DayOfWeekAndPeriodOfYearDto();
        dayOfWeekAndPeriodOfYearDto.setDaysOfWeek(daysOfWeek);
        dayOfWeekAndPeriodOfYearDto.setYearAround(yearAround);
        dayOfWeekAndPeriodOfYearDto.setPeriodsOfYear(periodsOfYear);
        return dayOfWeekAndPeriodOfYearDto;
    }

    public List<DayOfWeekDto> mapToDayOfWeekDtoList(List<ProcessPeriodicityPeriodOfYear> processPeriodicityPeriodOfYear) {
        List<DayOfWeekDto> dayOfWeekDtoList = new ArrayList<>();
        for (ProcessPeriodicityPeriodOfYear dbObj : processPeriodicityPeriodOfYear) {
            dayOfWeekDtoList.add(mapToDayOfWeekDto(dbObj));
        }
        return dayOfWeekDtoList;
    }

    public DayOfWeekDto mapToDayOfWeekDto(ProcessPeriodicityPeriodOfYear processPeriodicityPeriodOfYear) {
        DayOfWeekDto dayOfWeekDto = new DayOfWeekDto();
        dayOfWeekDto.setDays(processPeriodicityPeriodOfYear.getDays());
        dayOfWeekDto.setWeek(processPeriodicityPeriodOfYear.getWeek());
        dayOfWeekDto.setId(processPeriodicityPeriodOfYear.getId());
        return dayOfWeekDto;
    }

    public List<PeriodOfYearDto> mapToIssuingPeriodsDtoList(List<ProcessPeriodicityIssuingPeriods> processPeriodicityIssuingPeriods) {
        List<PeriodOfYearDto> issuingPeriodList = new ArrayList<>();
        for (ProcessPeriodicityIssuingPeriods dbObj : processPeriodicityIssuingPeriods) {
            issuingPeriodList.add(mapToIssuingPeriodsDto(dbObj));
        }
        return issuingPeriodList;
    }

    public PeriodOfYearDto mapToIssuingPeriodsDto(ProcessPeriodicityIssuingPeriods processPeriodicityIssuingPeriods) {
        PeriodOfYearDto periodOfYearDto = new PeriodOfYearDto();
        periodOfYearDto.setId(processPeriodicityIssuingPeriods.getId());
        periodOfYearDto.setStartDate(processPeriodicityIssuingPeriods.getPeriod_from());
        periodOfYearDto.setEndDate(processPeriodicityIssuingPeriods.getPeriod_to());
        return periodOfYearDto;
    }

    public void fillOneTimeDtoWithGeneralOptions(ProcessPeriodicityBillingProcessStart processPeriodicityBillingProcessStart, ProcessPeriodicityResponse processPeriodicityResponse) {
        OneTimeProcessPeriodicityDto oneTimeProcessPeriodicityDto = new OneTimeProcessPeriodicityDto();
        oneTimeProcessPeriodicityDto.setProcessPeriodicityBillingProcessStart(processPeriodicityBillingProcessStart);
        processPeriodicityResponse.setOneTimeProcessPeriodicityDto(oneTimeProcessPeriodicityDto);
    }

    public void fillUpdatedProcessPeriodicityGeneralOptions(ProcessPeriodicity processPeriodicity, String name,
                                                            List<ProcessPeriodicityIgnoreAtRuntime> ignoreAtRuntime) {
        processPeriodicity.setName(name);
        processPeriodicity.setIgnoreAtRuntime(ignoreAtRuntime);
    }

    public void fillUpdatedOneTimeProcessPeriodicity(ProcessPeriodicity processPeriodicity,
                                                     Long startAfterProcessBillingId,
                                                     ProcessPeriodicityBillingProcessStart processPeriodicityBillingProcessStart,
                                                     LocalDateTime billingProcessStartDate) {

        processPeriodicity.setStartAfterProcessBillingId(startAfterProcessBillingId);
        processPeriodicity.setProcessPeriodicityBillingProcessStart(processPeriodicityBillingProcessStart);
        processPeriodicity.setBillingProcessStartDate(billingProcessStartDate);
    }

    public void fillUpdatedPeriodicProcessPeriodicity(ProcessPeriodicity processPeriodicity,
                                                      Long startAfterProcessBillingId,
                                                      List<ProcessPeriodicityExclude> processPeriodicityExcludes,
                                                      Long calendarId,
                                                      ProcessPeriodicityChangeTo processPeriodicityChangeTo,
                                                      ProcessPeriodicityPeriodType processPeriodicityPeriodType) {

        processPeriodicity.setStartAfterProcessBillingId(startAfterProcessBillingId);
        processPeriodicity.setProcessPeriodicityExcludes(processPeriodicityExcludes);
        processPeriodicity.setCalendarId(calendarId);
        processPeriodicity.setProcessPeriodicityChangeTo(processPeriodicityChangeTo);
        processPeriodicity.setProcessPeriodicityPeriodType(processPeriodicityPeriodType);
    }

    public ProcessPeriodicityTimeIntervals mapToTimeIntervals(ProcessPeriodicityTimeIntervalDto dto, Long processPeriodicityId) {
        ProcessPeriodicityTimeIntervals processPeriodicityTimeIntervals = new ProcessPeriodicityTimeIntervals();
        processPeriodicityTimeIntervals.setStartTime(dto.getStartTime());
        processPeriodicityTimeIntervals.setEndTime(dto.getEndTime());
        processPeriodicityTimeIntervals.setProcessPeriodicityId(processPeriodicityId);
        processPeriodicityTimeIntervals.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityTimeIntervals;
    }

    public ProcessPeriodicityTimeIntervals fillUpdatedTimeIntervals(ProcessPeriodicityTimeIntervals processPeriodicityTimeIntervals, ProcessPeriodicityTimeIntervalDto dto) {
        processPeriodicityTimeIntervals.setStartTime(dto.getStartTime());
        processPeriodicityTimeIntervals.setEndTime(dto.getEndTime());
        processPeriodicityTimeIntervals.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityTimeIntervals;
    }

    public ProcessPeriodicityDayOfMonths mapToDayOfMonths(DateOfMonthDto dto, Long processPeriodicityId) {
        ProcessPeriodicityDayOfMonths processPeriodicityDayOfMonths = new ProcessPeriodicityDayOfMonths();
        processPeriodicityDayOfMonths.setMonth(dto.getMonth());
        processPeriodicityDayOfMonths.setMonthNumber(dto.getMonthNumbers());
        processPeriodicityDayOfMonths.setProcessPeriodicityId(processPeriodicityId);
        processPeriodicityDayOfMonths.setStatus(EntityStatus.ACTIVE);

        return processPeriodicityDayOfMonths;
    }

    public ProcessPeriodicityDayOfMonths fillUpdatedToDayOfMonths(ProcessPeriodicityDayOfMonths processPeriodicityDayOfMonths, DateOfMonthDto dto) {
        processPeriodicityDayOfMonths.setMonth(dto.getMonth());
        processPeriodicityDayOfMonths.setMonthNumber(dto.getMonthNumbers());
        processPeriodicityDayOfMonths.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityDayOfMonths;
    }

    public ProcessPeriodicityPeriodOfYear mapToPeriodOfYear(DayOfWeekDto dto, Long processPeriodicityId) {
        ProcessPeriodicityPeriodOfYear processPeriodicityPeriodOfYear = new ProcessPeriodicityPeriodOfYear();
        processPeriodicityPeriodOfYear.setDays(dto.getDays());
        processPeriodicityPeriodOfYear.setWeek(dto.getWeek());
        processPeriodicityPeriodOfYear.setProcessPeriodicityId(processPeriodicityId);
        processPeriodicityPeriodOfYear.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityPeriodOfYear;
    }

    public ProcessPeriodicityPeriodOfYear fillUpdatedPeriodOfYear(ProcessPeriodicityPeriodOfYear processPeriodicityPeriodOfYear, DayOfWeekDto dto) {
        processPeriodicityPeriodOfYear.setDays(dto.getDays());
        processPeriodicityPeriodOfYear.setWeek(dto.getWeek());
        processPeriodicityPeriodOfYear.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityPeriodOfYear;
    }

    public ProcessPeriodicityIssuingPeriods mapIssuingPeriods(PeriodOfYearDto dto, Long processPeriodicityId) {
        ProcessPeriodicityIssuingPeriods processPeriodicityIssuingPeriods = new ProcessPeriodicityIssuingPeriods();
        processPeriodicityIssuingPeriods.setPeriod_from(dto.getStartDate());
        processPeriodicityIssuingPeriods.setPeriod_to(dto.getEndDate());
        processPeriodicityIssuingPeriods.setProcessPeriodicityId(processPeriodicityId);
        processPeriodicityIssuingPeriods.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityIssuingPeriods;
    }

    public ProcessPeriodicityIssuingPeriods fillUpdatedPeriodOfYear(ProcessPeriodicityIssuingPeriods processPeriodicityIssuingPeriods, PeriodOfYearDto dto) {
        processPeriodicityIssuingPeriods.setPeriod_from(dto.getStartDate());
        processPeriodicityIssuingPeriods.setPeriod_to(dto.getEndDate());
        processPeriodicityIssuingPeriods.setStatus(EntityStatus.ACTIVE);
        return processPeriodicityIssuingPeriods;
    }

    private CalendarShortResponse getCalendar(Long calendarId) {
        return new CalendarShortResponse(
                calendarRepository
                        .findByIdAndStatusIsIn(calendarId, List.of(ACTIVE, INACTIVE,DELETED))
                        .orElseThrow(() -> new DomainEntityNotFoundException("Process Periodicity calendar not found or is DELETED;"))
        );
    }
}
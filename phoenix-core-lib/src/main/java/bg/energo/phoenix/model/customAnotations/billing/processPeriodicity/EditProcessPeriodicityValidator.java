package bg.energo.phoenix.model.customAnotations.billing.processPeriodicity;

import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityBillingProcessStart;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.DTO.*;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.EditProcessPeriodicityRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {EditProcessPeriodicityValidator.EditProcessPeriodicityValidatorImpl.class})
public @interface EditProcessPeriodicityValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class EditProcessPeriodicityValidatorImpl implements ConstraintValidator<EditProcessPeriodicityValidator, EditProcessPeriodicityRequest> {

        @Override
        public void initialize(EditProcessPeriodicityValidator constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(EditProcessPeriodicityRequest request, ConstraintValidatorContext constraintValidatorContext) {
            StringBuilder errors = new StringBuilder();
            if (request.getProcessPeriodicityType().equals(ProcessPeriodicityType.PERIODICAL)) {
                isCalendarIdValid(request, errors);
                isChangeToValid(request, errors);
                isTimeIntervalsValid(request.getStartTimeIntervals(), errors);
                if (request.getProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType() == null) {
                    errors.append("processPeriodicityPeriodOptionsDto.processPeriodicityPeriodType-processPeriodicityPeriodType cannot be null;");
                    constraintValidatorContext.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                    return false;
                }
                switch (request.getProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType()) {
                    case PERIOD_OF_YEAR -> {
                        validatePeriodOfYearOptions(request, errors);
                        validateRRuleOption(request);
                    }
                    case DAY_OF_MONTH -> {
                        validateDayOfMonth(request.getProcessPeriodicityPeriodOptionsDto().getDateOfMonths(), errors);
                        validateRRuleOption(request);
                    }
                    case FORMULA -> {
                        validateRRuleMandatoryOption(request.getProcessPeriodicityPeriodOptionsDto().getRRUle(), errors);
                    }
                    default -> {
                        errors.append("processPeriodicityPeriodOptionsDto.processPeriodicityPeriodType-Invalid processPeriodicityPeriodType;");
                    }
                }
            } else {
                isOneTimeStartAfterIsValid(request, errors);
                isOneTimeDateAndTimeValid(request, errors);
            }
            if (!errors.isEmpty()) {
                constraintValidatorContext.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                return false;
            }
            return true;
        }

        private void validateRRuleMandatoryOption(String rRule, StringBuilder errors) {
            if (StringUtils.isEmpty(rRule)) {
                errors.append("processPeriodicityPeriodOptionsDto.RRUle-RRUle is mandatory when FORMULA type is chosen;");
            }
        }

        private void validateRRuleOption(EditProcessPeriodicityRequest request) {
            if (!StringUtils.isEmpty(request.getProcessPeriodicityPeriodOptionsDto().getRRUle())) {
                request.getProcessPeriodicityPeriodOptionsDto().setRRUle(null);
            }
        }

        private void isCalendarIdValid(EditProcessPeriodicityRequest request, StringBuilder errors) {
            if ((BooleanUtils.isTrue(request.getIsWeekendsExcluded()) || BooleanUtils.isTrue(request.getIsHolidaysExcluded())) && request.getCalendarId() == null) {
                errors.append("calendarId-calendarId is mandatory when exclude option is selected;");
            }
        }

        private void isChangeToValid(EditProcessPeriodicityRequest request, StringBuilder errors) {
            if (request.getChangeTo() != null && !(request.getIsWeekendsExcluded() || request.getIsHolidaysExcluded())) {
                errors.append("changeTo-changeTo cannot selected when there is no exclude options;");
            }
        }

        private void isTimeIntervalsValid(List<ProcessPeriodicityTimeIntervalDto> intervalList, StringBuilder errors) {
            if (intervalList != null && !intervalList.isEmpty()) {
                for (int i = 0; i < intervalList.size(); i++) {
                    if (intervalList.get(i).getStartTime() == null) {
                        errors.append("startTimeIntervals[%s]-start time is mandatory for each interval;".formatted(i));
                    } else {
                        if (intervalList.get(i).getEndTime() == null) {
                            if (i != intervalList.size() - 1) {
                                errors.append("startTimeIntervals[%s]-end time is mandatory if next interval is added;".formatted(i));
                            }
                        } else if (!isIntervalValid(intervalList.get(i).getStartTime(), intervalList.get(i).getEndTime())) {
                            errors.append("startTimeIntervals[%s]-start time of the interval must be before end time;".formatted(i));
                        } else if (i < intervalList.size() - 1) {
                            if (isIntervalsOverlapping(intervalList.get(i).getEndTime(), intervalList.get(i + 1).getStartTime())) {
                                errors.append("startTimeIntervals-End time of the [%s] interval must be before the start time of [%s] interval;".formatted(i, i + 1));
                            }
                        }

                    }
                }
            }
        }


        private boolean isIntervalValid(LocalTime startTime, LocalTime endTime) {
            return startTime.isBefore(endTime);
        }

        private boolean isIntervalsOverlapping(LocalTime endTime1, LocalTime startTime2) {
            return endTime1.isAfter(startTime2);
        }

        private void validatePeriodOfYearOptions(EditProcessPeriodicityRequest request, StringBuilder errors) {
            isWeekAndDaysValid(request.getProcessPeriodicityPeriodOptionsDto().getDayOfWeekAndPeriodOfYearDto().getDaysOfWeek(), errors);
            if (validateStructureOfIssuingPeriodOfProcess(request.getProcessPeriodicityPeriodOptionsDto().getDayOfWeekAndPeriodOfYearDto(), errors)) {
                if (request.getProcessPeriodicityPeriodOptionsDto().getDayOfWeekAndPeriodOfYearDto().getYearAround() == null
                        || !request.getProcessPeriodicityPeriodOptionsDto().getDayOfWeekAndPeriodOfYearDto().getYearAround()) {
                    validateIssuingPeriodsFormatAndIntervals(request.getProcessPeriodicityPeriodOptionsDto().getDayOfWeekAndPeriodOfYearDto().getPeriodsOfYear(), errors);
                }
            }
        }

        private void isWeekAndDaysValid(List<DayOfWeekDto> dayOfWeekDtoList, StringBuilder errors) {
            if (dayOfWeekDtoList == null || dayOfWeekDtoList.isEmpty()) {
            } else {
                for (int i = 0; i < dayOfWeekDtoList.size(); i++) {
                    DayOfWeekDto dto = dayOfWeekDtoList.get(i);
                    if (dto.getWeek() != null && (dto.getDays() == null || dto.getDays().isEmpty())) {
                        errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.daysOfWeek[%s]-at least one day must be selected when week is chosen;".formatted(i));
                    }
                    if ((dto.getDays() != null && !dto.getDays().isEmpty()) && dto.getWeek() == null) {
                        errors.append("ProcessPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.daysOfWeek[%s]-week must be selected when days are chosen;".formatted(i));
                    }
                    if (dto.getDays() != null && dto.getDays().contains(Day.ALL_DAYS) && dto.getDays().size() > 1) {
                        errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.daysOfWeek[%s]-Only one day of the week can be selected when All days is chosen;".formatted(i));
                    }
                }
            }
        }

        private boolean validateStructureOfIssuingPeriodOfProcess(DayOfWeekAndPeriodOfYearDto dayOfWeekAndPeriodOfYearDto, StringBuilder errors) {
            if ((dayOfWeekAndPeriodOfYearDto.getYearAround() == null || dayOfWeekAndPeriodOfYearDto.getYearAround().equals(false))
                    && (dayOfWeekAndPeriodOfYearDto.getPeriodsOfYear() == null || dayOfWeekAndPeriodOfYearDto.getPeriodsOfYear().isEmpty())) {
                errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto-It is mandatory to select Year-around checkbox or to define at least one range;");
                return false;
            } else if ((dayOfWeekAndPeriodOfYearDto.getYearAround() != null && dayOfWeekAndPeriodOfYearDto.getYearAround().equals(true))
                    && (dayOfWeekAndPeriodOfYearDto.getPeriodsOfYear() != null && !dayOfWeekAndPeriodOfYearDto.getPeriodsOfYear().isEmpty())) {
                errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto-If Year-around checkbox is selected, period should be empty;");
                return false;
            }
            return true;
        }

        private void validateIssuingPeriodsFormatAndIntervals(List<PeriodOfYearDto> periodsOfYear, StringBuilder errors) {
            int falseCounter = 0;
            for (int i = 0; i < periodsOfYear.size(); i++) {
                if (!IsIssuingPeriodsDateFormatValid(periodsOfYear.get(i).getStartDate(), errors, i, "startDate")) {
                    falseCounter++;
                }
                if (!IsIssuingPeriodsDateFormatValid(periodsOfYear.get(i).getEndDate(), errors, i, "endDate")) {
                    falseCounter++;
                }
            }
            if (falseCounter == 0) {
                Collections.sort(periodsOfYear);
                for (int i = 0; i < periodsOfYear.size(); i++) {
                    if (!isIssuingPeriodsIntervalValid(periodsOfYear.get(i).getStartDate(), periodsOfYear.get(i).getEndDate())) {
                        errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.periodsOfYear[%s]-starDate must  must be less than EndDate;".formatted(i));
                    } else if (i < periodsOfYear.size() - 1) {
                        if (isIssuingPeriodsIOverlapping(periodsOfYear.get(i).getEndDate(), periodsOfYear.get(i + 1).getStartDate())) {
                            errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.periodsOfYear-EndDate of the [%s] interval must be before the startDate of [%s] interval;".formatted(i, i + 1));
                        }
                    }
                }
            }
        }

        private boolean isIssuingPeriodsIntervalValid(String startTime, String endTime) {
            return PeriodOfYearDto.dateToNumber(startTime) < PeriodOfYearDto.dateToNumber(endTime);
        }

        private boolean isIssuingPeriodsIOverlapping(String endTime1, String startTime2) {
            return PeriodOfYearDto.dateToNumber(endTime1) >= PeriodOfYearDto.dateToNumber(startTime2);
        }

        private boolean IsIssuingPeriodsDateFormatValid(String date, StringBuilder errors, int i, String fieldName) {
            if (date == null) {
                errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.periodsOfYear[%s]-[%s] must define;".formatted(i, fieldName));
                return false;
            }
            Pattern pattern = Pattern.compile("^[0-9]{2}\\.[0-9]{2}$");
            Matcher matcher = pattern.matcher(date);
            if (!matcher.matches()) {
                errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.periodsOfYear[%s]-[%s] dont match format;".formatted(i, fieldName));
                return false;
            } else {
                int month = Integer.parseInt(date.substring(0, 2));
                int day = Integer.parseInt(date.substring(3));
                if (month > 12 || day > 31) {
                    errors.append("processPeriodicityPeriodOptionsDto.dayOfWeekAndPeriodOfYearDto.periodsOfYear[%s]-[%s] invalid month or day value;".formatted(i, fieldName));
                    return false;
                }
            }
            return true;
        }

        private void validateDayOfMonth(List<DateOfMonthDto> dateOfMonthDto, StringBuilder errors) {
            if (CollectionUtils.isEmpty(dateOfMonthDto)) {
                errors.append("processPeriodicityPeriodOptionsDto.dateOfMonths.daysOfWeek-At least one day of the month must be selected when Day of the month is chosen;");
                return;
            }
            for (int i = 0; i < dateOfMonthDto.size(); i++) {
                DateOfMonthDto dto = dateOfMonthDto.get(i);
                Month month = dto.getMonth();
                if (month == null) {
                    errors.append("processPeriodicityPeriodOptionsDto.dateOfMonths[%s]-month cannot be null;".formatted(i));
                    return;
                }
                if ((CollectionUtils.isEmpty(dto.getMonthNumbers()))) {
                    errors.append("ProcessPeriodicityPeriodOptionsDto.dateOfMonths[%s].monthNumbers-at least one day must be selected when month is chosen;".formatted(i));
                    return;
                }
                if (dto.getMonthNumbers().contains(MonthNumber.ALL_DAYS) && dto.getMonthNumbers().size() > 1) {
                    errors.append("processPeriodicityPeriodOptionsDto.dateOfMonths[%s].monthNumbers-Only one day of the month can be selected when All days is chosen;".formatted(i));
                    return;
                }
                switch (month) {
                    case FEBRUARY -> {
                        if (dateOfMonthDto.get(i).getMonthNumbers().contains(MonthNumber.THIRTY) || dateOfMonthDto.get(i).getMonthNumbers().contains(MonthNumber.THIRTYONE)) {
                            errors.append("processPeriodicityPeriodOptionsDto.dateOfMonths[%s].monthNumbers-THIRTY and THIRTY ONE is not valid when FEBRUARY is chosen;".formatted(i));
                        }
                    }
                    case APRIL, JUNE, SEPTEMBER, NOVEMBER -> {
                        if (dateOfMonthDto.get(i).getMonthNumbers().contains(MonthNumber.THIRTYONE)) {
                            errors.append("processPeriodicityPeriodOptionsDto.dateOfMonths[%s].monthNumbers-THIRTY ONE is not valid when %s is chosen;".formatted(i, month));
                        }
                    }
                }
            }
        }


        private void isOneTimeStartAfterIsValid(EditProcessPeriodicityRequest request, StringBuilder errors) {
            if (request.getOneTimeProcessPeriodicityDto().getProcessPeriodicityBillingProcessStart().equals(ProcessPeriodicityBillingProcessStart.AFTER_PROCESS)
                    && request.getStartAfterProcessId() == null) {
                errors.append("startAfterProcessId-startAfterProcessId must be selected when one time after process is chosen;");
            }
        }

        private void isOneTimeDateAndTimeValid(EditProcessPeriodicityRequest request, StringBuilder errors) {
            if (request.getOneTimeProcessPeriodicityDto().getProcessPeriodicityBillingProcessStart().equals(ProcessPeriodicityBillingProcessStart.DATE_AND_TIME)) {
                if (request.getOneTimeProcessPeriodicityDto().getOneTimeStartDate() == null) {
                    errors.append("oneTimeProcessPeriodicityDto.startDate-startDate is mandatory when process type is DATE_AND_TIME;");
                }
                if (request.getOneTimeProcessPeriodicityDto().getOneTimeStartTime() == null) {
                    errors.append("oneTimeProcessPeriodicityDto.startTime-startTime is mandatory when process type is DATE_AND_TIME;");
                } else {
                    LocalDateTime processStartDateAndTime = LocalDateTime.of(request.getOneTimeProcessPeriodicityDto().getOneTimeStartDate(), request.getOneTimeProcessPeriodicityDto().getOneTimeStartTime()).truncatedTo(ChronoUnit.MINUTES);
                    if (ChronoUnit.MINUTES.between(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES), processStartDateAndTime) <= 30L) {
                        errors.append("oneTimeProcessPeriodicityDto.startTime-startTime must be in the future and has bigger interval then 30 minutes compared to the current time;");
                    }
                }
            }
        }

    }

}


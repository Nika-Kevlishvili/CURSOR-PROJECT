package bg.energo.phoenix.model.customAnotations.billing.processPeriodicity;

import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityBillingProcessStart;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityType;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.CreateProcessPeriodicityRequest;
import bg.energo.phoenix.model.request.billing.ProcessPeriodicity.baseDTO.*;
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
@Constraint(validatedBy = {ProcessPeriodicityValidator.ProcessPeriodicityValidatorImpl.class})
public @interface ProcessPeriodicityValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class ProcessPeriodicityValidatorImpl implements ConstraintValidator<ProcessPeriodicityValidator, CreateProcessPeriodicityRequest> {

        @Override
        public void initialize(ProcessPeriodicityValidator constraintAnnotation) {
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(CreateProcessPeriodicityRequest request, ConstraintValidatorContext constraintValidatorContext) {
            StringBuilder errors = new StringBuilder();
            if (request.getProcessPeriodicityType().equals(ProcessPeriodicityType.PERIODICAL)) {
                isCalendarIdValid(request, errors);
                isChangeToValid(request, errors);
                isTimeIntervalsValid(request.getStartTimeIntervals(), errors);
                if (request.getBaseProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType() == null) {
                    errors.append("baseProcessPeriodicityPeriodOptionsDto.processPeriodicityPeriodType-processPeriodicityPeriodType cannot be null;");
                    constraintValidatorContext.buildConstraintViolationWithTemplate(errors.toString()).addConstraintViolation();
                    return false;
                }
                switch (request.getBaseProcessPeriodicityPeriodOptionsDto().getProcessPeriodicityPeriodType()) {
                    case PERIOD_OF_YEAR -> {
                        validatePeriodOfYearOptions(request, errors);
                        validateRRuleOption(request);
                    }
                    case DAY_OF_MONTH -> {
                        validateDayOfMonth(request.getBaseProcessPeriodicityPeriodOptionsDto().getDateOfMonths(), errors);
                        validateRRuleOption(request);
                    }
                    case FORMULA -> {
                        validateRRuleMandatoryOption(request.getBaseProcessPeriodicityPeriodOptionsDto().getRRUle(), errors);
                    }
                    default -> {
                        errors.append("baseProcessPeriodicityPeriodOptionsDto.processPeriodicityPeriodType-Invalid processPeriodicityPeriodType;");
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
                errors.append("baseProcessPeriodicityPeriodOptionsDto.RRUle-RRUle is mandatory when FORMULA type is chosen;");
            }
        }

        private void validateRRuleOption(CreateProcessPeriodicityRequest request) {
            if (!StringUtils.isEmpty(request.getBaseProcessPeriodicityPeriodOptionsDto().getRRUle())) {
                request.getBaseProcessPeriodicityPeriodOptionsDto().setRRUle(null);
            }
        }

        /**
         * Checks if the calendarId is valid in the given CreateProcessPeriodicityRequest object.
         *
         * @param request The CreateProcessPeriodicityRequest object
         * @param errors  A StringBuilder to store any validation errors
         */
        private void isCalendarIdValid(CreateProcessPeriodicityRequest request, StringBuilder errors) {
            if ((BooleanUtils.isTrue(request.getIsWeekendsExcluded()) || BooleanUtils.isTrue(request.getIsHolidaysExcluded())) && request.getCalendarId() == null) {
                errors.append("calendarId-calendarId is mandatory when exclude option is selected;");
            }
        }

        /**
         * Checks if the changeTo field is valid in the given CreateProcessPeriodicityRequest object.
         *
         * @param request The CreateProcessPeriodicityRequest object
         * @param errors  A StringBuilder to store any validation errors
         */
        private void isChangeToValid(CreateProcessPeriodicityRequest request, StringBuilder errors) {
            if (request.getChangeTo() != null && !(request.getIsWeekendsExcluded() || request.getIsHolidaysExcluded())) {
                errors.append("changeTo-changeTo cannot selected when there is no exclude options;");
            }
        }

        /**
         * Checks if the time intervals in the given list are valid.
         *
         * @param intervalList The list of time intervals to validate
         * @param errors       A StringBuilder to store any validation errors
         */
        private void isTimeIntervalsValid(List<BaseProcessPeriodicityTimeIntervalDto> intervalList, StringBuilder errors) {
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

        private void validatePeriodOfYearOptions(CreateProcessPeriodicityRequest request, StringBuilder errors) {
            isWeekAndDaysValid(request.getBaseProcessPeriodicityPeriodOptionsDto().getBaseDayOfWeekAndPeriodOfYearDto().getDaysOfWeek(), errors);
            if (validateStructureOfIssuingPeriodOfProcess(request.getBaseProcessPeriodicityPeriodOptionsDto().getBaseDayOfWeekAndPeriodOfYearDto(), errors)) {
                if (request.getBaseProcessPeriodicityPeriodOptionsDto().getBaseDayOfWeekAndPeriodOfYearDto().getYearAround() == null
                        || !request.getBaseProcessPeriodicityPeriodOptionsDto().getBaseDayOfWeekAndPeriodOfYearDto().getYearAround()) {
                    validateIssuingPeriodsFormatAndIntervals(request.getBaseProcessPeriodicityPeriodOptionsDto().getBaseDayOfWeekAndPeriodOfYearDto().getPeriodsOfYear(), errors);
                }
            }
        }

        /**
         * Checks if the selected week and days are valid in the given list of BaseDayOfWeekDto objects.
         *
         * @param dayOfWeekDtoList The list of BaseDayOfWeekDto objects
         * @param errors           A StringBuilder to store any validation errors
         */
        private void isWeekAndDaysValid(List<BaseDayOfWeekDto> dayOfWeekDtoList, StringBuilder errors) {
            if (dayOfWeekDtoList == null || dayOfWeekDtoList.isEmpty()) {
                errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.daysOfWeek-At least one day of the week must be selected when Period of the year is chosen;");
            } else {
                for (int i = 0; i < dayOfWeekDtoList.size(); i++) {
                    BaseDayOfWeekDto dto = dayOfWeekDtoList.get(i);
                    if (dto.getWeek() != null && (dto.getDays() == null || dto.getDays().isEmpty())) {
                        errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.daysOfWeek[%s]-at least one day must be selected when week is chosen;".formatted(i));
                    }
                    if ((dto.getDays() != null && !dto.getDays().isEmpty()) && dto.getWeek() == null) {
                        errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.daysOfWeek[%s]-week must be selected when days are chosen;".formatted(i));
                    }
                    if (dto.getDays() != null && dto.getDays().contains(Day.ALL_DAYS) && dto.getDays().size() > 1) {
                        errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.daysOfWeek[%s]-Only one day of the week can be selected when All days is chosen;".formatted(i));
                    }
                }
            }
        }

        /**
         * Validates the structure of the issuing period of a process.
         *
         * @param baseDayOfWeekAndPeriodOfYearDto The BaseDayOfWeekAndPeriodOfYearDto object representing the issuing period
         * @param errors                          A StringBuilder to store any validation errors
         * @return true if the structure is valid, false otherwise
         */
        private boolean validateStructureOfIssuingPeriodOfProcess(BaseDayOfWeekAndPeriodOfYearDto baseDayOfWeekAndPeriodOfYearDto, StringBuilder errors) {
            if ((baseDayOfWeekAndPeriodOfYearDto.getYearAround() == null || baseDayOfWeekAndPeriodOfYearDto.getYearAround().equals(false))
                    && (baseDayOfWeekAndPeriodOfYearDto.getPeriodsOfYear() == null || baseDayOfWeekAndPeriodOfYearDto.getPeriodsOfYear().isEmpty())) {
                errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto-It is mandatory to select Year-around checkbox or to define at least one range;");
                return false;
            } else if ((baseDayOfWeekAndPeriodOfYearDto.getYearAround() != null && baseDayOfWeekAndPeriodOfYearDto.getYearAround().equals(true))
                    && (baseDayOfWeekAndPeriodOfYearDto.getPeriodsOfYear() != null && !baseDayOfWeekAndPeriodOfYearDto.getPeriodsOfYear().isEmpty())) {
                errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto-If Year-around checkbox is selected, period should be empty;");
                return false;
            }
            return true;
        }

        /**
         * Validates the format and intervals of the issuing periods in a list of BasePeriodOfYearDto objects.
         *
         * @param periodsOfYear The list of BasePeriodOfYearDto objects representing the issuing periods
         * @param errors        A StringBuilder to store any validation errors
         */
        private void validateIssuingPeriodsFormatAndIntervals(List<BasePeriodOfYearDto> periodsOfYear, StringBuilder errors) {
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
                        errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.periodsOfYear[%s]-starDate must  must be less than EndDate;".formatted(i));
                    } else if (i < periodsOfYear.size() - 1) {
                        if (isIssuingPeriodsIOverlapping(periodsOfYear.get(i).getEndDate(), periodsOfYear.get(i + 1).getStartDate())) {
                            errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.periodsOfYear-EndDate of the [%s] interval must be before the startDate of [%s] interval;".formatted(i, i + 1));
                        }
                    }
                }
            }
        }

        private boolean isIssuingPeriodsIntervalValid(String startTime, String endTime) {
            return BasePeriodOfYearDto.dateToNumber(startTime) < BasePeriodOfYearDto.dateToNumber(endTime);
        }

        private boolean isIssuingPeriodsIOverlapping(String endTime1, String startTime2) {
            return BasePeriodOfYearDto.dateToNumber(endTime1) >= BasePeriodOfYearDto.dateToNumber(startTime2);
        }

        /**
         * Checks if the date in the format "MM.dd" is valid and matches format.
         *
         * @param date      The date to validate
         * @param errors    A StringBuilder to store any validation errors
         * @param i         The index of the baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.periodsOfYear
         * @param fieldName The name of the field being validated (startDate or endDate)
         * @return true if the date is valid, false otherwise
         */
        private boolean IsIssuingPeriodsDateFormatValid(String date, StringBuilder errors, int i, String fieldName) {
            if (date == null) {
                errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.periodsOfYear[%s]-[%s] must define;".formatted(i, fieldName));
                return false;
            }
            Pattern pattern = Pattern.compile("^[0-9]{2}\\.[0-9]{2}$");
            Matcher matcher = pattern.matcher(date);
            if (!matcher.matches()) {
                errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.periodsOfYear[%s]-[%s] dont match format;".formatted(i, fieldName));
                return false;
            } else {
                int month = Integer.parseInt(date.substring(0, 2));
                int day = Integer.parseInt(date.substring(3));
                if (month > 12 || day > 31) {
                    errors.append("baseProcessPeriodicityPeriodOptionsDto.baseDayOfWeekAndPeriodOfYearDto.periodsOfYear[%s]-[%s] invalid month or day value;".formatted(i, fieldName));
                    return false;
                }
            }
            return true;
        }

        /**
         * Validates the list of BaseDateOfMonthDto objects.
         *
         * @param dateOfMonthDto The list of BaseDateOfMonthDto objects to validate
         * @param errors         A StringBuilder to store any validation errors
         */
        private void validateDayOfMonth(List<BaseDateOfMonthDto> dateOfMonthDto, StringBuilder errors) {
            if (CollectionUtils.isEmpty(dateOfMonthDto)) {
                errors.append("baseProcessPeriodicityPeriodOptionsDto.dateOfMonths.daysOfWeek-At least one day of the month must be selected when Day of the month is chosen;");
                return;
            }
            for (int i = 0; i < dateOfMonthDto.size(); i++) {
                BaseDateOfMonthDto dto = dateOfMonthDto.get(i);
                Month month = dto.getMonth();
                if (month == null) {
                    errors.append("baseProcessPeriodicityPeriodOptionsDto.dateOfMonths[%s]-month cannot be null;".formatted(i));
                    return;
                }
                if ((CollectionUtils.isEmpty(dto.getMonthNumbers()))) {
                    errors.append("baseProcessPeriodicityPeriodOptionsDto.dateOfMonths[%s].monthNumbers-at least one day must be selected when month is chosen;".formatted(i));
                    return;
                }
                if (dto.getMonthNumbers().contains(MonthNumber.ALL_DAYS) && dto.getMonthNumbers().size() > 1) {
                    errors.append("baseProcessPeriodicityPeriodOptionsDto.dateOfMonths[%s].monthNumbers-Only one day of the month can be selected when All days is chosen;".formatted(i));
                    return;
                }
                switch (month) {
                    case FEBRUARY -> {
                        if (dateOfMonthDto.get(i).getMonthNumbers().contains(MonthNumber.THIRTY) || dateOfMonthDto.get(i).getMonthNumbers().contains(MonthNumber.THIRTYONE)) {
                            errors.append("baseProcessPeriodicityPeriodOptionsDto.dateOfMonths[%s].monthNumbers-THIRTY and THIRTY ONE is not valid when FEBRUARY is chosen;".formatted(i));
                        }
                    }
                    case APRIL, JUNE, SEPTEMBER, NOVEMBER -> {
                        if (dateOfMonthDto.get(i).getMonthNumbers().contains(MonthNumber.THIRTYONE)) {
                            errors.append("baseProcessPeriodicityPeriodOptionsDto.dateOfMonths[%s].monthNumbers-THIRTY ONE is not valid when %s is chosen;".formatted(i, month));
                        }
                    }
                }
            }
        }


        /**
         * Checks if the startAfterProcessId is valid when the process periodicity billing process start is set to AFTER_PROCESS.
         *
         * @param request The CreateProcessPeriodicityRequest object
         * @param errors  A StringBuilder to store any validation errors
         */
        private void isOneTimeStartAfterIsValid(CreateProcessPeriodicityRequest request, StringBuilder errors) {
            if (request.getOneTimeProcessPeriodicityDto().getProcessPeriodicityBillingProcessStart().equals(ProcessPeriodicityBillingProcessStart.AFTER_PROCESS)
                    && request.getStartAfterProcessId() == null) {
                errors.append("startAfterProcessId-startAfterProcessId must be selected when one time after process is chosen;");
            }
        }

        private void isOneTimeDateAndTimeValid(CreateProcessPeriodicityRequest request, StringBuilder errors) {
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


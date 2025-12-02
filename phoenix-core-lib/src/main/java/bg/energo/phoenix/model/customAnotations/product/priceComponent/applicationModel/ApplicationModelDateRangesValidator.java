package bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel;

import bg.energo.phoenix.model.request.product.iap.interimAdvancePayment.periodical.PeriodOfYearBaseRequest;
import bg.energo.phoenix.model.request.product.price.aplicationModel.APPeriodOfYearRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ApplicationModelDateRangesValidator.ApplicationModelDateRangesValidatorImpl.class})
public @interface ApplicationModelDateRangesValidator {


    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};


    class ApplicationModelDateRangesValidatorImpl implements ConstraintValidator<ApplicationModelDateRangesValidator, List<APPeriodOfYearRequest>> {

        /**
         * Validate That format of start and end dates are valid and there is no overlap between provided periods. Sort provided periods by their "startDates"
         * and then check in sorted collection - It is violation if current periods end date is equals or greater than next periods start date
         *
         * @param periodOfYears object to validate
         * @param context       context in which the constraint is evaluated
         * @return true if constraints are satisfied, else false
         */
        @Override
        public boolean isValid(List<APPeriodOfYearRequest> periodOfYears, ConstraintValidatorContext context) {
            boolean result = true;
            int index = 0;
            if (periodOfYears != null) {
                result = rangesFormatIsValid(periodOfYears, context);
                if (result) {
                    Collections.sort(periodOfYears);
                    if (PeriodOfYearBaseRequest.dateToNumber(periodOfYears.get(0).getStartDate()) >= PeriodOfYearBaseRequest.dateToNumber(periodOfYears.get(0).getEndDate())) {
                        context.buildConstraintViolationWithTemplate("applicationModelRequest.volumesByScaleRequest.periodsOfYear[0]-Start Date must be less than End Date;")
                                .addConstraintViolation();
                        result = false;
                    }
                    for (int i = 0; i < periodOfYears.size() - 1; i++) {
                        if (PeriodOfYearBaseRequest.dateToNumber(periodOfYears.get(i + 1).getStartDate()) >= PeriodOfYearBaseRequest.dateToNumber(periodOfYears.get(i + 1).getEndDate())) {
                            context.buildConstraintViolationWithTemplate(String.format("applicationModelRequest.volumesByScaleRequest.periodsOfYear[%s]-Start Date must be less than End Date;", i))
                                    .addConstraintViolation();
                            result = false;
                        }

                        if (PeriodOfYearBaseRequest.dateToNumber(periodOfYears.get(i).getEndDate()) >= PeriodOfYearBaseRequest.dateToNumber(periodOfYears.get(i + 1).getStartDate())) {
                            context.buildConstraintViolationWithTemplate(String.format("applicationModelRequest.volumesByScaleRequest.periodsOfYear[%s]-Date range overlaps previous range;", i))
                                    .addConstraintViolation();
                            result = false;
                        }
                    }
                }

            }
            return result;
        }

        /**
         * Validate Date ranges - It is violation if current periods end date is equals or greater than next periods start date
         *
         * @param periodOfYears object to validate
         * @param context       context in which the constraint is evaluated
         * @return true if ranges format is valid, else false
         */
        private boolean rangesFormatIsValid(List<APPeriodOfYearRequest> periodOfYears, ConstraintValidatorContext context) {
            boolean result = true;
            int index = 0;
            for (APPeriodOfYearRequest periodOfYear : periodOfYears) {
                if (periodOfYear.getStartDate() != null && dateFormatIsInvalid(periodOfYear.getStartDate())) {
                    context.buildConstraintViolationWithTemplate(String.format("applicationModelRequest.volumesByScaleRequest.periodsOfYear[%s].startDate-Date range Start Date invalid format or symbols;", index))
                            .addConstraintViolation();
                    result = false;
                } else if (periodOfYear.getStartDate() == null) {
                    result = false;
                }
                if (periodOfYear.getEndDate() != null && dateFormatIsInvalid(periodOfYear.getEndDate())) {
                    context.buildConstraintViolationWithTemplate(String.format("applicationModelRequest.volumesByScaleRequest.periodsOfYear[%s].endDate-Date range End Date invalid format or symbols;", index))
                            .addConstraintViolation();
                    result = false;
                } else if (periodOfYear.getEndDate() == null) {
                    result = false;
                }
                index++;
            }
            return result;
        }

        /**
         * Validate format and value of received date field
         *
         * @param date field which is validated
         * @return true if format and value is valid, else false
         */
        private boolean dateFormatIsInvalid(String date) {
            Pattern pattern = Pattern.compile("^[0-9]{2}\\.[0-9]{2}$");
            Matcher matcher = pattern.matcher(date);
            if (!matcher.matches()) {
                return true;
            } else {
                int month = Integer.parseInt(date.substring(0, 2));
                int day = Integer.parseInt(date.substring(3));
                return month > 12 || day > 31;
            }
        }

    }
}

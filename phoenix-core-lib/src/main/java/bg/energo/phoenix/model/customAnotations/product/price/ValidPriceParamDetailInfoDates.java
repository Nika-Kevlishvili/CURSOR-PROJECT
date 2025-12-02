package bg.energo.phoenix.model.customAnotations.product.price;

import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.request.product.price.priceParameter.CreatePriceParameterDetailRequest;
import bg.energo.phoenix.model.request.product.price.priceParameter.CreatePriceParameterRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;
import java.util.List;

import static java.lang.annotation.ElementType.TYPE;

@Target( { TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidPriceParamDetailInfoDates.PriceParamDetailInfoDatesValidator.class})
public @interface ValidPriceParamDetailInfoDates {

    String value() default "";

    String message() default "priceParameterDetails-Price parameter detail infos should be created within ONE_DAY/ONE_MONTH range;";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PriceParamDetailInfoDatesValidator implements ConstraintValidator<ValidPriceParamDetailInfoDates, CreatePriceParameterRequest> {

        @Override
        public boolean isValid(CreatePriceParameterRequest value, ConstraintValidatorContext context) {
            return isPeriodFromWithinAllowedRange(value.getPriceParameterDetails(), value.getPeriodType());
        }

        /**
         * Determines whether the periodFrom values in a list of {@link CreatePriceParameterDetailRequest} objects
         * are within the allowed range based on the provided period type.
         *
         * @param requests a list of {@link CreatePriceParameterDetailRequest} objects to be checked
         * @param periodType the type of period for which the price parameter is being created
         * @return true if the periodFrom values in the list are within the allowed range, false otherwise
         */
        private boolean isPeriodFromWithinAllowedRange(List<CreatePriceParameterDetailRequest> requests, PeriodType periodType) {
            // Price parameter can be created without pricing values
            if (CollectionUtils.isEmpty(requests)) {
                return true;
            }

            // If period type is ONE_MONTH or ONE_DAY, only one value is allowed
            if ((periodType.equals(PeriodType.ONE_MONTH) || periodType.equals(PeriodType.ONE_DAY))
                    && requests.size() > 1) {
                return false;
            }

            // Sort the list based on periodFrom field in ascending order
            requests.sort(Comparator.comparing(CreatePriceParameterDetailRequest::getPeriodFrom));

            // Retrieve the periodFrom day value of the first and last elements in the list
            int firstDay = requests.get(0).getPeriodFrom().getDayOfMonth();
            int firstMonth = requests.get(0).getPeriodFrom().getMonthValue();
            int firstYear = requests.get(0).getPeriodFrom().getYear();

            int lastDay = requests.get(requests.size() - 1).getPeriodFrom().getDayOfMonth();
            int lastMonth = requests.get(requests.size() - 1).getPeriodFrom().getMonthValue();
            int lastYear = requests.get(requests.size() - 1).getPeriodFrom().getYear();

            // Compare the dates of the first and last periodFrom values to check if they belong to the same day/month
            return firstDay == lastDay
                    && firstMonth == lastMonth
                    && firstYear == lastYear;
        }
    }
}

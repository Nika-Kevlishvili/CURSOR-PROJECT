package bg.energo.phoenix.model.customAnotations.pod.billingByScales;

import bg.energo.phoenix.model.request.pod.billingByScales.BillingByScalesCreateRequest;
import bg.energo.phoenix.model.request.pod.billingByScales.BillingByScalesTableCreateRequest;
import bg.energo.phoenix.util.epb.EPBObjectUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.Range;

import java.lang.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidPeriods.PeriodValidator.class})
public @interface ValidPeriods {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PeriodValidator implements ConstraintValidator<ValidPeriods, BillingByScalesCreateRequest> {
        private static final LocalDate MIN_DATE = LocalDate.of(1990, Month.JANUARY, 1);
        private static final LocalDate MAX_DATE = LocalDate.of(2090, Month.DECEMBER, 31);

        @Override
        public boolean isValid(BillingByScalesCreateRequest request, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();

            boolean isMandatoryFieldsValid = true;
            boolean isPeriodsValid = true;

            List<BillingByScalesTableCreateRequest> scalesTable = request.getBillingByScalesTableCreateRequests();

            for (int i = 0; i < scalesTable.size(); i++) {
                BillingByScalesTableCreateRequest unit = scalesTable.get(i);
                LocalDate periodFrom = unit.getPeriodFrom();
                LocalDate periodTo = unit.getPeriodTo();

                if (!EPBObjectUtils.isAnyFieldNotNull(unit)) {
                    continue; // Nothing to validate
                }

                boolean isValidPeriod = validatePeriodDates(periodFrom, periodTo, context, i);
                if (isMandatoryFieldsValid && !isValidPeriod) {
                    isMandatoryFieldsValid = false;
                }

                boolean isValidMandatoryFields = validateMandatoryFields(request, unit, context, i);
                if (isPeriodsValid && !isValidMandatoryFields) {
                    isPeriodsValid = false;
                }
            }

            return isMandatoryFieldsValid && isPeriodsValid;
        }

        private boolean validatePeriodDates(LocalDate periodFrom, LocalDate periodTo, ConstraintValidatorContext context, int index) {
            if (periodFrom == null) {
                addConstraintViolation(context, "periodFrom-[PeriodFrom] is mandatory;", index);
                return false;
            }

            if (periodFrom.isBefore(MIN_DATE) || periodFrom.isAfter(MAX_DATE)) {
                addConstraintViolation(context, "periodFrom-[PeriodFrom] must be between %s and %s;"
                        .formatted(MIN_DATE.toString(), MAX_DATE.toString()), index);
                return false;
            }

            if (periodTo == null) {
                addConstraintViolation(context, "periodTo-[periodTo] is mandatory;", index);
                return false;
            }

            if (periodTo.isBefore(MIN_DATE) || periodTo.isAfter(MAX_DATE)) {
                addConstraintViolation(context, "periodTo-[periodTo] must be between %s and %s;"
                        .formatted(MIN_DATE.toString(), MAX_DATE.toString()), index);
                return false;
            }

            if (periodTo.isBefore(periodFrom)) {
                addConstraintViolation(context, "periodTo-[periodTo] should be before (or equal) period from;", index);
                return false;
            }

            if (ChronoUnit.YEARS.between(periodFrom, periodTo) > 1) {
                addConstraintViolation(context, "periodFrom-[PeriodFrom] Period should be limited to one-year time interval;", index);
                addConstraintViolation(context, "periodTo-[PeriodTo] Period should be limited to one-year time interval;", index);
                return false;
            }

            return true;
        }

        private boolean validateMandatoryFields(BillingByScalesCreateRequest request, BillingByScalesTableCreateRequest unit, ConstraintValidatorContext context, int index) {
            String meterNumber = unit.getMeterNumber();
            String scaleType = unit.getScaleType();
            String scaleCode = unit.getScaleCode();
            String tariffScale = unit.getTariffScale();

            // Validate when both period fields are present
            if (unit.getPeriodFrom() != null && unit.getPeriodTo() != null) {
                if (isEmpty(meterNumber)&&!isEmpty(scaleCode)) {
                    addConstraintViolation(context, "meterNumber-[MeterNumber] is required when periodFrom and PeriodTo are filled in;", index);
                    return false;
                }
                if (isEmpty(scaleType)) {
                    addConstraintViolation(context, "scaleType-[ScaleType] is required when periodFrom and PeriodTo are filled in;", index);
                    return false;
                }
                if (isEmpty(tariffScale) && scaleCode == null) {
                    addConstraintViolation(context, "tariffScale-[TariffScale] is required when periodFrom and PeriodTo are filled in;", index);
                    return false;
                }
                if (!isEmpty(tariffScale) && scaleCode != null) {
                    addConstraintViolation(context, "tariffScale-[TariffScale] can't be filled in while scaleCode is active;", index);
                    return false;
                }
            }

            // Validate specific rules when "tariffScale" is empty
            if (isEmpty(tariffScale)) {
                if (scaleCode == null) {
                    addConstraintViolation(context, "scaleCode-[ScaleCode] is required when tariffScale is empty;", index);
                    return false;
                }
                if (isEmpty(meterNumber)) {
                    addConstraintViolation(context, "meterNumber-[MeterNumber] is required when scaleCode is filled In;", index);
                    return false;
                }
                if (isEmpty(scaleType)) {
                    addConstraintViolation(context, "scaleType-[ScaleType] is required when scaleCode is filled In;", index);
                    return false;
                }
            }

            if (!validateScaleCodeFields(request, unit, context, index)) {
                return false;
            }

            // Validate specific rules when "scaleCode" is null
            if (scaleCode == null && isEmpty(tariffScale)) {
                addConstraintViolation(context, "tariffScale-[TariffScale] is required when scaleCode not is filled In;", index);
                return false;
            }

            return validateTariffScaleFields(unit, context, index);
        }

        private boolean validateScaleCodeFields(BillingByScalesCreateRequest request, BillingByScalesTableCreateRequest unit, ConstraintValidatorContext context, int index) {
            boolean isCorrectionChecked = Boolean.TRUE.equals(request.getCorrection());
            boolean isOnlyCorrectionChecked = isCorrectionChecked && !Boolean.TRUE.equals(request.getOverride());

            BigDecimal minValue = isOnlyCorrectionChecked ? BigDecimal.valueOf(-9999999.99999) : BigDecimal.ZERO;
            Range<BigDecimal> allowedValueRange = Range.of(minValue, BigDecimal.valueOf(9999999.99999));

            BigDecimal difference = unit.getDifference();
            if (Objects.nonNull(difference)) {
                if (!allowedValueRange.contains(difference)) {
                    addConstraintViolation(context, "difference-[Difference] must be within the range %s to 9999999.99999;".formatted(minValue.toString()), index);
                    return false;
                }
            }

            BigDecimal correction = unit.getCorrection();
            if (Objects.nonNull(correction)) {
                if (!allowedValueRange.contains(correction)) {
                    addConstraintViolation(context, "correction-[Correction] must be within the range %s to 9999999.99999;".formatted(minValue.toString()), index);
                    return false;
                }
            }

            BigDecimal deduction = unit.getDeducted();
            if (Objects.nonNull(deduction)) {
                if (!allowedValueRange.contains(deduction)) {
                    addConstraintViolation(context, "deduction-[Deduction] must be within the range %s to 9999999.99999;".formatted(minValue.toString()), index);
                    return false;
                }
            }

            BigDecimal totalValue = unit.getTotalValue();
            if (Objects.nonNull(totalValue)) {
                if (!allowedValueRange.contains(totalValue)) {
                    addConstraintViolation(context, "totalValue-[TotalValue] must be within the range %s to 9999999.99999;".formatted(minValue.toString()), index);
                    return false;
                }
            }

            BigDecimal totalVolumes = unit.getTotalVolumes();
            if (Objects.nonNull(totalVolumes)) {
                if (!allowedValueRange.contains(totalVolumes)) {
                    addConstraintViolation(context, "totalVolumes-[TotalVolumes] must be within the range %s to 9999999.99999;".formatted(minValue.toString()), index);
                    return false;
                }
            }

            BigDecimal volumes = unit.getVolumes();
            if (Objects.nonNull(volumes)) {
                if (!allowedValueRange.contains(volumes)) {
                    addConstraintViolation(context, "volumes-[Volumes] must be within the range %s to 9999999.99999;".formatted(minValue.toString()), index);
                    return false;
                }
            }

            if (!isCorrectionChecked) {
                if (!isEmpty(unit.getScaleCode())) {
                    if (unit.getNewMeterReading() == null) {
                        addConstraintViolation(context, "newMeterReading-[NewMeterReading] is required when scaleCode is filled In;", index);
                        return false;
                    }
                    if (unit.getOldMeterReading() == null) {
                        addConstraintViolation(context, "oldMeterReading-[OldMeterReading] is required when scaleCode is filled In;", index);
                        return false;
                    }
                    if (difference == null) {
                        addConstraintViolation(context, "difference-[Difference] is required when scaleCode is filled In;", index);
                        return false;
                    }
                }
            }

            if (!isEmpty(unit.getScaleCode())) {
                if (unit.getMultiplier() == null) {
                    addConstraintViolation(context, "multiplier-[Multiplier] is required when scaleCode is filled In;", index);
                    return false;
                }

                if (totalVolumes == null) {
                    addConstraintViolation(context, "volumes-[Volumes] is required when scaleCode is filled In;", index);
                    return false;
                }
            }

            return true;
        }

        private boolean validateTariffScaleFields(BillingByScalesTableCreateRequest unit, ConstraintValidatorContext context, int index) {
            String tariffScale = unit.getTariffScale();

            if (!isEmpty(tariffScale)) {
                if (unit.getVolumes() == null) {
                    addConstraintViolation(context, "volumes-[Volumes] is mandatory when tariffScale is filled in;", index);
                    return false;
                }
                if (unit.getUnitPrice() == null) {
                    addConstraintViolation(context, "unitPrice-[UnitPrice] is mandatory when tariffScale is filled in;", index);
                    return false;
                }
                if (unit.getTotalValue() == null) {
                    addConstraintViolation(context, "totalValue-[TotalValue] is mandatory when tariffScale is filled in;", index);
                    return false;
                }
            }
            return true;
        }

        private boolean isEmpty(String value) {
            return value == null || value.trim().isEmpty();
        }

        private void addConstraintViolation(ConstraintValidatorContext context, String message, int index) {
            context.buildConstraintViolationWithTemplate("billingByScalesTableCreateRequests[%s].%s".formatted(index, message)).addConstraintViolation();
        }
    }
}
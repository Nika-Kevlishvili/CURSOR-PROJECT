package bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel;

import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationLevel;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationType;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ApplicationModelRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ApplicationModelRequestValidator.ApplicationModelRequestValidatorImpl.class})
public @interface ApplicationModelRequestValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ApplicationModelRequestValidatorImpl implements ConstraintValidator<ApplicationModelRequestValidator, ApplicationModelRequest> {
        @Override
        public boolean isValid(ApplicationModelRequest request, ConstraintValidatorContext context) {
            ApplicationModelType applicationModelType = request.getApplicationModelType();
            ApplicationType applicationType = request.getApplicationType();
            ApplicationLevel applicationLevel = request.getApplicationLevel();
            if (applicationModelType == null || (!applicationModelType.equals(ApplicationModelType.PRICE_AM_PER_PIECE) && applicationType == null)) {
                return false;
            }
            if (applicationModelType.equals(ApplicationModelType.PRICE_AM_PER_PIECE)) {
                if (request.getPerPieceRequest() == null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.perPieceRequest-perPieceRequest can not be null for this application model and application type;").addConstraintViolation();
                    return false;
                }
                if (request.getApplicationType() != null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.applicationType-applicationType should be null for this model type;").addConstraintViolation();
                    return false;
                }
            } else if (applicationModelType.equals(ApplicationModelType.PRICE_AM_OVERTIME) && applicationType.equals(ApplicationType.ONE_TIME)) {
                if (request.getOverTimeOneTimeRequest() == null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.overTimeOneTimeRequest-overTimeOneTimeRequest can not be null for this application model and application type;").addConstraintViolation();
                    return false;
                }
                if (applicationLevel == null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.applicationLevel-ApplicationLevel is mandatory when the Price application model over time is chosen;").addConstraintViolation();
                    return false;
                }
            } else if (applicationModelType.equals(ApplicationModelType.PRICE_AM_OVERTIME) && applicationType.equals(ApplicationType.PERIODICALLY)) {
                if (request.getOverTimePeriodicallyRequest() == null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.overTimePeriodicallyRequest-overTimePeriodicallyRequest can not be null for this application model and application type;").addConstraintViolation();
                    return false;
                }
                if (applicationLevel == null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.applicationLevel-ApplicationLevel is mandatory when the Price application model over time is chosen;").addConstraintViolation();
                    return false;
                }
            } else if (applicationModelType.equals(ApplicationModelType.PRICE_AM_FOR_VOLUMES) && applicationType.equals(ApplicationType.BY_SCALES)) {
                if (request.getVolumesByScaleRequest() == null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.volumesByScaleRequest-volumesByScaleRequest can not be null for this application model and application type;").addConstraintViolation();
                    return false;
                }
            } else if (applicationModelType.equals(ApplicationModelType.PRICE_AM_FOR_VOLUMES) && applicationType.equals(ApplicationType.BY_SETTLEMENT_PERIODS)) {
                if (request.getSettlementPeriodsRequest() == null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.settlementPeriodsRequest-settlementPeriodsRequest can not be null for this application model and application type;").addConstraintViolation();
                    return false;
                }
            } else if (!applicationModelType.equals(ApplicationModelType.PRICE_AM_OVERTIME)) {
                if (applicationLevel != null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.applicationLevel-applicationLevel can only be chosen when the application model type is Price application model over time;").addConstraintViolation();
                    return false;
                }
            } else if (applicationType.equals(ApplicationType.WITH_ELECTRICITY_INVOICE)) {
                if (request.getOverTimeWithElectricityInvoiceRequest() == null) {
                    context.buildConstraintViolationWithTemplate("applicationModelRequest.overTimeWithElectricityInvoiceRequest-overTimeWithElectricityInvoiceRequest can not be null for this application model and application type;").addConstraintViolation();
                    return false;
                }
            }
            return true;
        }
    }
}

package bg.energo.phoenix.model.customAnotations.product.priceComponent.applicationModel;

import bg.energo.phoenix.model.request.product.price.aplicationModel.ProfilesRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ProfilesRequestValidator.ProfilesRequestValidatorImpl.class})

public @interface ProfilesRequestValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ProfilesRequestValidatorImpl implements ConstraintValidator<ProfilesRequestValidator, List<ProfilesRequest>> {

        @Override
        public boolean isValid(List<ProfilesRequest> request, ConstraintValidatorContext context) {
            if(CollectionUtils.isEmpty(request)){
                return false;
            }
            Set<Long> collect = request.stream().map(ProfilesRequest::getProfileId).collect(Collectors.toSet());
            if(collect.size()!=request.size()){
                context.buildConstraintViolationWithTemplate("applicationModelRequest.settlementPeriodsRequest.profiles-profiles contain duplicate value;").addConstraintViolation();
                return false;
            }
            return true;
        }
    }

}

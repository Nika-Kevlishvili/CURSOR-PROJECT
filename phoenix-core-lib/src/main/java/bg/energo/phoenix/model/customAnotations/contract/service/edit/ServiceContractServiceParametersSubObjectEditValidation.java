package bg.energo.phoenix.model.customAnotations.contract.service.edit;

import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractContractNumbersEditRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractPodsEditRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractServiceParametersEditRequest;
import bg.energo.phoenix.model.request.contract.service.edit.ServiceContractUnrecognizedPodsEditRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ServiceContractServiceParametersSubObjectEditValidation.ServiceContractServiceParametersSubObjectEditValidationImpl.class})
public @interface ServiceContractServiceParametersSubObjectEditValidation {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceContractServiceParametersSubObjectEditValidationImpl implements ConstraintValidator<ServiceContractServiceParametersSubObjectEditValidation, ServiceContractServiceParametersEditRequest> {
        @Override
        public boolean isValid(ServiceContractServiceParametersEditRequest request, ConstraintValidatorContext context) {
            List<ServiceContractContractNumbersEditRequest> contractNumbersEditList = request.getContractNumbersEditList();
            List<ServiceContractPodsEditRequest> podsEditList = request.getPodsEditList();
            List<ServiceContractUnrecognizedPodsEditRequest> unrecognizedPodsEditList = request.getUnrecognizedPodsEditList();
           /* if (CollectionUtils.isEmpty(contractNumbersEditList) && CollectionUtils.isEmpty(podsEditList) && CollectionUtils.isEmpty(unrecognizedPodsEditList)) {
                context.buildConstraintViolationWithTemplate("paymentGuarantee.contractNumbersEditList- [contractNumbersEditList], " +
                        "paymentGuarantee.podsEditList- [podsEditList] and " +
                        "paymentGuarantee.unrecognizedPodsEditList- [unrecognizedPodsEditList] can't be empty at same same time;").addConstraintViolation();
                return false;
            }
            if(!CollectionUtils.isEmpty(contractNumbersEditList) && !CollectionUtils.isEmpty(podsEditList) && !CollectionUtils.isEmpty(unrecognizedPodsEditList)){
                context.buildConstraintViolationWithTemplate("paymentGuarantee.contractNumbersEditList- [contractNumbersEditList], "+
                        "paymentGuarantee.podsEditList- [podsEditList] and "+
                        "paymentGuarantee.unrecognizedPodsEditList- [unrecognizedPodsEditList] can't be empty at same same time;").addConstraintViolation();
                return false;
            }*/

            return true;
        }
    }
}

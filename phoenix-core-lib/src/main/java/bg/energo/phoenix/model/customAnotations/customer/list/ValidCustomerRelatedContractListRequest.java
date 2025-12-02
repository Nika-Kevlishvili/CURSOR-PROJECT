package bg.energo.phoenix.model.customAnotations.customer.list;

import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.request.customer.list.CustomerRelatedContractListRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidCustomerRelatedContractListRequest.CustomerRelatedContractListRequestValidator.class})
public @interface ValidCustomerRelatedContractListRequest {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerRelatedContractListRequestValidator implements ConstraintValidator<ValidCustomerRelatedContractListRequest, CustomerRelatedContractListRequest> {

        @Override
        public boolean isValid(CustomerRelatedContractListRequest request, ConstraintValidatorContext context) {
            StringBuilder sb = new StringBuilder();

            validateContractTypes(request, sb);
            validateContractStatuses(request, sb);
            validateContractSubStatuses(request, sb);


            if (!sb.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }

        private static void validateContractTypes(CustomerRelatedContractListRequest request, StringBuilder sb) {
            if (CollectionUtils.isNotEmpty(request.getContractTypes())) {
                for (String contractType : request.getContractTypes()) {
                    if (Arrays.stream(ContractType.values()).map(Enum::name).noneMatch(type -> type.equals(contractType))) {
                        sb.append("contractTypes-Invalid contract type: ").append(contractType).append(";");
                    }
                }
            }
        }

        private static void validateContractStatuses(CustomerRelatedContractListRequest request, StringBuilder sb) {
            if (CollectionUtils.isNotEmpty(request.getContractStatuses())) {
                boolean noneMatchesFromProductContractStatuses = false;
                for (String contractStatus : request.getContractStatuses()) {
                    if (Arrays.stream(ContractDetailsStatus.values()).noneMatch(status -> status.name().equals(contractStatus))) {
                        noneMatchesFromProductContractStatuses = true;
                        break;
                    }
                }

                boolean noneMatchesFromServiceContractStatuses = false;
                for (String contractStatus : request.getContractStatuses()) {
                    if (Arrays.stream(ServiceContractDetailStatus.values()).noneMatch(status -> status.name().equals(contractStatus))) {
                        noneMatchesFromServiceContractStatuses = true;
                        break;
                    }
                }

                if (noneMatchesFromProductContractStatuses && noneMatchesFromServiceContractStatuses) {
                    sb.append("contractStatuses-List contains invalid contract status.");
                }
            }
        }

        private static void validateContractSubStatuses(CustomerRelatedContractListRequest request, StringBuilder sb) {
            if (CollectionUtils.isNotEmpty(request.getContractSubStatuses())) {
                boolean noneMatchesFromProductContractSubStatuses = false;
                for (String contractSubStatus : request.getContractSubStatuses()) {
                    if (Arrays.stream(ContractDetailsSubStatus.values()).noneMatch(status -> status.name().equals(contractSubStatus))) {
                        noneMatchesFromProductContractSubStatuses = true;
                        break;
                    }
                }

                boolean noneMatchesFromServiceContractSubStatuses = false;
                for (String contractSubStatus : request.getContractSubStatuses()) {
                    if (Arrays.stream(ServiceContractDetailsSubStatus.values()).noneMatch(status -> status.name().equals(contractSubStatus))) {
                        noneMatchesFromServiceContractSubStatuses = true;
                        break;
                    }
                }

                if (noneMatchesFromProductContractSubStatuses && noneMatchesFromServiceContractSubStatuses) {
                    sb.append("contractSubStatuses-List contains invalid contract sub status.");
                }
            }
        }

    }
}

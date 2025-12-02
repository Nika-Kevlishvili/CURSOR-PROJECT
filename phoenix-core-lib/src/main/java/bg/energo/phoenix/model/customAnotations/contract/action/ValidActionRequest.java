package bg.energo.phoenix.model.customAnotations.contract.action;

import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer;
import bg.energo.phoenix.model.request.contract.action.ActionRequest;
import bg.energo.phoenix.util.contract.action.ActionTypeProperties;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.time.LocalDate;

import static bg.energo.phoenix.model.enums.contract.action.ActionPenaltyPayer.EPRES;
import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidActionRequest.ActionRequestValidator.class})
public @interface ValidActionRequest {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @RequiredArgsConstructor
    class ActionRequestValidator implements ConstraintValidator<ValidActionRequest, ActionRequest> {

        private final ActionTypeProperties actionTypeProperties;

        @Override
        public boolean isValid(ActionRequest request, ConstraintValidatorContext context) {
            StringBuilder sb = new StringBuilder();

            // NOTE: null checks for mandatory fields are handled by @NotNull annotation in ActionRequest class

            validateNoticeAndExecutionDate(request, sb);
            validatePenalty(request, sb);
            validateTermination(request, sb);
            validatePenaltyClaim(request, sb);
            validatePenaltyPayer(request, sb);
            validateContractAndActionType(request, sb);

            if (!sb.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }


        private void validateNoticeAndExecutionDate(ActionRequest request, StringBuilder sb) {
            LocalDate noticeReceivingDate = request.getNoticeReceivingDate();
            LocalDate executionDate = request.getExecutionDate();

            if (noticeReceivingDate != null && executionDate != null && noticeReceivingDate.isAfter(executionDate)) {
                sb.append("noticeReceivingDate-Notice receiving date should be before the execution date;");
            }
        }

        private void validatePenalty(ActionRequest request, StringBuilder sb) {
            Long penaltyId = request.getPenaltyId();
            Boolean withoutPenalty = request.getWithoutPenalty();

            if ((penaltyId == null && Boolean.FALSE.equals(withoutPenalty))
                    || (penaltyId != null && Boolean.TRUE.equals(withoutPenalty))) {
                sb.append("penaltyId-Either penalty or [without penalty] option should be selected;");
            }
        }

        private void validateTermination(ActionRequest request, StringBuilder sb) {
            Long terminationId = request.getTerminationId();
            Boolean withoutAutomaticTermination = request.getWithoutAutomaticTermination();
            Long actionTypeId = request.getActionTypeId();

            if (actionTypeId != null) {
                if (actionTypeProperties.isActionTypeRelatedToContractTermination(actionTypeId)) {
                    if ((terminationId == null && Boolean.FALSE.equals(withoutAutomaticTermination))
                            || (terminationId != null && Boolean.TRUE.equals(withoutAutomaticTermination))) {
                        sb.append("terminationId-Either termination or [without automatic termination] option should be selected;");
                    }
                } else {
                    if (terminationId != null || withoutAutomaticTermination != null) {
                        sb.append("terminationId-Termination and [without automatic termination] options are not allowed for this action type;");
                    }
                }
            }
        }

        private void validatePenaltyClaim(ActionRequest request, StringBuilder sb) {
            BigDecimal penaltyClaimAmount = request.getPenaltyClaimAmount();
            Long penaltyClaimAmountCurrencyId = request.getPenaltyClaimAmountCurrencyId();

            if (penaltyClaimAmount == null && penaltyClaimAmountCurrencyId != null) {
                sb.append("penaltyClaimAmount-Penalty claim amount is required when corresponding currency is selected;");
            }

            if (penaltyClaimAmount != null && penaltyClaimAmountCurrencyId == null) {
                sb.append("penaltyClaimAmountCurrencyId-Currency is required when corresponding penalty claim amount is entered;");
            }
        }

        private void validatePenaltyPayer(ActionRequest request, StringBuilder sb) {
            ActionPenaltyPayer penaltyPayer = request.getPenaltyPayer();
            if (penaltyPayer != null && penaltyPayer.equals(EPRES)) {
                if (request.getPenaltyClaimAmount() != null) {
                    sb.append("penaltyPayer-When EPRES is selected as penalty payer, penalty claim amount should be empty;");
                }

                if (request.getPenaltyClaimAmountCurrencyId() != null) {
                    sb.append("penaltyPayer-When EPRES is selected as penalty payer, penalty claim amount currency should be empty;");
                }

                if (BooleanUtils.isTrue(request.getDontAllowAutomaticPenaltyClaim())) {
                    sb.append("penaltyPayer-When EPRES is selected as penalty payer, [Do not allow automatic penalty claim] checkbox should not be checked;");
                }
            }
        }

        private void validateContractAndActionType(ActionRequest request, StringBuilder sb) {
            Long actionTypeId = request.getActionTypeId();
            ContractType contractType = request.getContractType();

            if (contractType != null && actionTypeId != null) {
                if (contractType.equals(ContractType.PRODUCT_CONTRACT)) {
                    validateProductContract(request, sb, actionTypeId);
                } else {
                    validateServiceContract(request, sb, actionTypeId);
                }
            }
        }

        private void validateProductContract(ActionRequest request, StringBuilder sb, Long actionTypeId) {
            if (actionTypeProperties.isActionTypeRelatedToPodTermination(actionTypeId)) {
                if (CollectionUtils.isEmpty(request.getPods())) {
                    sb.append("pods-Pods are required for product contract and this action type combination;");
                }
            } else {
                if (CollectionUtils.isNotEmpty(request.getPods())) {
                    sb.append("pods-Pods are not allowed for this contract type and action type combination;");
                }
            }
        }

        private void validateServiceContract(ActionRequest request, StringBuilder sb, Long actionTypeId) {
            if (actionTypeProperties.isActionTypeRelatedToPodTermination(actionTypeId)) {
                sb.append("actionTypeId-This action type is not allowed for service contract;");
            }

            if (CollectionUtils.isNotEmpty(request.getPods())) {
                sb.append("pods-Pods are not allowed for this contract type and action type combination;");
            }
        }

    }

}

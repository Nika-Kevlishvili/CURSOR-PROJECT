package bg.energo.phoenix.model.customAnotations.contract.order.goods;

import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderBaseRequest;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderBasicParametersCreateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidGoodsOrderBankingDetails.GoodsOrderBankingDetailsValidator.class})
public @interface ValidGoodsOrderBankingDetails {
    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsOrderBankingDetailsValidator implements ConstraintValidator<ValidGoodsOrderBankingDetails, GoodsOrderBaseRequest> {

        @Override
        public boolean isValid(GoodsOrderBaseRequest request, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();

            GoodsOrderBasicParametersCreateRequest basicParameters = request.getBasicParameters();


            StringBuilder errorMessage = new StringBuilder();

            if (Boolean.TRUE.equals(basicParameters.getDirectDebit())) {
                if (basicParameters.getBankId() == null) {
                    errorMessage.append("basicParameters.bankId-Bank is mandatory when direct debit is selected;");
                }

                if (StringUtils.isEmpty(basicParameters.getIban())) {
                    errorMessage.append("basicParameters.iban-IBAN is mandatory when direct debit is selected;");
                }
            }

            if (!errorMessage.isEmpty()) {
                context.buildConstraintViolationWithTemplate(errorMessage.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }

    }
}

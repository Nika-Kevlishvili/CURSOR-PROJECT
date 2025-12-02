package bg.energo.phoenix.model.customAnotations.contract.order.goods.request;

import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderProxyAddRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.regex.Pattern;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {GoodsOrderProxyEmailValidation.GoodsOrderProxyEmailValidationImpl.class})
public @interface GoodsOrderProxyEmailValidation {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsOrderProxyEmailValidationImpl implements ConstraintValidator<GoodsOrderProxyEmailValidation, GoodsOrderProxyAddRequest> {
        @Override
        public boolean isValid(GoodsOrderProxyAddRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessage = new StringBuilder();
            String emailValidationRegex = "^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,3})+$";
            boolean isValid = true;

            if(request.getProxyEmail() != null){
                if(!(request.getProxyEmail().length() > 1 && request.getProxyEmail().length() <= 512)){
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyEmail-[proxyEmail] email length should be between 1 and 512;");
                }
            }
           /* if(request.getAuthorizedProxyEmail() != null){
                if(!(request.getAuthorizedProxyEmail().length() > 1 && request.getAuthorizedProxyEmail().length() <= 512)){
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] email length should be between 1 and 512;");
                }
            }*/

            if(request.getProxyEmail()!=null && !patternMatches(request.getProxyEmail(),emailValidationRegex)){
                isValid = false;
                validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyEmail-[proxyEmail] email validation failed;");
            }
            if(request.getAuthorizedProxyEmail()!=null && !patternMatches(request.getAuthorizedProxyEmail(),emailValidationRegex)){
                isValid = false;
                validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] email validation failed;");
            }
            if(request.getAuthorizedProxyEmail() == null && request.getProxyEmail() == null){
                isValid = false;
                validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyEmail-[proxyEmail] shouldn't be empty;");
                validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] shouldn't be empty;");
            }

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }
        public static boolean patternMatches(String emailAddress, String regexPattern) {
            return Pattern.compile(regexPattern)
                    .matcher(emailAddress)
                    .matches();
        }

    }
}

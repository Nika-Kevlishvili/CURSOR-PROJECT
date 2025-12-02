package bg.energo.phoenix.model.customAnotations.contract.proxy;

import bg.energo.phoenix.model.request.contract.ProxyAddRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.regex.Pattern;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {PhoneValidationForProxy.PhoneValidationForProxyImpl.class})
public @interface PhoneValidationForProxy {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PhoneValidationForProxyImpl implements ConstraintValidator<PhoneValidationForProxy, ProxyAddRequest> {
        @Override
        public boolean isValid(ProxyAddRequest request, ConstraintValidatorContext context) {
            StringBuilder validationMessage = new StringBuilder();
            String patternForPhoneText = "^\\+?[0-9*-+\\-()\\s]+$";
            boolean isValid = true;

            if(request.getProxyPhone() != null){
                if(!(request.getProxyPhone().length() >= 1 && request.getProxyPhone().length() <= 32)){
                    isValid = false;
                    validationMessage.append("proxyPhone-[proxyPhone] length should be between 1 and 32;");
                }
            }
            if(request.getAuthorizedProxyPhone() != null){
                if(!(request.getAuthorizedProxyPhone().length() > 1 && request.getAuthorizedProxyPhone().length() <= 32)){
                    isValid = false;
                    validationMessage.append("authorizedProxyPhone-[authorizedProxyPhone] length should be between 1 and 32;");
                }
            }
            if(request.getProxyPhone()!=null && !patternMatches(request.getProxyPhone(),patternForPhoneText)){
                isValid = false;
                validationMessage.append("proxyPhone-[proxyPhone] pattern validation failed;");
            }
            if(request.getAuthorizedProxyPhone()!=null && !patternMatches(request.getAuthorizedProxyPhone(),patternForPhoneText)){
                isValid = false;
                validationMessage.append("authorizedProxyPhone-[authorizedProxyPhone] pattern validation failed;");
            }
            if(request.getProxyPhone() == null && request.getAuthorizedProxyPhone() == null){
                isValid = false;
                validationMessage.append("proxyPhone-[proxyPhone] shouldn't be empty;");
            }

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }
        public static boolean patternMatches(String phone, String regexPattern) {
            return Pattern.compile(regexPattern)
                    .matcher(phone)
                    .matches();
        }

    }
}

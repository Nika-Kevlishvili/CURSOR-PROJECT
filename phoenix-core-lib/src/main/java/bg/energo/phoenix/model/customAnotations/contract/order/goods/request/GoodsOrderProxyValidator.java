package bg.energo.phoenix.model.customAnotations.contract.order.goods.request;

import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderProxyAddRequest;
import bg.energo.phoenix.util.customer.CustomerIdentifierValidator;
import io.micrometer.core.instrument.util.StringUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {GoodsOrderProxyValidator.GoodsOrderProxyDatesValidatorImpl.class})
public @interface GoodsOrderProxyValidator {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    class GoodsOrderProxyDatesValidatorImpl implements ConstraintValidator<GoodsOrderProxyValidator, GoodsOrderProxyAddRequest> {
        public static boolean patternMatches(String emailAddress, String regexPattern) {
            return Pattern.compile(regexPattern)
                    .matcher(emailAddress)
                    .matches();
        }

        @Override
        public boolean isValid(GoodsOrderProxyAddRequest request, ConstraintValidatorContext context) { StringBuilder validationMessage = new StringBuilder();
            boolean isValid = true;

            String emailValidationRegex = "^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,3})+$";
            String patternForPhoneText = "^[0-9*+–-]+$";
            String foreignCustomerPatternText = "^[0-9A-Z]+$";

            boolean authorizedProxyAdded = anyRequireFieldIsFilledInAuthorizedProxy(request);
            if (request.getProxyForeignEntityPerson()) {
                // 9-13 legal
                // 10 private
                String proxyIdentifier = request.getProxyCustomerIdentifier();
                if (!(!proxyIdentifier.isEmpty() && proxyIdentifier.length() <= 32)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyCustomerIdentifier-[proxyCustomerIdentifier] length should be between 1 and 32;");
                }
                if (!patternMatches(proxyIdentifier, foreignCustomerPatternText)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyCustomerIdentifier-[proxyCustomerIdentifier] does not match validation;");
                }
            } else {
                String proxyCustomerIdentifier = request.getProxyCustomerIdentifier();
                CustomerType customerType = null;
                if (proxyCustomerIdentifier.length() == 9 || proxyCustomerIdentifier.length() == 13) {
                    customerType = CustomerType.LEGAL_ENTITY;
                } else if (proxyCustomerIdentifier.length() == 10) {
                    customerType = CustomerType.PRIVATE_CUSTOMER;
                }
                if (customerType == null) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyCustomerIdentifier-[proxyCustomerIdentifier] proxy identifier cant determine customerType;");
                } else {
                    isValid = CustomerIdentifierValidator.isValidCustomerIdentifier(
                            request.getProxyCustomerIdentifier(),
                            customerType,
                            request.getProxyForeignEntityPerson(),
                            validationMessage
                    );
                }
            }

            if (authorizedProxyAdded) {
                if (request.getProxyEmail() != null) {
                    if (!(request.getProxyEmail().length() > 1 && request.getProxyEmail().length() <= 512)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyEmail-[proxyEmail] email length should be between 1 and 512;");
                    }
                }
                if(!checkForEmail(request.getProxyEmail())){
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyEmail-[proxyEmail] should have '@' and '.' symbols;");
                }
                if(StringUtils.isEmpty(request.getProxyEmail())){
                    if(StringUtils.isEmpty(request.getAuthorizedProxyEmail())){
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail]  should be present when proxyEmail is null ;");
                    }
                }
                if (request.getProxyEmail() != null && !patternMatches(request.getProxyEmail(), emailValidationRegex)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyEmail-[proxyEmail] email validation failed;");
                }

                if (request.getProxyPhone() != null) {
                    if (!(request.getProxyPhone().length() >= 1 && request.getProxyPhone().length() <= 32)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyPhone-[proxyPhone] length should be between 1 and 32;");
                    }
                }
                if (request.getProxyPhone() != null && !patternMatches(request.getProxyPhone(), patternForPhoneText)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyPhone-[proxyPhone] pattern validation failed;");
                }
            } else {
                if (request.getProxyEmail() == null || request.getProxyEmail().isEmpty()) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyEmail-[proxyEmail] shouldn't be null;");
                }
                if (request.getProxyPhone() == null || request.getProxyPhone().isEmpty()) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyPhone-[proxyPhone] shouldn't be null;");
                }
            }

            if (request.getProxyData() != null /*&& request.getProxyValidTill() != null*/) {
                if (request.getProxyData().isAfter(LocalDate.of(2090, 12, 31))) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyData-[proxyData] should be before 31-12-2090;");
                }
                if (request.getProxyData().isBefore(LocalDate.of(1990, 1, 1))) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyData-[proxyData] should be after 01-01-1990;");
                }
                LocalDate now = LocalDate.now();
                if (request.getProxyValidTill() != null) {
                    if (request.getProxyValidTill().isAfter(LocalDate.of(2090, 12, 31))) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyValidTill-[proxyValidTill] should be before 31-12-2090;");
                    }
                    if (request.getProxyValidTill().isBefore(LocalDate.of(1990, 1, 1))) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyValidTill-[proxyValidTill] should be after 01-01-1990;");
                    }
                    if (request.getProxyData().equals(request.getProxyValidTill())) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyData-[proxyData] and [proxyValidTill] cant be same;");
                    }

                    if (!request.getProxyValidTill().isAfter(now)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyValidTill-[proxyValidTill] should be after current date;");
                    }
                    if (!request.getProxyValidTill().isAfter(request.getProxyData())) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyValidTill-[proxyValidTill] should be after proxy date;");
                    }
                    if (!request.getProxyData().isBefore(request.getProxyValidTill())) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyDate-[proxyDate] should be after validDate date;");
                    }
                }
                if (!request.getProxyData().isBefore(now)) {
                    if (!request.getProxyData().equals(now)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyData-[proxyData] Should be before current date;");
                    }
                }
            } else {
                isValid = false;
                validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyData-[proxyData] should not be null;");
            }
            String registrationNumber = request.getRegistrationNumber();
            if (registrationNumber != null) {
                String registrationNumberRegex = "^[0-9A-Za-zА-Яа-я–-]+$";
                if (!(registrationNumber.length() >= 1 && registrationNumber.length() <= 512)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.registrationNumber-[registrationNumber] size should be between 1 and 512 symbols;");
                }
                if (!matchesPattern(registrationNumber, registrationNumberRegex)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyPowerOfAttorneyNumber-[proxyPowerOfAttorneyNumber] does not match allowed symbols;");
                }
            }
            if (anyRequireFieldIsFilledInAuthorizedProxy(request)) {
                if (request.getAuthorizedProxyData() != null /*&& request.getAuthorizedProxyValidTill() != null*/) {
                    if (request.getAuthorizedProxyData().isAfter(LocalDate.of(2090, 12, 31))) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyData-[authorizedProxyData] should be before 31-12-2090;");
                    }
                    if (request.getAuthorizedProxyData().isBefore(LocalDate.of(1990, 1, 1))) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyData-[authorizedProxyData] should be after 01-01-1990;");
                    }
                } else {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyData-[authorizedProxyData] should not be null;");
                }
            }

            if (authorizedProxyAdded) {
                if (request.getAuthorizedProxyForeignEntityPerson()) {
                    String authorizedProxyIdentifier = request.getAuthorizedProxyCustomerIdentifier();
                    if (!(!authorizedProxyIdentifier.isEmpty() && authorizedProxyIdentifier.length() <= 32)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyCustomerIdentifier-[authorizedProxyCustomerIdentifier] length should be between 1 and 32;");
                    }
             /*       if (!patternMatches(authorizedProxyIdentifier, foreignCustomerPatternText)) {
                        isValid = false;
                        validationMessage.append("basicParameters.proxy.proxyCustomerIdentifier-[proxyCustomerIdentifier] does not match validation;");
                    }*/
                    if(!checkIdentifierFormat(authorizedProxyIdentifier)){
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyCustomerIdentifier-[authorizedProxyCustomerIdentifier] Customer Identifier invalid format or symbols;");
                    }
                } else {
                    String authorizedProxyIdentifier = request.getAuthorizedProxyCustomerIdentifier();
                    CustomerType customerType = null;
                    if (authorizedProxyIdentifier.length() == 9 || authorizedProxyIdentifier.length() == 13) {
                        customerType = CustomerType.LEGAL_ENTITY;
                    } else if (authorizedProxyIdentifier.length() == 10) {
                        customerType = CustomerType.PRIVATE_CUSTOMER;
                    } else {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyCustomerIdentifier-[authorizedProxyCustomerIdentifier] can't determine customer type;");
                    }
                    boolean isAuthorizedProxyIdentifierValid = CustomerIdentifierValidator.isValidCustomerIdentifier(
                            request.getAuthorizedProxyCustomerIdentifier(),
                            customerType,
                            request.getAuthorizedProxyForeignEntityPerson(),
                            validationMessage
                    );
                    if (!isAuthorizedProxyIdentifierValid) {
                        isValid = false;
                    }
                }
                if (request.getAuthorizedProxyForeignEntityPerson() == null) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyForeignEntityPerson-[authorizedProxyForeignEntityPerson] can't be null;");
                }
                String proxyAuthorizedByProxy = request.getProxyAuthorizedByProxy();
                String proxyAuthorizedByProxyRegex = "^[А-Яа-яA-Za-z0-9\\d–@#$&*()+\\-:.,'‘€№=\\s]*$";
                if (proxyAuthorizedByProxy == null || proxyAuthorizedByProxy.isEmpty()) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyAuthorizedByProxy-[proxyAuthorizedByProxy] is mandatory;");
                } else if (proxyAuthorizedByProxy.length() >= 512) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyAuthorizedByProxy-[proxyAuthorizedByProxy] size should be between 1 and 512 symbols;");
                } else if (!matchesPattern(proxyAuthorizedByProxy, proxyAuthorizedByProxyRegex)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyAuthorizedByProxy-[proxyAuthorizedByProxy] does not match allowed symbols;");
                }
                String authorizedProxyCustomerIdentifier = request.getAuthorizedProxyCustomerIdentifier();
                if (authorizedProxyCustomerIdentifier == null || authorizedProxyCustomerIdentifier.isEmpty()) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyCustomerIdentifier-[authorizedProxyCustomerIdentifier] is required;");
                } else if (authorizedProxyCustomerIdentifier.length() >= 32) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyCustomerIdentifier-[authorizedProxyCustomerIdentifier] size should be between 1 and 32 symbols;");
                }
                String authorizedProxyEmail = request.getAuthorizedProxyEmail();
                if (authorizedProxyEmail != null) {
                    if (!(authorizedProxyEmail.length() > 1 && authorizedProxyEmail.length() <= 512)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] email length should be between 1 and 512;");
                    }
                }
                if(!checkForEmail(authorizedProxyEmail)){
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] should have '@' and '.' symbols;");
                }
                if (authorizedProxyEmail != null && !patternMatches(authorizedProxyEmail, emailValidationRegex)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] email validation failed;");
                }
                if (authorizedProxyEmail == null && request.getProxyEmail() == null) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyEmail-[proxyEmail] shouldn't be empty;");
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyEmail-[authorizedProxyEmail] shouldn't be empty;");
                }

                String authorizedProxyPhone = request.getAuthorizedProxyPhone();
                if (authorizedProxyPhone != null) {
                    if (!(authorizedProxyPhone.length() >= 1 && authorizedProxyPhone.length() <= 32)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyPhone-[authorizedProxyPhone] length should be between 1 and 32;");
                    }
                }
                if (authorizedProxyPhone != null && !patternMatches(authorizedProxyPhone, patternForPhoneText)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyPhone-[authorizedProxyPhone] pattern validation failed;");
                }
                if (request.getProxyPhone() == null) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.proxyPhone-[proxyPhone] shouldn't be empty;");
                }

                String authorizedProxyPowerOfAttorneyNumber = request.getAuthorizedProxyPowerOfAttorneyNumber();
                String authorizedProxyPowerOfAttorneyNumberRegex = "^[0-9A-Za-zА-Яа-я–-]+$";
                if (authorizedProxyPowerOfAttorneyNumber == null || authorizedProxyPowerOfAttorneyNumber.isEmpty()) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyPowerOfAttorneyNumber-[authorizedProxyPowerOfAttorneyNumber] shouldn't be null;");
                } else if (authorizedProxyPowerOfAttorneyNumber.length() >= 32) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyPowerOfAttorneyNumber-[authorizedProxyPowerOfAttorneyNumber] size should be between 1 and 32 symbols;");
                } else if (!matchesPattern(authorizedProxyPowerOfAttorneyNumber, authorizedProxyPowerOfAttorneyNumberRegex)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyPowerOfAttorneyNumber-[authorizedProxyPowerOfAttorneyNumber] does not match allowed symbols;");
                }
                LocalDate authorizedProxyData = request.getAuthorizedProxyData();
                if (authorizedProxyData == null) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyData-[authorizedProxyData] shouldn't be null;");
                }
                LocalDate authorizedProxyValidTill = request.getAuthorizedProxyValidTill();
                LocalDate now = LocalDate.now();
                if (authorizedProxyValidTill != null) {
                    if (authorizedProxyValidTill.isAfter(LocalDate.of(2090, 12, 31))) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyValidTill-[authorizedProxyValidTill] should be before 31-12-2090;");
                    }
                    if (authorizedProxyValidTill.isBefore(LocalDate.of(1990, 1, 1))) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyValidTill-[authorizedProxyValidTill] should be after 01-01-1990;");
                    }
                    /*if (authorizedProxyData != null && request.getAuthorizedProxyData().equals(authorizedProxyValidTill)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyData-[authorizedProxyData] and [authorizedProxyValidTill] cant be same;");
                    }*/

                    if (!authorizedProxyValidTill.isAfter(now)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyValidTill-[authorizedProxyValidTill] should be after current date;");
                    }
                    if (!request.getAuthorizedProxyData().isBefore(now)) {
                        if (!request.getAuthorizedProxyData().equals(now)) {
                            isValid = false;
                            validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyData-[authorizedProxyData] Should be before current date;");
                        }
                    }
                }
                String authorizedProxyRegistrationNumber = request.getAuthorizedProxyRegistrationNumber();
                String authorizedProxyRegistrationNumberRegex = "^[0-9A-Za-zА-Яа-я–-]+$";
                if (authorizedProxyRegistrationNumber != null) {
                    if (authorizedProxyRegistrationNumber.length() >= 32) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyRegistrationNumber-[authorizedProxyRegistrationNumber] size should be between 1 and 32 symbols;");
                    } else if (!matchesPattern(authorizedProxyRegistrationNumber, authorizedProxyRegistrationNumberRegex)) {
                        isValid = false;
                        validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyRegistrationNumber-[authorizedProxyRegistrationNumber] does not match allowed symbols;");
                    }
                }
                String authorizedProxyAreaOfOperation = request.getAuthorizedProxyAreaOfOperation();
                String authorizedProxyAreaOfOperationRegex = "^[А-Яа-яA-Za-z\\d–@#$&*()+-:.,‘€№=\\s]*$";
                if (authorizedProxyAreaOfOperation == null || authorizedProxyAreaOfOperation.isEmpty()) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyAreaOfOperation-[authorizedProxyAreaOfOperation] shouldn't be null;");
                } else if (authorizedProxyAreaOfOperation.length() >= 512) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyAreaOfOperation-[authorizedProxyAreaOfOperation] size should be between 1 and 32 symbols;");
                } else if (!matchesPattern(authorizedProxyAreaOfOperation, authorizedProxyAreaOfOperationRegex)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyAreaOfOperation-[authorizedProxyAreaOfOperation] does not match allowed symbols;");
                }
                String authorizedProxyNotaryPublic = request.getAuthorizedProxyNotaryPublic();
                String authorizedProxyNotaryPublicRegex = "^[А-Яа-яA-Za-z\\d–@#$&*()+-:.,‘€№=\\s]*$";
                if (authorizedProxyNotaryPublic == null || authorizedProxyNotaryPublic.isEmpty()) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyAreaOfOperation-[authorizedProxyAreaOfOperation] shouldn't be null;");
                } else if (authorizedProxyNotaryPublic.length() >= 512) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyAreaOfOperation-[authorizedProxyAreaOfOperation] size should be between 1 and 32 symbols;");
                } else if (!matchesPattern(authorizedProxyNotaryPublic, authorizedProxyNotaryPublicRegex)) {
                    isValid = false;
                    validationMessage.append("basicParameters.goodsOrderProxyAddRequest.authorizedProxyNotaryPublic-[authorizedProxyNotaryPublic] does not match allowed symbols;");
                }
            }

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
            }

            return isValid;
        }
        private Boolean checkForEmail(String text){
            if(StringUtils.isNotEmpty(text)){
                return text.contains("@") && text.contains(".");
            } else return true;
        }
        private Boolean checkIdentifierFormat(String proxyIdentifier) {
            if(proxyIdentifier.length() >= 8){
                String dateString = proxyIdentifier.substring(0, 8);
                String lastFourChars = proxyIdentifier.substring(proxyIdentifier.length() - 4);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                dateFormat.setLenient(false);
                try {
                    Date date = dateFormat.parse(dateString);
                    return lastFourChars.matches("[A-Za-z0-9]+");
                } catch (Exception e) {
                    return false;
                }
            } else return false;
        }

        public boolean matchesPattern(String input, String regex) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);
            return matcher.matches();
        }

        private boolean anyRequireFieldIsFilledInAuthorizedProxy(GoodsOrderProxyAddRequest request) {
            if (request.getAuthorizedProxyForeignEntityPerson() != null) {
                return true;
            } else if (request.getProxyAuthorizedByProxy() != null) {
                return true;
            } else if (request.getAuthorizedProxyCustomerIdentifier() != null) {
                return true;
            } else if (request.getAuthorizedProxyEmail() != null) {
                return true;
            } else if (request.getAuthorizedProxyPhone() != null) {
                return true;
            } else if (request.getAuthorizedProxyPowerOfAttorneyNumber() != null) {
                return true;
            } else if (request.getAuthorizedProxyData() != null) {
                return true;
            } else if (request.getAuthorizedProxyNotaryPublic() != null) {
                return true;
            } else return request.getAuthorizedProxyAreaOfOperation() != null;
        }
    }
}

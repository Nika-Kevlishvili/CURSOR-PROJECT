package bg.energo.phoenix.model.customAnotations.contract.order.service;

import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderAuthorizedProxyRequest;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyBaseRequest;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyRequest;
import bg.energo.phoenix.util.customer.CustomerIdentifierValidator;
import bg.energo.phoenix.util.epb.EPBListUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.*;
import java.util.List;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ValidServiceOrderProxies.ValidServiceOrderProxyImpl.class})
public @interface ValidServiceOrderProxies {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ValidServiceOrderProxyImpl implements ConstraintValidator<ValidServiceOrderProxies, List<? extends ServiceOrderProxyBaseRequest>> {

        @Override
        public boolean isValid(List<? extends ServiceOrderProxyBaseRequest> proxies, ConstraintValidatorContext constraintValidatorContext) {
            if (CollectionUtils.isEmpty(proxies)) {
                return true;
            }

            StringBuilder validationMessage = new StringBuilder();

            for (int i = 0; i < proxies.size(); i++) {
                ServiceOrderProxyBaseRequest request = proxies.get(i);
                if (request.getProxy() == null) {
                    continue; // this case will be validated by NotNull annotation
                }

                ServiceOrderProxyRequest proxy = request.getProxy();
                ServiceOrderAuthorizedProxyRequest authorizedProxy = request.getAuthorizedProxy();

                if (StringUtils.isEmpty(proxy.getEmail())
                        && (authorizedProxy == null || StringUtils.isEmpty(authorizedProxy.getEmail()))) {
                    validationMessage.append("basicParameters.proxies[%s].proxy.email-Proxy email should be present if authorized proxy's email is empty;".formatted(i));
                }

                if (authorizedProxy != null && StringUtils.isEmpty(authorizedProxy.getEmail())) {
                    validationMessage.append("basicParameters.proxies[%s].authorizedProxy.email-Authorized proxy email should be present;".formatted(i));
                }

                if (StringUtils.isEmpty(proxy.getPhone())
                        && (authorizedProxy == null || StringUtils.isEmpty(authorizedProxy.getPhone()))) {
                    validationMessage.append("basicParameters.proxies[%s].proxy.phone-Proxy phone should be present if authorized proxy's phone is empty;".formatted(i));
                }

                if (authorizedProxy != null && StringUtils.isEmpty(authorizedProxy.getPhone())) {
                    validationMessage.append("basicParameters.proxies[%s].authorizedProxy.phone-Authorized proxy phone should be present;".formatted(i));
                }

                validateProxyIdentifiers(request, validationMessage, i);
            }

            validateManagers(proxies, validationMessage);

            if (!validationMessage.isEmpty()) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate(validationMessage.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }


        private static void validateManagers(List<? extends ServiceOrderProxyBaseRequest> proxies, StringBuilder validationMessage) {
            List<Long> managerIds = proxies
                    .stream()
                    .flatMap(p -> p.getManagers().stream())
                    .toList();

            if (EPBListUtils.notAllUnique(managerIds)) {
                validationMessage.append("You cannot add multiple proxies for the same manager.");
            }
        }


        /**
         * Validates the identifiers of the proxy and the authorized proxy to match the validations for UIC/personal numbers.
         *
         * @param request The request containing the parameters for the proxy.
         */
        private static void validateProxyIdentifiers(ServiceOrderProxyBaseRequest request, StringBuilder validationMessage, int index) {
            StringBuilder sb = new StringBuilder();

            if (BooleanUtils.isFalse(request.getProxy().getForeignEntity())) {
                if (StringUtils.isEmpty(request.getProxy().getCustomerIdentifier())) {
                    return; // will be handled by not null validation
                }

                String customerIdentifier = request.getProxy().getCustomerIdentifier();
                if (customerIdentifier.length() == 9 || customerIdentifier.length() == 13) {
                    CustomerIdentifierValidator.validateUIC(customerIdentifier, false, sb);
                } else if (customerIdentifier.length() == 10) {
                    CustomerIdentifierValidator.validatePersonalNumber(customerIdentifier, false, sb);
                } else {
                    validationMessage.append("basicParameters.proxies[%s].proxy.customerIdentifier-Customer Identifier invalid format or symbols;".formatted(index));
                }

                if (!sb.isEmpty()) {
                    validationMessage.append("basicParameters.proxies[%s].proxy.customerIdentifier-Customer Identifier invalid format or symbols;".formatted(index));
                }
            } else {
                if (StringUtils.isEmpty(request.getProxy().getCustomerIdentifier())) {
                    return; // will be handled by not null validation
                }

                if (!request.getProxy().getCustomerIdentifier().matches("^[0-9A-Z/–-]+${1,32}")) {
                    validationMessage.append("basicParameters.proxies[%s].proxy.customerIdentifier-Customer Identifier invalid format or symbols;".formatted(index));
                }
            }

            if (request.getAuthorizedProxy() != null) {
                if (StringUtils.isEmpty(request.getAuthorizedProxy().getCustomerIdentifier())) {
                    return;
                }

                // authorized proxy can be of only private type
                CustomerIdentifierValidator.validatePersonalNumber(
                        request.getAuthorizedProxy().getCustomerIdentifier(),
                        !BooleanUtils.isFalse(request.getAuthorizedProxy().getForeignEntity()),
                        sb
                );

                if (!sb.isEmpty()) {
                    validationMessage.append("basicParameters.proxies[%s].authorizedProxy.customerIdentifier-Customer Identifier invalid format or symbols;".formatted(index));
                }
            }
        }
    }

}

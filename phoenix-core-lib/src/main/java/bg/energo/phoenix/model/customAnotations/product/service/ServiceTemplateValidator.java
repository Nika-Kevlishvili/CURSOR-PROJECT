package bg.energo.phoenix.model.customAnotations.product.service;

import bg.energo.phoenix.model.enums.product.service.ServiceSaleMethod;
import bg.energo.phoenix.model.request.product.product.TemplateSubObjectRequest;
import bg.energo.phoenix.model.request.product.service.BaseServiceRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {ServiceTemplateValidator.ServiceTemplateValidatorImpl.class})
public @interface ServiceTemplateValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class ServiceTemplateValidatorImpl implements ConstraintValidator<ServiceTemplateValidator, BaseServiceRequest> {
        @Override
        public boolean isValid(BaseServiceRequest request, ConstraintValidatorContext context) {
            if(request.getBasicSettings()==null){
                return true;
            }
            return validateTemplates(context, request.getBasicSettings().getSaleMethods(), request.getTemplateIds());
        }

        private static boolean validateTemplates(ConstraintValidatorContext context,Set<ServiceSaleMethod> saleMethods, Set<TemplateSubObjectRequest> templateIds) {
            //TODO TEMPLATE for delivery purpose - should be removed
            /*if(saleMethods==null){
                return true;
            }
            if (templateIds == null) {
                context.buildConstraintViolationWithTemplate("templateIds-templateIds can not be null!;").addConstraintViolation();
                return false;
            }
            Map<ProductServiceTemplateType, List<TemplateSubObjectRequest>> collect =
                    templateIds.stream().collect(Collectors.groupingBy(TemplateSubObjectRequest::getTemplateType));
            StringBuilder messages = new StringBuilder();
            if (!collect.containsKey(ProductServiceTemplateType.CONTRACT_TEMPLATE) && saleMethods.contains(ServiceSaleMethod.CONTRACT)) {
                messages.append("templateIds[%s]-Contract template should be provided!;".formatted(ProductServiceTemplateType.CONTRACT_TEMPLATE));
            }
            if (!collect.containsKey(ProductServiceTemplateType.INVOICE_TEMPLATE)) {
                messages.append("templateIds[%s]-Invoice template should be provided!;".formatted(ProductServiceTemplateType.INVOICE_TEMPLATE));
            }
            if (!collect.containsKey(ProductServiceTemplateType.EMAIL_TEMPLATE)) {
                messages.append("templateIds[%s]-Email template should be provided!;".formatted(ProductServiceTemplateType.EMAIL_TEMPLATE));
            }
            if (collect.containsKey(ProductServiceTemplateType.EMAIL_TEMPLATE) && collect.get(ProductServiceTemplateType.EMAIL_TEMPLATE).size() > 1) {
                messages.append("templateIds[%s]-Only one Email template should be provided!;".formatted(ProductServiceTemplateType.EMAIL_TEMPLATE));
            }
            if (collect.containsKey(ProductServiceTemplateType.INVOICE_TEMPLATE) && collect.get(ProductServiceTemplateType.INVOICE_TEMPLATE).size() > 1) {
                messages.append("templateIds[%s]-Only one Invoice template should be provided!;".formatted(ProductServiceTemplateType.INVOICE_TEMPLATE));
            }
            if (!messages.isEmpty()) {
                context.buildConstraintViolationWithTemplate(messages.toString()).addConstraintViolation();
                return false;
            }*/
            return true;
        }

    }
}

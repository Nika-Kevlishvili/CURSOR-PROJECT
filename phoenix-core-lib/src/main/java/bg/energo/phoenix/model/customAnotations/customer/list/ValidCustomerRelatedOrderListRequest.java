package bg.energo.phoenix.model.customAnotations.customer.list;

import bg.energo.phoenix.model.enums.contract.OrderType;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.request.customer.list.CustomerRelatedOrderListRequest;
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
@Constraint(validatedBy = {ValidCustomerRelatedOrderListRequest.CustomerRelatedOrderListRequestValidator.class})
public @interface ValidCustomerRelatedOrderListRequest {

    String value() default "";

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CustomerRelatedOrderListRequestValidator implements ConstraintValidator<ValidCustomerRelatedOrderListRequest, CustomerRelatedOrderListRequest> {

        @Override
        public boolean isValid(CustomerRelatedOrderListRequest value, ConstraintValidatorContext context) {
            StringBuilder sb = new StringBuilder();

            validateOrderTypes(value, sb);
            validateOrderStatuses(value, sb);

            if (!sb.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(sb.toString()).addConstraintViolation();
                return false;
            }

            return true;
        }

        private static void validateOrderTypes(CustomerRelatedOrderListRequest request, StringBuilder sb) {
            if (CollectionUtils.isNotEmpty(request.getOrderTypes())) {
                for (String orderType : request.getOrderTypes()) {
                    if (Arrays.stream(OrderType.values()).map(Enum::name).noneMatch(type -> type.equals(orderType))) {
                        sb.append("orderTypes-Invalid order type: ").append(orderType).append(";");
                    }
                }
            }
        }

        private static void validateOrderStatuses(CustomerRelatedOrderListRequest request, StringBuilder sb) {
            if (CollectionUtils.isNotEmpty(request.getOrderStatuses())) {
                boolean noneMatchesFromServiceOrderStatuses = false;
                for (String orderStatus : request.getOrderStatuses()) {
                    if (Arrays.stream(ServiceOrderStatus.values()).map(Enum::name).noneMatch(status -> status.equals(orderStatus))) {
                        noneMatchesFromServiceOrderStatuses = true;
                        break;
                    }
                }

                boolean noneMatchesFromGoodsOrderStatuses = false;
                for (String orderStatus : request.getOrderStatuses()) {
                    if (Arrays.stream(GoodsOrderStatus.values()).map(Enum::name).noneMatch(status -> status.equals(orderStatus))) {
                        noneMatchesFromGoodsOrderStatuses = true;
                        break;
                    }
                }

                if (noneMatchesFromServiceOrderStatuses && noneMatchesFromGoodsOrderStatuses) {
                    sb.append("orderStatuses-List contains invalid order status.");
                }
            }
        }
    }

}


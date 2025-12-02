package bg.energo.phoenix.model.customAnotations.contract.order.goods.request;

import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderGoodsParametersTableItem;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.*;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {GoodsOrderGoodsParametersTableValidator.GoodsOrderGoodsParametersTableValidatorImpl.class})
public @interface GoodsOrderGoodsParametersTableValidator {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class GoodsOrderGoodsParametersTableValidatorImpl implements ConstraintValidator<GoodsOrderGoodsParametersTableValidator, List<GoodsOrderGoodsParametersTableItem>> {
        private boolean isValid;
        private StringBuilder violations;

        @Override
        public boolean isValid(List<GoodsOrderGoodsParametersTableItem> tableItems, ConstraintValidatorContext context) {
            isValid = true;
            violations = new StringBuilder();
            if (CollectionUtils.isNotEmpty(tableItems)) {
                checkTableItemsCount(tableItems);
                Range<Integer> codeForConnectionWithOtherSystemRange = Range.between(1, 256);
                Range<Integer> quantityRange = Range.between(1, 9999);
                Range<BigDecimal> priceRange = Range.between(new BigDecimal("0.01"), new BigDecimal("999999999999.99"), Comparator.naturalOrder());
                Range<Integer> numberOfIncomingAccountRange = Range.between(1, 32);
                Range<Integer> costCenterOrControllingOrderRange = Range.between(1, 32);

                Set<Long> uniqueGoodsDetailIdHolder = new HashSet<>();

                for (int i = 0; i < tableItems.size(); i++) {
                    GoodsOrderGoodsParametersTableItem goodsOrderGoodsParametersTableItem = tableItems.get(i);

                    String name = goodsOrderGoodsParametersTableItem.getName();
                    String codeForConnectionWithOtherSystem = goodsOrderGoodsParametersTableItem.getCodeForConnectionWithOtherSystem();
                    Long goodsUnitId = goodsOrderGoodsParametersTableItem.getGoodsUnitId();
                    Integer quantity = goodsOrderGoodsParametersTableItem.getQuantity();
                    BigDecimal price = goodsOrderGoodsParametersTableItem.getPrice();
                    Long currencyId = goodsOrderGoodsParametersTableItem.getCurrencyId();
                    String numberOfIncomingAccount = goodsOrderGoodsParametersTableItem.getNumberOfIncomingAccount();
                    String costCenterOrControllingOrder = goodsOrderGoodsParametersTableItem.getCostCenterOrControllingOrder();

                    Long goodsDetailId = goodsOrderGoodsParametersTableItem.getGoodsDetailId();
                    if (goodsDetailId == null) {
                        if (StringUtils.isBlank(name)) {
                            addViolation("goodsParameters.goods[%s].name-Name must not be blank;".formatted(i));
                        }

                        if (codeForConnectionWithOtherSystem != null) {
                            if (!codeForConnectionWithOtherSystemRange.contains(codeForConnectionWithOtherSystem.length())) {
                                addViolation("goodsParameters.goods[%s].codeForConnectionWithOtherSystem-Code For Connection With Other System length must be in range: min [%s] and max [%s];".formatted(i, codeForConnectionWithOtherSystemRange.getMinimum(), codeForConnectionWithOtherSystemRange.getMaximum()));
                            }
                        }

                        if (goodsUnitId == null) {
                            addViolation("goodsParameters.goods[%s].goodUnitId-Goods Unit ID must not be null;".formatted(i));
                        }

                        if (price != null) {
                            if (!priceRange.contains(price)) {
                                addViolation("goodsParameters.goods[%s].price-Price must be in range: min [%s] and max [%s];".formatted(i, priceRange.getMinimum(), priceRange.getMaximum()));
                            } else {
                                int scale = price.scale();
                                int precision = price.precision();
                                if ((scale > 2) || (precision > 15)) {
                                    addViolation("goodsParameters.goods[%s].price-Invalid price format;".formatted(i));
                                }
                            }
                        } else {
                            addViolation("goodsParameters.goods[%s].price-Price must not be null;".formatted(i));
                        }

                        if (currencyId == null) {
                            addViolation("goodsParameters.goods[%s].currencyId-Currency ID must not be null;".formatted(i));
                        }

                        if (numberOfIncomingAccount != null) {
                            if (!numberOfIncomingAccountRange.contains(numberOfIncomingAccount.length())) {
                                addViolation("goodsParameters.goods[%s].numberOfIncomingAccount-Number of Incoming Account length must be in range: min [%s] and max [%s];".formatted(i, numberOfIncomingAccountRange.getMinimum(), numberOfIncomingAccountRange.getMaximum()));
                            }
                        }

                        if (costCenterOrControllingOrder != null) {
                            if (!costCenterOrControllingOrderRange.contains(costCenterOrControllingOrder.length())) {
                                addViolation("goodsParameters.goods[%s].costCenterOrControllingOrder-Cost Center Or Controlling Order length must be in range: min [%s] and max [%s];".formatted(i, costCenterOrControllingOrderRange.getMinimum(), costCenterOrControllingOrderRange.getMaximum()));
                            }
                        }
                    } else {
                        if (!quantityRange.contains(quantity)) {
                            addViolation("goodsParameters.goods[%s].quantity-Quantity must be in range: min [%s] and max [%s];".formatted(i, quantityRange.getMinimum(), quantityRange.getMaximum()));
                        }

                        if (!uniqueGoodsDetailIdHolder.add(goodsDetailId)) {
                            addViolation("goodsParameters.goods[%s].goodsDetailId-Duplicated goods detail with id: [%s] found;".formatted(i, goodsDetailId));
                        }
                    }
                }

                if (!isValid) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(violations.toString()).addConstraintViolation();
                }
            }
            return isValid;
        }

        private void checkTableItemsCount(List<GoodsOrderGoodsParametersTableItem> tableItems) {
            if(tableItems.size() > 1000){
                addViolation("goodsParameters.goods- table count should be less or equal to 1000;");
            }
        }

        private void addViolation(String violation) {
            violations.append(violation);
            isValid = false;
        }
    }
}

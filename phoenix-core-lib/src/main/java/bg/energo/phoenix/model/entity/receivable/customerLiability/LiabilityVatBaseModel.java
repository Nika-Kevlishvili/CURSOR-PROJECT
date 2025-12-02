package bg.energo.phoenix.model.entity.receivable.customerLiability;

import java.math.BigDecimal;

public interface LiabilityVatBaseModel {
    Long getCustomerId();

    Long getMainCurrencyId();

    BigDecimal getTotalAmountWithoutVatMainCurrency();

    BigDecimal getTotalAmountWithoutVatAltCurrency();
}

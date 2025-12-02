package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import java.math.BigDecimal;

public interface MLOCustomerResultForLiability {
    Long getDepositId();

    BigDecimal getOffsetAmountInDepositCurrency();

}

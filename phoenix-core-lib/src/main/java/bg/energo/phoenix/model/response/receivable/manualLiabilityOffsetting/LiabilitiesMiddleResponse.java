package bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting;

import java.math.BigDecimal;

public interface LiabilitiesMiddleResponse {

    Long getId();

    String getCurrentAmount();

    Long getCustomerId();

    BigDecimal getAmount();

    Long getCurrencyId();

}

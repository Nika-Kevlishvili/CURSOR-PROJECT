package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import java.math.BigDecimal;

public interface ProcessMiddleResult {
    Long getCustomerId();
    Long getPodId();
    BigDecimal getOverdueAmountForPod();
    BigDecimal getOverdueAmountForContract();
    BigDecimal getOverdueAmountForBillingGroup();
    Long getChangeOfCbgId();

}

package bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import java.math.BigDecimal;

public interface ProcessResultResponse {

    String getCustomerNumber();

    Long getCustomerId();

    Long getPodId();

    String getPodIdentifier();

    BigDecimal getOverdueAmountForPod();

    BigDecimal getOverdueAmountForContract();

    BigDecimal getOverdueAmountForBillingGroup();

    Long getObjectionWithdrawalToCbgId();
}

package bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResultFullResponse {
    private Long processResultId;
    private Long customerId;
    private Long customerNumber;
    private Long podId;
    private String podIdentifier;
    private BigDecimal overdueAmountForContract;
    private BigDecimal overdueAmountForBillingGroup;
    private BigDecimal overdueAmountForPod;
    private Boolean isChecked;
    private Long balancingGroupCoordinatorGroundsId;
    private String balancingGroupCoordinatorGroundsName;
    private Long groundsForObjectionWithdrawalToChangeOfCbgId;
    private String groundsForObjectionWithdrawalToChangeOfCbgName;

}

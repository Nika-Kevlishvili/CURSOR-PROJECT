package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectionToChangeOfCbgProcessResultResponse {
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

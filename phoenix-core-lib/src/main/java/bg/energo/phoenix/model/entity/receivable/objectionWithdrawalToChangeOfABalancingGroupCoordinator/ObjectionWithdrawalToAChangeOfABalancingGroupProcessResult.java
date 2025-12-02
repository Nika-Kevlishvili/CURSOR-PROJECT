package bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ProcessResultResponse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "objection_withdrawal_to_change_of_cbg_process_results", schema = "receivable")

public class ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "objection_withdrawal_to_change_of_cbg_process_results_id_seq",
            schema = "receivable",
            sequenceName = "objection_withdrawal_to_change_of_cbg_process_results_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_withdrawal_to_change_of_cbg_process_results_id_seq"
    )
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "grounds_for_obj_withdrawal_to_change_of_cbg_id")
    private Long groundForObjectionWithdrawalToChangeOfCbgId;

    @Column(name = "overdue_amount_for_contract")
    private BigDecimal overdueAmountForContract;

    @Column(name = "overdue_amount_for_billing_group")
    private BigDecimal overdueAmountForBillingGroup;

    @Column(name = "overdue_amount_for_pod")
    private BigDecimal overdueAmountForPod;

    @Column(name = "balancing_group_coordinator_ground_id")
    private Long balancingGroupCoordinatorGroundId;

    @Column(name = "is_checked")
    private Boolean isChecked;

    @Column(name = "change_withdrawal_of_cbg_id")
    private Long changeWithdrawalOfCbgId;

    public ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult(ProcessResultResponse response,Long withdrawalId,Long balancingGroupCoordinatorGroundId,Long groundForObjectionWithdrawalToChangeOfCbgId) {
        this.customerId= response.getCustomerId();
        this.podId= response.getPodId();
        this.overdueAmountForContract=response.getOverdueAmountForContract();
        this.overdueAmountForBillingGroup=response.getOverdueAmountForBillingGroup();
        this.overdueAmountForPod=response.getOverdueAmountForPod();
        this.isChecked= equalToZero(response.getOverdueAmountForPod());
        this.changeWithdrawalOfCbgId= withdrawalId;
        this.balancingGroupCoordinatorGroundId=balancingGroupCoordinatorGroundId;
        this.groundForObjectionWithdrawalToChangeOfCbgId=groundForObjectionWithdrawalToChangeOfCbgId;

    }

    private boolean equalToZero(BigDecimal number) {
        return number.compareTo(BigDecimal.ZERO)==0;
    }
}

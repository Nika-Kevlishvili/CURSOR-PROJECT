package bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection.ProcessMiddleResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "objection_to_change_of_cbg_process_results", schema = "receivable")

public class ObjectionToChangeOfCbgProcessResult extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "objection_to_change_of_cbg_process_results_id_seq",
            sequenceName = "receivable.objection_to_change_of_cbg_process_results_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_to_change_of_cbg_process_results_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_id")
    private Long customer;

    @Column(name = "pod_id")
    private Long pod;

    @Column(name = "grounds_for_obj_withdrawal_to_change_of_cbg_id")
    private Long groundForObjWithdrawalToChangeOfCbg;

    @Column(name = "overdue_amount_for_contract")
    private BigDecimal overdueAmountForContract;

    @Column(name = "overdue_amount_for_billing_group")
    private BigDecimal overdueAmountForBillingGroup;

    @Column(name = "overdue_amount_for_pod")
    private BigDecimal overdueAmountForPod;

    @Column(name = "balancing_group_coordinator_ground_id")
    private Long balancingGroupCoordinatorGround;

    @Column(name = "is_checked")
    private boolean isChecked;

    @Column(name = "change_of_cbg_id")
    private Long changeOfCbg;

    public ObjectionToChangeOfCbgProcessResult(ProcessMiddleResult result) {
        this.customer = result.getCustomerId();
        this.pod = result.getPodId();
        this.overdueAmountForContract = result.getOverdueAmountForContract();
        this.overdueAmountForBillingGroup = result.getOverdueAmountForBillingGroup();
        this.overdueAmountForPod = result.getOverdueAmountForPod();
        this.changeOfCbg = result.getChangeOfCbgId();
        this.isChecked = result.getOverdueAmountForPod().compareTo(BigDecimal.ZERO) > 0;
    }
}

package bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.response.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationPodRequest;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_dcn_cancellation_pods", schema = "receivable")
public class PowerSupplyDcnCancellationPods extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "power_supply_dcn_cancellation_pods_id_seq",
            sequenceName = "receivable.power_supply_dcn_cancellation_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_dcn_cancellation_pods_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "is_checked")
    private boolean isChecked;

    @Column(name = "cancellation_reason_id")
    private Long cancellationReasonId;

    @Column(name = "power_supply_dcn_cancellation_id")
    private Long powerSupplyDcnCancellationId;


    public PowerSupplyDcnCancellationPods(CancellationPodRequest cancellationPodRequest, Long cancellationId) {
        this.customerId = cancellationPodRequest.getCustomerId();
        this.podId = cancellationPodRequest.getPodId();
        this.isChecked = true;
        this.cancellationReasonId = cancellationPodRequest.getCancellationReasonId();
        this.powerSupplyDcnCancellationId = cancellationId;
    }
}

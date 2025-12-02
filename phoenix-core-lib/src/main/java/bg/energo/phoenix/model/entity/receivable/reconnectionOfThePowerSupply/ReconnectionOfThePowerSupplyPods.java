package bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.request.receivable.reconnectionOfPowerSupply.ReconnectionPodRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "power_supply_reconnection_pods", schema = "receivable")
public class ReconnectionOfThePowerSupplyPods extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "power_supply_reconnection_pods_id_seq",
            sequenceName = "receivable.power_supply_reconnection_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_reconnection_pods_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "power_supply_disconnection_request_id")
    private Long powerSupplyDisconnectionRequestId;

    @Column(name = "cancelation_reason_id")
    private Long cancellationReasonId;

    @Column(name = "power_supply_reconnection_id")
    private Long powerSupplyReconnectionId;

    @Column(name = "reconnection_date")
    private LocalDate reconnectionDate;


    public ReconnectionOfThePowerSupplyPods(ReconnectionPodRequest reconnectionPodRequest, Long reconnectionId) {
        this.customerId = reconnectionPodRequest.getCustomerId();
        this.podId = reconnectionPodRequest.getPodId();
        this.powerSupplyReconnectionId= reconnectionId;
        this.cancellationReasonId = (reconnectionPodRequest.getCancellationReasonId());
        this.powerSupplyDisconnectionRequestId=reconnectionPodRequest.getRequestForDisconnectionOfPowerSupplyId();
    }

}

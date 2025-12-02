package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_disconnection_request_pod_liabilities", schema = "receivable")
public class DisconnectionPowerSupplyRequestsPodLiabilities extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_disconnection_request_pod_liabilities_id_seq",
            schema = "receivable",
            sequenceName = "power_supply_disconnection_request_pod_liabilities_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_request_pod_liabilities_id_seq"
    )
    private Long id;

    @Column(name = "power_supply_disconnection_request_pod_id")
    private Long powerSupplyDisconnectionRequestPodId;

    @Column(name = "customer_liability_id")
    private Long customerLiabilityId;

    @Column(name = "liability_amount")
    private BigDecimal liabilityAmount;

}

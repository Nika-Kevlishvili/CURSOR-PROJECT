package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
@EqualsAndHashCode(callSuper = true)
@Table(name = "power_supply_disconnection_pods", schema = "receivable")
public class DisconnectionOfPowerSupplyPod extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_disconnection_pods_id_seq",
            schema = "receivable",
            sequenceName = "power_supply_disconnection_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_pods_id_seq"
    )
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "power_supply_disconnection_id")
    private Long powerSupplyDisconnectionId;

    @Column(name = "is_checked")
    private boolean isChecked;

    @Column(name = "disconnection_date")
    private LocalDate disconnectionDate;

    @Column(name = "express_reconnection")
    private boolean expressReconnection ;

    @Column(name = "grid_operator_tax_id")
    private Long gridOperatorTaxId;
}

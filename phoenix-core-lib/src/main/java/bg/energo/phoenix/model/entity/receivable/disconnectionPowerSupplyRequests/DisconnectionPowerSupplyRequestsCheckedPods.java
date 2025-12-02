package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_disconnection_request_checked_pods", schema = "receivable")
public class DisconnectionPowerSupplyRequestsCheckedPods extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "power_supply_disconnection_request_checked_pods_id_seq",
            schema = "receivable",
            sequenceName = "power_supply_disconnection_request_checked_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_disconnection_request_checked_pods_id_seq"
    )
    private Long id;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "power_supply_disconnection_request_id")
    private Long powerSupplyDisconnectionRequestId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;

}

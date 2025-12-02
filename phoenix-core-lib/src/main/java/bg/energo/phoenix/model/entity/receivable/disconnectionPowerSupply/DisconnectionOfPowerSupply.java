package bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.disconnectionPowerSupply.PowerSupplyDisconnectionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "power_supply_disconnections", schema = "receivable")
public class DisconnectionOfPowerSupply extends BaseEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "disconnection_number")
    private String disconnectionNumber;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "disconnection_status")
    private PowerSupplyDisconnectionStatus disconnectionStatus;

    @Column(name = "power_supply_disconnection_request_id")
    private Long powerSupplyDisconnectionRequestId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private EntityStatus status;
}

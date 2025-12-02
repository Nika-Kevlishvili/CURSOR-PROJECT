package bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.reconnectionOfThePowerSupply.ReconnectionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "power_supply_reconnections", schema = "receivable")

public class ReconnectionOfThePowerSupply extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "power_supply_reconnections_id_seq",
            sequenceName = "receivable.power_supply_reconnections_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_reconnections_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "reconnection_number")
    private String reconnectionNumber;

    @Column(name = "reconnection_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReconnectionStatus reconnectionStatus;

    @Column(name = "grid_operator_id")
    private Long gridOperatorId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "internal_template_id")
    private Long internalTemplateId;


}

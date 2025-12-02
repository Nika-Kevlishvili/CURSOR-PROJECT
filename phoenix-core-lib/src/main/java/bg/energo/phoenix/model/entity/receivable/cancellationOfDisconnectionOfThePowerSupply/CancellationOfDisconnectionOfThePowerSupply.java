package bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "power_supply_dcn_cancellations", schema = "receivable")

public class CancellationOfDisconnectionOfThePowerSupply extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "power_supply_dcn_cancellations_id_seq",
            sequenceName = "receivable.power_supply_dcn_cancellations_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_dcn_cancellations_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "cancellation_number")
    private String number;

    @Column(name = "power_supply_disconnection_request_id")
    private Long requestForDisconnectionOfThePowerSupplyId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "cancellation_status")
    private CancellationOfDisconnectionOfThePowerSupplyStatus cancellationStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus entityStatus;
}

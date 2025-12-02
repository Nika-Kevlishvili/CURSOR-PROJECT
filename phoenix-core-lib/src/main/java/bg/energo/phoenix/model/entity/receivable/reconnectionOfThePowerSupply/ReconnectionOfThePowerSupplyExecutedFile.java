package bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "power_supply_reconnection_gr_operator_files", schema = "receivable")
@Builder
public class ReconnectionOfThePowerSupplyExecutedFile extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "power_supply_reconnection_gr_operator_files_id_seq",
            sequenceName = "receivable.power_supply_reconnection_gr_operator_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_reconnection_gr_operator_files_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "power_supply_reconnection_id")
    private Long reconnectionId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ReceivableSubObjectStatus status;
}

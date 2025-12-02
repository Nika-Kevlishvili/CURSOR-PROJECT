package bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply;

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
@Table(name = "reconnection_power_supply_documents", schema = "receivable")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ReconnectionOfThePowerSupplyDocuments extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reconnection_power_supply_documents_id_seq")
    @SequenceGenerator(name = "reconnection_power_supply_documents_id_seq", schema = "receivable", sequenceName = "reconnection_power_supply_documents_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "reconnection_power_supply_id")
    private Long reconnectionPowerSupplyId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}

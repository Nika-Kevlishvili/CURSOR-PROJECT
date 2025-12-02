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
@Table(name = "disconnection_power_supply_requests_documents", schema = "receivable")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class DisconnectionPowerSupplyRequestsDocuments extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "disconnection_power_supply_requests_documents_id_seq")
    @SequenceGenerator(name = "disconnection_power_supply_requests_documents_id_seq", schema = "receivable", sequenceName = "disconnection_power_supply_requests_documents_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "disconnection_power_supply_request_id")
    private Long disconnectionPowerSupplyRequestId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}

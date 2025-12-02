package bg.energo.phoenix.model.entity.contract.service;

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
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "contract_internal_intermediaries", schema = "service_contract")
public class ServiceContractInternalIntermediary extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_contract_internal_intermediary_seq")
    @SequenceGenerator(name = "service_contract_internal_intermediary_seq", sequenceName = "contract_internal_intermediaries_id_seq", schema = "service_contract", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_manager_id")
    private Long internalIntermediaryId;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}

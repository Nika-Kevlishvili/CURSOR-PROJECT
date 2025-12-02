package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
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
@Table(schema = "service_contract", name = "contract_related_service_contracts")
public class ServiceContractRelatedServiceContract  extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "service_contract_related_service_contracts_seq",
            sequenceName = "service_contract.contract_related_service_contracts_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_contract_related_service_contracts_seq"
    )
    private Long id;

    @Column(name = "contract_id")
    private Long serviceContractId;

    @Column(name = "related_contract_id")
    private Long relatedServiceContractId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

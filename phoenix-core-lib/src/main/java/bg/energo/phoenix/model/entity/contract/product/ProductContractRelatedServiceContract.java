package bg.energo.phoenix.model.entity.contract.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contract_related_service_contracts", schema = "product_contract")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ProductContractRelatedServiceContract extends BaseEntity {

    @Id
    @SequenceGenerator(name = "product_contract_related_service_contracts_id_seq", sequenceName = "product_contract.contract_related_service_contracts_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_contract_related_service_contracts_id_seq")
    private Long id;

    @Column(name = "contract_id")
    private Long productContractId;

    @Column(name = "related_service_contract_id")
    private Long serviceContractId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

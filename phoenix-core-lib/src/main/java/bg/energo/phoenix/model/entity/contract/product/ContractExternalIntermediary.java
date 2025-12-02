package bg.energo.phoenix.model.entity.contract.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contract_external_intermediaries", schema = "product_contract")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ContractExternalIntermediary extends BaseEntity {

    @Id
    @SequenceGenerator(name = "contract_external_intermediaries_id_seq", sequenceName = "product_contract.contract_external_intermediaries_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_external_intermediaries_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "external_intermediary_id")
    private Long externalIntermediaryId;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;


}

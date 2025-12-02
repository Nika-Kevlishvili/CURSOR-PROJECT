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
@Table(name = "contract_internal_intermediaries", schema = "product_contract")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ContractInternalIntermediary extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "contract_internal_intermediaries_id_seq",
            sequenceName = "product_contract.contract_internal_intermediaries_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contract_internal_intermediaries_id_seq"
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "account_manager_id")
    private Long accountManagerId;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;

}

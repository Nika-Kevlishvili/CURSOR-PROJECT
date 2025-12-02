package bg.energo.phoenix.model.entity.contract.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contract_version_types", schema = "product_contract")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductContractVersionTypes extends BaseEntity {

    @Id
    @SequenceGenerator(name = "contract_version_types_id_seq", sequenceName = "product_contract.contract_version_types_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_version_types_id_seq")
    private Long id;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;
    @Column(name = "contract_version_type_id")
    private Long contractVersionTypeId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;

}

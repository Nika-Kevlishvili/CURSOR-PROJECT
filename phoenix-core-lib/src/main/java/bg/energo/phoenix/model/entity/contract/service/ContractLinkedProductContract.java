package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
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
@Table(schema = "service_contract", name = "contract_linked_product_contracts")
public class ContractLinkedProductContract extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "contract_linked_product_contracts_seq",
            sequenceName = "service_contract.contract_linked_product_contracts_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "contract_linked_product_contracts_seq"
    )
    private Long id;

    @Column(name = "contractId")
    private Long contractId;

    @Column(name = "linked_product_contract_id")
    private Long linkedProductContractId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;
}

package bg.energo.phoenix.model.entity.contract.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(schema = "product_contract", name = "contract_resigned_contracts")
public class ProductContractResignedContracts extends BaseEntity {
    @Id
    @SequenceGenerator(name = "related_contracts_seq", schema = "product_contract", sequenceName = "related_contracts_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "related_contracts_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "resigned_contract_id")
    private Long resignedContractId;
}

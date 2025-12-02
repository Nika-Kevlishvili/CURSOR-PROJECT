package bg.energo.phoenix.model.entity.contract.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "contract_product_additional_params", schema = "product_contract")
public class ProductContractAdditionalParams extends BaseEntity {
    @Id
    @SequenceGenerator(name = "contract_additional_params_id_seq", sequenceName = "product_contract.contract_additional_params_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_additional_params_id_seq")
    private Long id;

    @Column(name = "label")
    private String label;

    @Column(name = "value")
    private String value;

    @Column(name = "product_additional_param_id")
    private Long productAdditionalParamId;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

}

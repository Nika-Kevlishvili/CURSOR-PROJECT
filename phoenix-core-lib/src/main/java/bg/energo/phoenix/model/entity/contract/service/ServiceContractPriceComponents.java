package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "contract_price_components", schema = "service_contract")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServiceContractPriceComponents extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "service_contract_price_components_id_seq",
            sequenceName = "service_contract.contract_price_components_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_contract_price_components_id_seq"
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "price_component_formula_variable_id")
    private Long priceComponentFormulaVariableId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;
}

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

import java.math.BigDecimal;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "contract_iap_price_components", schema = "service_contract")
public class ServiceContractInterimPriceFormula extends BaseEntity {
    @Id
    @SequenceGenerator(name = "service_contract_iap_price_components_id_seq", sequenceName = "service_contract.contract_iap_price_components_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_contract_iap_price_components_id_seq")
    private Long id;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "price_component_formula_variable_id")
    private Long formulaId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "contract_interim_advance_payment_id")
    private Long contractInterimAdvancePaymentId;

    public ServiceContractInterimPriceFormula(BigDecimal value, Long formulaId, Long contractInterimAdvancePaymentId) {
        this.value = value;
        this.formulaId = formulaId;
        this.contractInterimAdvancePaymentId = contractInterimAdvancePaymentId;
        this.status = EntityStatus.ACTIVE;
    }
}

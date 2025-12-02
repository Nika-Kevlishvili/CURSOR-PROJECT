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
@Table(name = "contract_interim_advance_payments", schema = "service_contract")

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServiceContractInterimAdvancePayments extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "service_contract_interim_advance_payments_seq",
            sequenceName = "service_contract.contract_interim_advance_payments_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_contract_interim_advance_payments_seq"
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "issue_day")
    private Integer issueDate;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "interim_advance_payment_id")
    private Long interimAdvancePaymentId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "term_value")
    private Integer termValue;
}

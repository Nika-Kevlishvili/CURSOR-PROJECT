package bg.energo.phoenix.model.entity.receivable.manualLiabilityOffsetting;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mlo_customer_deposits", schema = "receivable")
public class MLOCustomerDeposits extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "mlo_customer_deposits_id_seq",
            schema = "receivable",
            sequenceName = "mlo_customer_deposits_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "mlo_customer_deposits_id_seq"
    )
    private Long id;

    @Column(name = "manual_liabilitie_offsetting_id")
    private Long manualLiabilityOffsettingId;

    @Column(name = "customer_deposit_id")
    private Long customerDepositId;

    @Column(name = "before_current_amount")
    private BigDecimal beforeCurrentAmount;

    @Column(name = "after_current_amount")
    private BigDecimal afterCurrentAmount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "liability_id")
    private Long liabilityId;

    @Column(name = "reversal_receivable_id")
    private Long reversalReceivableId;

    public MLOCustomerDeposits(Long customerDepositId, Long liabilityOffsettingId) {
        this.customerDepositId = customerDepositId;
        this.manualLiabilityOffsettingId = liabilityOffsettingId;
    }

}

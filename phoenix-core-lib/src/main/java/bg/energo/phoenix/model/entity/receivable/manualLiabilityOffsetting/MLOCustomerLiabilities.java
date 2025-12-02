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
@Table(name = "mlo_customer_liabilities", schema = "receivable")
public class MLOCustomerLiabilities extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "mlo_customer_liabilities_id_seq",
            schema = "receivable",
            sequenceName = "mlo_customer_liabilities_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "mlo_customer_liabilities_id_seq"
    )
    private Long id;

    @Column(name = "manual_liabilitie_offsetting_id")
    private Long manualLiabilityOffsettingId;

    @Column(name = "customer_liabilitie_id")
    private Long customerLiabilitiesId;

    @Column(name = "before_current_amount")
    private BigDecimal beforeCurrentAmount;

    @Column(name = "after_current_amount")
    private BigDecimal afterCurrentAmount;

    @Column(name = "currency_id")
    private Long currencyId;

    public MLOCustomerLiabilities(Long customerLiabilitiesId, Long liabilityOffsettingId) {
        this.customerLiabilitiesId = customerLiabilitiesId;
        this.manualLiabilityOffsettingId = liabilityOffsettingId;
    }

}

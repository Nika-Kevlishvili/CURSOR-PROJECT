package bg.energo.phoenix.model.entity.receivable.customerLiability;

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
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_liabilitie_paid_by_rescheduling", schema = "receivable")
public class CustomerLiabilityPaidByRescheduling extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_liabilitie_paid_by_receivables_id_seq",
            sequenceName = "receivable.customer_liabilitie_paid_by_receivables_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_liabilitie_paid_by_receivables_id_seq"
    )
    private Long id;

    @Column(name = "customer_liabilitie_id")
    private Long customerLiabilityId;

    @Column(name = "customer_rescheduling_id")
    private Long customerReschedulingId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

package bg.energo.phoenix.model.entity.receivable.customerLiability;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.OperationContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_liabilitie_paid_by_payments", schema = "receivable")
public class CustomerLiabilityPaidByPayment extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_liabilitie_paid_by_payments_id_seq",
            sequenceName = "receivable.customer_liabilitie_paid_by_payments_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_liabilitie_paid_by_payments_id_seq"
    )
    private Long id;

    @Column(name = "customer_liabilitie_id")
    private Long customerLiabilityId;

    @Column(name = "customer_payment_id")
    private Long customerPaymentId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency_id")
    private Long currencyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "operation_date")
    private LocalDateTime operationDate;

    @Column(name = "operation_context")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private OperationContext operationContext;

}

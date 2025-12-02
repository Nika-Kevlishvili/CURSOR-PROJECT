package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "billing_contract_billing_groups", schema = "billing")

public class BillingRunBillingGroup extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billings_billing_group_id_seq",
            sequenceName = "billing.billing_billing_groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billings_billing_group_id_seq"
    )
    private Long id;

    @Column(name = "contract_billing_group_id")
    private Long billingGroupId;

    @Column(name = "billing_id")
    private Long billingRunId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    public BillingRunBillingGroup(BillingRunBillingGroup x, Long billingRunId) {

        this.billingGroupId = x.getBillingGroupId();
        this.billingRunId = billingRunId;
        this.status = x.getStatus();

    }
}

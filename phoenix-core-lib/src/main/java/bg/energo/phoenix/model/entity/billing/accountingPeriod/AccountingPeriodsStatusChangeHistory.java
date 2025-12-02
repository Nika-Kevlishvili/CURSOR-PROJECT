package bg.energo.phoenix.model.entity.billing.accountingPeriod;

import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account_period_status_change_hist", schema = "billing")
public class AccountingPeriodsStatusChangeHistory {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_account_period_status_change_hist_id_seq",
            schema = "billing",
            sequenceName = "account_period_status_change_hist_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_account_period_status_change_hist_id_seq"
    )
    private Long id;

    @Column(name = "account_period_id")
    private Long accountingPeriodId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountingPeriodStatus status;

    @Column(name = "create_date")
    private LocalDateTime createDate;

}

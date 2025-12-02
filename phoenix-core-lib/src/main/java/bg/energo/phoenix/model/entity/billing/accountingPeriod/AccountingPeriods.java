package bg.energo.phoenix.model.entity.billing.accountingPeriod;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountPeriodFileGenerationStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "account_periods", schema = "billing")
public class AccountingPeriods extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_account_periods_id_seq",
            schema = "billing",
            sequenceName = "account_periods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_account_periods_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountingPeriodStatus status;

    @Column(name = "file_generation_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountPeriodFileGenerationStatus fileGenerationStatus;

    @Column(name = "modify_date")
    private LocalDateTime modifyDate;
}

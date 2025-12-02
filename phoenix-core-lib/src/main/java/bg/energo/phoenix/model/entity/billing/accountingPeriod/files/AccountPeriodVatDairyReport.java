package bg.energo.phoenix.model.entity.billing.accountingPeriod.files;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "account_period_vat_dairy_report", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountPeriodVatDairyReport extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "account_period_vat_dairy_report_id_seq",
            schema = "billing",
            sequenceName = "account_period_vat_dairy_report_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "account_period_vat_dairy_report_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "account_period_id")
    private Long accountPeriodId;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}

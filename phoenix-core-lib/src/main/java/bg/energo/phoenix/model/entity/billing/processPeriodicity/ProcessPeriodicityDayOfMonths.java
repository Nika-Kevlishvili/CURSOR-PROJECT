package bg.energo.phoenix.model.entity.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Month;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.MonthNumber;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "process_periodicity_day_of_months", schema = "billing")
@Entity

public class ProcessPeriodicityDayOfMonths extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "process_periodicity_day_of_months_id_seq",
            sequenceName = "billing.process_periodicity_day_of_months_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_periodicity_day_of_months_id_seq"
    )
    private Long id;

    @Column(name = "process_periodicity_id")
    private Long processPeriodicityId;

    @Column(name = "month")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Month month;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "billing.billing_month_number"
            )
    )
    @Column(name = "month_number", columnDefinition = "billing.billing_month_number[]")
    private List<MonthNumber> monthNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

package bg.energo.phoenix.model.entity.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.WeekTemporary;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.Day;
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
@Table(name = "process_periodicity_period_of_year", schema = "billing")
@Entity

public class ProcessPeriodicityPeriodOfYear extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "process_periodicity_period_of_year_id_seq",
            sequenceName = "billing.process_periodicity_period_of_year_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_periodicity_period_of_year_id_seq"
    )
    private Long id;

    @Column(name = "process_periodicity_id")
    private Long processPeriodicityId;

    @Column(name = "week")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private WeekTemporary week;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "billing.process_periodicity_day"
            )
    )
    @Column(name = "day", columnDefinition = "billing.process_periodicity_day[]")
    private List<Day> days;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}

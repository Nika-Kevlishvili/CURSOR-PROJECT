package bg.energo.phoenix.model.entity.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.*;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "process_periodicity", schema = "billing")
@Entity
public class ProcessPeriodicity extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "process_periodicity_id_seq",
            sequenceName = "billing.process_periodicity_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_periodicity_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type")
    private ProcessPeriodicityType processPeriodicityType;

    @Column(name = "start_after_process_billing_id")
    private Long startAfterProcessBillingId;

    @Column(name = "ignore_at_runtime", columnDefinition = "billing.process_periodicity_ignore_at_runtime[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "billing.process_periodicity_ignore_at_runtime"
            )
    )
    private List<ProcessPeriodicityIgnoreAtRuntime> ignoreAtRuntime;

    @Column(name = "excludes", columnDefinition = "billing.process_periodicity_exclude[]")
    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "billing.process_periodicity_exclude"
            )
    )
    private List<ProcessPeriodicityExclude> processPeriodicityExcludes;

    @Column(name = "calendar_id")
    private Long calendarId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "change_to")
    private ProcessPeriodicityChangeTo processPeriodicityChangeTo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "period_type")
    private ProcessPeriodicityPeriodType processPeriodicityPeriodType;

    @Column(name = "rrule_formula")
    private String rruleFormula;

    @Column(name = "year_round")
    private Boolean yearRound;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "billing_process_start")
    private ProcessPeriodicityBillingProcessStart processPeriodicityBillingProcessStart;

    @Column(name = "billing_process_start_date")
    private LocalDateTime billingProcessStartDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
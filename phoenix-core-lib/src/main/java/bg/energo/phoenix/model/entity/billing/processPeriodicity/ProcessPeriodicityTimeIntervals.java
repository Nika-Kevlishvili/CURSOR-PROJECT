package bg.energo.phoenix.model.entity.billing.processPeriodicity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "process_periodicity_time_intervals", schema = "billing")
@Entity
public class ProcessPeriodicityTimeIntervals extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "process_periodicity_start_time_intervals_id_seq",
            sequenceName = "billing.process_periodicity_start_time_intervals_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_periodicity_start_time_intervals_id_seq"
    )
    private Long id;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "process_periodicity_id")
    private Long processPeriodicityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}

package bg.energo.phoenix.model.entity.pod.meter;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.pod.meter.MeterStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "meters", schema = "pod")
public class Meter extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "meters_id_seq",
            sequenceName = "pod.meters_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "meters_id_seq"
    )
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "grid_operator_id")
    private Long gridOperatorId;

    @Column(name = "installment_date")
    private LocalDate installmentDate;

    @Column(name = "remove_date")
    private LocalDate removeDate;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private MeterStatus status;

}

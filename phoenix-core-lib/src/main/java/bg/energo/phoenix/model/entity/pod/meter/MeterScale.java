package bg.energo.phoenix.model.entity.pod.meter;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.pod.PodSubObjectStatus;
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

@Table(name = "meter_scales", schema = "pod")
public class MeterScale extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "meter_scales_id_seq",
            sequenceName = "pod.meter_scales_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "meter_scales_id_seq"
    )
    private Long id;

    @Column(name = "meter_id")
    private Long meterId;

    @Column(name = "scale_id")
    private Long scaleId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PodSubObjectStatus status;

}

package bg.energo.phoenix.model.entity.pod.pod;

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
@Table(name = "pod_details_additional_params", schema = "pod")
public class PointOfDeliveryDetailsAdditionalParameters extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "pod_details_additional_params_id_seq",
            sequenceName = "pod.pod_details_additional_params_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "pod_details_additional_params_id_seq"
    )
    private Long id;

    @Column(name = "pod_detail_id")
    private Long podDetailId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "pod_additional_param")
    private Long podAdditionalParamId;

}

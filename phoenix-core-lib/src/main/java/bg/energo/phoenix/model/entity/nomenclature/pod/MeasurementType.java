package bg.energo.phoenix.model.entity.nomenclature.pod;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.pod.MeasurementTypeRequest;
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
@Table(name = "pod_measurement_types", schema = "nomenclature")
public class MeasurementType extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "nomenclature_pod_measurement_types_id_seq",
            schema = "nomenclature",
            sequenceName = "pod_measurement_types_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "nomenclature_pod_measurement_types_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "grid_operator_id")
    private Long gridOperatorId;

    public MeasurementType(MeasurementTypeRequest measurementTypeRequest) {
        this.name = measurementTypeRequest.getName().trim();
        this.status = measurementTypeRequest.getStatus();
        this.isDefault = measurementTypeRequest.getDefaultSelection();
    }
}

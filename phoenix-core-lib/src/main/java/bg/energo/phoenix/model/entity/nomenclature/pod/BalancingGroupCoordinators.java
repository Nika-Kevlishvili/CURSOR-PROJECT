package bg.energo.phoenix.model.entity.nomenclature.pod;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.pod.BalancingGroupCoordinatorsRequest;
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

@Table(name = "balancing_group_coordinators", schema = "nomenclature")
public class BalancingGroupCoordinators extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "balancing_group_coordinators_seq",
            sequenceName = "nomenclature.balancing_group_coordinators_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "balancing_group_coordinators_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    public BalancingGroupCoordinators(BalancingGroupCoordinatorsRequest request) {
        this.name = request.getName().trim();
        this.fullName = request.getFullName().trim();
        this.status = request.getStatus();
        this.defaultSelection = request.getDefaultSelection();
    }
}

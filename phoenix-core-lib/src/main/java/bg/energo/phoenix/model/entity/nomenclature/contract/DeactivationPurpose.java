package bg.energo.phoenix.model.entity.nomenclature.contract;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.contract.DeactivationPurposeRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "nomenclature", name = "deactivation_purposes")
public class DeactivationPurpose extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "deactivation_purposes_seq",
            sequenceName = "nomenclature.deactivation_purposes_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "deactivation_purposes_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_hard_coded")
    private Boolean isHardCoded;

    public DeactivationPurpose(DeactivationPurposeRequest request){
        this.name = request.getName();
        this.isDefault = request.getDefaultSelection();
        this.status = request.getStatus();
    }
}

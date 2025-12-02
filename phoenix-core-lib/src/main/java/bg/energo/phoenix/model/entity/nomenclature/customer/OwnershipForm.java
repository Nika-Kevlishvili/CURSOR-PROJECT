package bg.energo.phoenix.model.entity.nomenclature.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.OwnershipFormRequest;
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

@Table(name = "ownership_forms", schema = "nomenclature")
public class OwnershipForm extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "ownership_forms_seq",
            sequenceName = "nomenclature.ownership_forms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "ownership_forms_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public OwnershipForm(OwnershipFormRequest request) {
        this.name = request.getName().trim();
        this.defaultSelection = request.getDefaultSelection();
        this.status = request.getStatus();
    }
}

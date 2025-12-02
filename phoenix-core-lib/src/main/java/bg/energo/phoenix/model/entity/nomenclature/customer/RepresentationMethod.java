package bg.energo.phoenix.model.entity.nomenclature.customer;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.customer.RepresentationMethodRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "representation_methods", schema = "nomenclature")
public class RepresentationMethod extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "representation_methods_seq",
            sequenceName = "nomenclature.representation_methods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "representation_methods_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private NomenclatureItemStatus status;


    public RepresentationMethod(RepresentationMethodRequest request) {
        this.name = request.getName().trim();
        this.status = request.getStatus();
        this.defaultSelection = request.getDefaultSelection();
    }
}

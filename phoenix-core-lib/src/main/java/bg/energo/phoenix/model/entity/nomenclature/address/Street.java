package bg.energo.phoenix.model.entity.nomenclature.address;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.StreetType;
import bg.energo.phoenix.model.request.nomenclature.address.StreetsRequest;
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

@Table(name = "streets", schema = "nomenclature")
public class Street extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "streets_id_seq",
            sequenceName = "nomenclature.streets_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "streets_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StreetType type;

    @Column(name = "is_default")
    private Boolean defaultSelection;

    @Column(name = "ordering_id")
    private Long orderingId;

    @ManyToOne
    @JoinColumn(name = "populated_place_id")
    private PopulatedPlace populatedPlace;


    public Street(StreetsRequest request) {
        this.name = request.getName().trim();
        this.status = request.getStatus();
        this.type = request.getType();
        this.defaultSelection = request.getDefaultSelection();
    }
}

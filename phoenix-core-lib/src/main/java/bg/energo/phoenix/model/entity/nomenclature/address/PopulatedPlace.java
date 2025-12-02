package bg.energo.phoenix.model.entity.nomenclature.address;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.address.PopulatedPlaceRequest;
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

@Table(name = "populated_places", schema = "nomenclature")
public class PopulatedPlace extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "populated_places_seq",
            sequenceName = "nomenclature.populated_places_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "populated_places_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipality_id", nullable = false)
    private Municipality municipality;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "ordering_id")
    private Long orderingId;

    public PopulatedPlace(PopulatedPlaceRequest request) {
        this.name = request.getName().trim();
        this.status = request.getStatus();
        this.defaultSelection = request.getDefaultSelection();
    }
}

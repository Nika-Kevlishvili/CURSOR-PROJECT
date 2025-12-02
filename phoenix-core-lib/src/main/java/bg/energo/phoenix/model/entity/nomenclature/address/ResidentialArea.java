package bg.energo.phoenix.model.entity.nomenclature.address;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.nomenclature.ResidentialAreaType;
import bg.energo.phoenix.model.request.nomenclature.address.ResidentialAreaRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "residential_areas", schema = "nomenclature")
public class ResidentialArea extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "residential_areas_id_seq",
            sequenceName = "nomenclature.residential_areas_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "residential_areas_id_seq"
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
    private Boolean defaultSelection;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ResidentialAreaType type;

    @ManyToOne
    @JoinColumn(name = "populated_place_id")
    private PopulatedPlace populatedPlace;


    public ResidentialArea(ResidentialAreaRequest request) {
        this.name = request.getName().trim();
        this.status = request.getStatus();
        this.defaultSelection = request.getDefaultSelection();
        this.type = request.getType();
    }

}

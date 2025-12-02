package phoenix.core.customer.model.entity.nomenclature.address;


import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.enums.nomenclature.ResidentialAreaType;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "residential_areas", schema = "nomenclature")
@Builder
public class ResidentialArea {
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
    @Type(type = "pgsql_enum")
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private Boolean defaultSelection;

    @Column(name = "system_user_id")
    private String systemUserId;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private ResidentialAreaType type;

    @ManyToOne
    @JoinColumn(name = "populated_place_id")
    private PopulatedPlace populatedPlace;


}

package phoenix.core.customer.model.entity.nomenclature.address;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.enums.nomenclature.StreetType;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "streets", schema = "nomenclature")
@Builder
public class Street {

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
    @Type(type = "pgsql_enum")
    private NomenclatureItemStatus status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private StreetType type;

    @Column(name = "is_default")
    private Boolean defaultSelection;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "system_user_id")
    private String systemUserId;

    @ManyToOne
    @JoinColumn(name = "populated_place_id")
    private PopulatedPlace populatedPlace;


}

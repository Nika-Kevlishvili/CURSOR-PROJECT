package bg.energo.phoenix.model.entity.nomenclature.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "sales_areas", schema = "nomenclature")
public class SalesArea extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "sales_areas_id_seq",
            sequenceName = "nomenclature.sales_areas_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sales_areas_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "login_portal_tag")
    private String loginPortalTag;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    @Column(name = "ordering_id")
    private Long orderingId;
}

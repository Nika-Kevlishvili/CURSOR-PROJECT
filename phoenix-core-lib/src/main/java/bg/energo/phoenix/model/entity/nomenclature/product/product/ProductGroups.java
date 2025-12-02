package bg.energo.phoenix.model.entity.nomenclature.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.product.ProductGroupsRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "nomenclature", name = "product_groups")
public class ProductGroups extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "product_groups_seq",
            sequenceName = "nomenclature.product_groups_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "product_groups_seq"
    )
    private Long id;

    @Nationalized
    @Column(name = "name")
    private String name;

    @Column(name = "name_transl")
    private String nameTransliterated;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public ProductGroups(ProductGroupsRequest productGroupsRequest) {
        this.name = productGroupsRequest.getName();
        this.nameTransliterated = productGroupsRequest.getNameTransliterated();
        this.status = productGroupsRequest.getStatus();
        this.defaultSelection = productGroupsRequest.getDefaultSelection();
    }
}

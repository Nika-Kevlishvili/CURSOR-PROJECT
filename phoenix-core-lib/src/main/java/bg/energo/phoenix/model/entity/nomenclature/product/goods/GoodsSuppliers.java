package bg.energo.phoenix.model.entity.nomenclature.product.goods;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.goods.GoodsSuppliersRequest;
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
@Table(schema = "nomenclature", name = "goods_suppliers")
public class GoodsSuppliers extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "goods_suppliers_seq",
            sequenceName = "nomenclature.goods_suppliers_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "goods_suppliers_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "is_default")
    private boolean defaultSelection;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public GoodsSuppliers(GoodsSuppliersRequest goodsSuppliersRequest) {
        this.name = goodsSuppliersRequest.getName().trim();
        this.identifier = goodsSuppliersRequest.getIdentifier();
        this.status = goodsSuppliersRequest.getStatus();
        this.defaultSelection = goodsSuppliersRequest.getDefaultSelection();
    }
}

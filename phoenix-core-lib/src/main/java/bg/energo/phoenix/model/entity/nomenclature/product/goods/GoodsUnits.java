package bg.energo.phoenix.model.entity.nomenclature.product.goods;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.goods.GoodsUnitsRequest;
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

@Table(name = "goods_units", schema = "nomenclature")
public class GoodsUnits extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "goods_units_id_seq",
            sequenceName = "nomenclature.goods_units_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "goods_units_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public GoodsUnits(GoodsUnitsRequest request){
        this.name = request.getName();
        this.isDefault = request.getDefaultSelection();
        this.status = request.getStatus();
    }

}

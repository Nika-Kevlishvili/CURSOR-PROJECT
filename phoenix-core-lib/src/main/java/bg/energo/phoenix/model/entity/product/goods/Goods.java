package bg.energo.phoenix.model.entity.product.goods;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.goods.GoodsStatus;
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

@Table(name = "goods", schema = "goods")
public class Goods extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "goods_id_seq",
            sequenceName = "goods.goods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "goods_id_seq"
    )
    private Long id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GoodsStatus goodsStatusEnum;

    @Column(name = "last_goods_details_id")
    private Long lastGoodsDetailsId;
}


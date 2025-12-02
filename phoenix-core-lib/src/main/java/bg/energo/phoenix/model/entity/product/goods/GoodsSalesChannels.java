package bg.energo.phoenix.model.entity.product.goods;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.SalesChannel;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
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

@Table(name = "goods_sales_channels", schema = "goods")
public class GoodsSalesChannels extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "goods_sales_channels_id_seq", sequenceName = "goods.goods_sales_channels_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "goods_sales_channels_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_details_id", nullable = false)
    private GoodsDetails goodsDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_channels_id", nullable = false)
    private SalesChannel salesChannel;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GoodsSubObjectStatus status;
}

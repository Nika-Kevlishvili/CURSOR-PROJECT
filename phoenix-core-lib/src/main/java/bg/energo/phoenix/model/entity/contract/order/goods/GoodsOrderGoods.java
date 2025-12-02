package bg.energo.phoenix.model.entity.contract.order.goods;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(schema = "goods_order", name = "order_goods")
public class GoodsOrderGoods extends BaseEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "goods_order_goods_id_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "goods_order_goods_id_seq", schema = "goods_order", sequenceName = "order_goods_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "other_system_connection_code")
    private String otherSystemConnectionCode;

    @Column(name = "goods_units_id")
    private Long goodsUnitsId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "income_account_numbers")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "goods_details_id")
    private Long goodsDetailsId;

    @Column(name = "order_id")
    private Long orderId;
}

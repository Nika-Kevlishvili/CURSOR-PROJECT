package bg.energo.phoenix.model.entity.contract.order.goods;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
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
@Table(name = "order_external_intermediaries", schema = "goods_order")
public class GoodsOrderExternalIntermediary extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "goods_order_external_intermediaries_id_seq",
            sequenceName = "order_external_intermediaries_id_seq",
            schema = "goods_order",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "goods_order_external_intermediaries_id_seq"
    )
    private Long id;

    @Column(name = "external_intermediary_id")
    private Long externalIntermediaryId;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

package bg.energo.phoenix.model.entity.contract.order.goods;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermDueDateChange;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermExcludes;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderPaymentTermType;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(schema = "goods_order", name = "order_payment_terms")
public class GoodsOrderPaymentTerm extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "goods_order_payment_term_seq")
    @SequenceGenerator(name = "goods_order_payment_term_seq", schema = "goods_order", sequenceName = "order_payment_terms_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GoodsOrderPaymentTermType type;

    @Column(name = "value")
    private Integer value;

    @Column(name = "calendar_id")
    private Long calendarId;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "goods_order.payment_term_exclude"
            )
    )
    @Column(name = "excludes", columnDefinition = "goods_order.payment_term_exclude[]")
    private List<GoodsOrderPaymentTermExcludes> excludes;

    @Column(name = "due_date_change")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GoodsOrderPaymentTermDueDateChange dueDateChange;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;
}
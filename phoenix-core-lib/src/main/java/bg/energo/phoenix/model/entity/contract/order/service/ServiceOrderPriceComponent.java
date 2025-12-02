package bg.energo.phoenix.model.entity.contract.order.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "order_price_components", schema = "service_order")
public class ServiceOrderPriceComponent extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "order_price_components_id_seq",
            sequenceName = "order_price_components_id_seq",
            schema = "service_order",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_price_components_id_seq"
    )
    private Long id;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "price_component_formula_variable_id")
    private Long priceComponentFormulaVariableId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "order_id")
    private Long orderId;

}

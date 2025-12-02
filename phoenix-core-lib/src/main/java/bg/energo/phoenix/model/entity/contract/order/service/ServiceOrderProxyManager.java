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

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "order_proxy_managers", schema = "service_order")
public class ServiceOrderProxyManager extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "order_proxy_managers_id_seq",
            sequenceName = "order_proxy_managers_id_seq",
            schema = "service_order",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_proxy_managers_id_seq"
    )
    private Long id;

    @Column(name = "order_proxy_id")
    private Long orderProxyId;

    @Column(name = "customer_manager_id")
    private Long customerManagerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

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

@Table(name = "order_tasks", schema = "service_order")
public class ServiceOrderTask extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "order_tasks_id_seq",
            sequenceName = "order_tasks_id_seq",
            schema = "service_order",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_tasks_id_seq"
    )
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

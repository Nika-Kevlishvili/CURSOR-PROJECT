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

@Table(name = "order_assisting_employees", schema = "service_order")
public class ServiceOrderAssistingEmployee extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "service_order_assisting_employees_id_seq",
            sequenceName = "order_assisting_employees_id_seq",
            schema = "service_order",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_order_assisting_employees_id_seq"
    )
    private Long id;

    @Column(name = "account_manager_id")
    private Long accountManagerId;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

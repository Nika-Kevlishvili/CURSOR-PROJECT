package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customer_connected_groups", schema = "customer")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerConnectedGroup extends BaseEntity {

    @Id
    @SequenceGenerator(name = "customer_connected_groups_id_seq", sequenceName = "customer.customer_connected_groups_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_connected_groups_id_seq")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "connected_group_id", nullable = false)
    private Long connectedGroupId;

    public CustomerConnectedGroup(Long customerId, Status status, Long connectedGroupId) {
        this.customerId = customerId;
        this.status = status;
        this.connectedGroupId = connectedGroupId;
    }
}


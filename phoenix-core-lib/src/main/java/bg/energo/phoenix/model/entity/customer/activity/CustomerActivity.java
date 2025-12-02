package bg.energo.phoenix.model.entity.customer.activity;

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

@Table(name = "customer_activity", schema = "customer")
public class CustomerActivity extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "customer_activity_id_seq",
            sequenceName = "customer.customer_activity_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_activity_id_seq"
    )
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "activity_id")
    private Long systemActivityId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}

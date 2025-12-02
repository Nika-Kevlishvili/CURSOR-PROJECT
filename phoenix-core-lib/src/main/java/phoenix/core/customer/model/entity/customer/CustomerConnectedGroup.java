package phoenix.core.customer.model.entity.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@Entity
@Table(name = "customer_connencted_groups", schema = "customer")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Data
public class CustomerConnectedGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "status", nullable = false)
    @Type(type = "pgsql_enum")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "connected_group_id", nullable = false)
    private Long connectedGroupId;

}


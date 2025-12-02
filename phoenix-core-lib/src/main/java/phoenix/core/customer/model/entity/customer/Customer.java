package phoenix.core.customer.model.entity.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.enums.customer.CustomerStatus;
import phoenix.core.customer.model.enums.customer.CustomerType;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "customers", schema = "customer")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Data
@EqualsAndHashCode(callSuper = true)
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_number", nullable = false)
    private Long customerNumber;

    @Column(name = "identifier", nullable = false, length = 13)
    private String identifier;

    @Column(name = "customer_type", nullable = false)
    @Type(type = "pgsql_enum")
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    @OneToMany(mappedBy = "customer")
    private List<CustomerOwner> customerOwners;

    @Column(name = "last_customer_detail_id")
    private Long lastCustomerDetailId;

    @Column(name = "status", nullable = false)
    @Type(type = "pgsql_enum")
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;
}

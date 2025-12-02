package phoenix.core.customer.model.entity.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.entity.nomenclature.customer.CiConnectionType;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "related_customers", schema = "customer")
public class RelatedCustomer extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "related_customers_id_seq",
            sequenceName = "customer.related_customers_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "related_customers_id_seq"
    )
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "related_customer_id")
    private Long relatedCustomerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_types_rci")
    private CiConnectionType ciConnectionType;

}

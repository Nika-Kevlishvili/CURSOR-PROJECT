package phoenix.core.customer.model.entity.customer;


import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.entity.nomenclature.customer.BelongingCapitalOwner;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Table(schema = "customer",name ="customer_owners" )
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
public class CustomerOwner extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "customer_owners_id_seq", sequenceName = "customer.customer_owners_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_owners_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "belonging_capital_owner_id")
    private BelongingCapitalOwner belongingCapitalOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_customer_id")
    private Customer ownerCustomer;

    @Column(name = "additional_info", length = 2048)
    private String additionalInfo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private Status status;

}

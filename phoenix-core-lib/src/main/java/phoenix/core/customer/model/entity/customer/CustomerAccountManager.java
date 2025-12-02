package phoenix.core.customer.model.entity.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.entity.nomenclature.customer.AccountManagerType;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@Entity
@Table(name = "customer_account_managers", schema = "customer")
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerAccountManager extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    @SequenceGenerator(
            name = "customer_account_managers_id_seq",
            sequenceName = "customer.customer_account_managers_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_account_managers_id_seq"
    )
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_detail_id", nullable = false)
    private CustomerDetails customerDetail;

    @Column(name = "manager_id", length = 10, nullable = false)
    private String managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_manager_type_id", nullable = false)
    private AccountManagerType accountManagerType;

    @Column(name = "status", nullable = false)
    @Type(type = "pgsql_enum")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "name_temp", length = 128)
    private String nameTemp;

    @Column(name = "middle_name_temp", length = 128)
    private String middleNameTemp;

    @Column(name = "surname_temp", length = 128)
    private String surnameTemp;
}

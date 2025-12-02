package phoenix.core.customer.model.entity.customer;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "status_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "unwanted_customers", schema = "customer")
public class UnwantedCustomer {
    @Id
    @SequenceGenerator(name = "unwanted_customers_id_seq", sequenceName = "customer.unwanted_customers_id_seq",allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unwanted_customers_id_seq")
    private Long id;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "name")
    private String name;

    @Column(name = "unwanted_customers_reason_id")
    private Long unwantedCustomerReasonId;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Column(name = "create_contract_restrict")
    private Boolean createContractRestriction;

    @Column(name = "create_order_restrict")
    private Boolean createOrderRestriction;

    @Column(name = "system_user_id")
    private String systemUserid;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Type(type = "status_enum")
    private UnwantedCustomerStatus status;

    @Column(name = "create_date")
    private Date createDate;

    @Column(name = "modify_date")
    private Date modifyDate;

    @Column(name = "modify_system_user_id")
    private String modifySystemUserId;

}

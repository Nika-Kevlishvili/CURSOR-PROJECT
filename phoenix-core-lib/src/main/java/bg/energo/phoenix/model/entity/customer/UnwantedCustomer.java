package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "unwanted_customers", schema = "customer")
public class UnwantedCustomer extends BaseEntity {
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

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UnwantedCustomerStatus status;
}

package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.customer.AccountManagerType;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * <h2>Customer Account Manager</h2>
 * <b>Variables</b>:<br>
 * <ul>
 *     <li>{@link #id} - Customer Account Manager ID</li>
 *     <li>{@link #customerDetail} - {@link CustomerDetails CustomerDetail} that holds presented {@link CustomerAccountManager}</li>
 *     <li>{@link #managerId} - {@link AccountManager#id}</li>
 *     <li>{@link #accountManagerType} - {@link AccountManagerType#id}</li>
 *     <li>{@link #status} - Customer Account Manager Status</li>
 * </ul>
 * @see bg.energo.phoenix.service.customer.CustomerAccountManagerService CustomerAccountManagerService
 */
@Entity
@Table(name = "customer_account_managers", schema = "customer")

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

    @Column(name = "account_manager_id", length = 10, nullable = false)
    private Long managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_manager_type_id", nullable = false,referencedColumnName = "id")
    private AccountManagerType accountManagerType;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private Status status;
}

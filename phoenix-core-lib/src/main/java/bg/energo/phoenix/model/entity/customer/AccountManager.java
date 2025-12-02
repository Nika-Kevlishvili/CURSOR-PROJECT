package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * <h2>AccountManager</h2>
 * Account Manager entity, update fetching from Portal every day at midnight <b>00:00</b><br>
 * <b>Variables</b>:<br>
 * <ul>
 *     <li>{@link #id} - Account Manager ID</li>
 *     <li>{@link #userName} - Account Manager username (example: a111111)</li>
 *     <li>{@link #firstName} - Account Manager Firstname</li>
 *     <li>{@link #lastName} - Account Manager Lastname</li>
 *     <li>{@link #displayName} - Account Manager Display Name (example: Doe, Joe)</li>
 *     <li>{@link #email} - Account Manager Email</li>
 *     <li>{@link #organizationalUnit} - Account Manager Organization Unit</li>
 *     <li>{@link #businessUnit} - Account Manager Business Unit</li>
 *     <li>{@link #status} - Account Manager Current Status</li>
 * </ul>
 * @see bg.energo.phoenix.service.customer.CustomerAccountManagerService#updateAccountManagersFromPortal(List) CustomerAccountManagerService.updateAccountManagersFromPortal()
 * @see bg.energo.phoenix.repository.customer.AccountManagerRepository AccountManagerRepository
 */
@Entity
@Table(name = "account_managers", schema = "customer")

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class AccountManager extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false, length = 2048)
    private String userName;

    @Column(name = "first_name", nullable = false, length = 2048)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 2048)
    private String lastName;

    @Column(name = "display_name", nullable = false, length = 2048)
    private String displayName;

    @Column(name = "email", nullable = false, length = 2048)
    private String email;

    @Column(name = "organizational_unit", nullable = false, length = 2048)
    private String organizationalUnit;

    @Column(name = "business_unit", nullable = false, length = 2048)
    private String businessUnit;

    @Column(name = "status", nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private Status status;
}

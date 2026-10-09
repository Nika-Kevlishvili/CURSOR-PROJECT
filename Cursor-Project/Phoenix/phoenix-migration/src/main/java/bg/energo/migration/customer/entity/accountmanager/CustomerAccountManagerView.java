package bg.energo.migration.customer.entity.accountmanager;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

@Data
@Entity
@Immutable
@Table(name = "v_customer_account_managers", schema = "customer_migration")
public class CustomerAccountManagerView {

    @Id
    private Long id;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "account_manager_id")
    private Long accountManagerId;

    @Column(name = "account_manager_type_id")
    private Long accountManagerTypeId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;
}

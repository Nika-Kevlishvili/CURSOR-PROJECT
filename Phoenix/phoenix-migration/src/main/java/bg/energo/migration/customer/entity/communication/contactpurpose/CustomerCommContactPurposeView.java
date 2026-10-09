package bg.energo.migration.customer.entity.communication.contactpurpose;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "v_customer_comm_contact_purposes", schema = "customer_migration")
public class CustomerCommContactPurposeView {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

}

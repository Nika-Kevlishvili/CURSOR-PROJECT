package bg.energo.migration.customer.entity.communication.contacts;

import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerCommContactTypes;
import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

@Data
@Entity
@Immutable
@Table(name = "v_customer_communication_contacts", schema = "customer_migration")
public class CustomerCommContactView {
    @Id
    private Long id;

    @Column(name = "send_sms")
    private Boolean sendSms;

    @Column(name = "contact_type")
    @Enumerated(EnumType.STRING)
    private CustomerCommContactTypes contactType;

    @Column(name = "contact_value")
    private String contactValue;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "customer_identifier")
    private String customerIdentifier;
}

package bg.energo.phoenix.model.entity.customer.communication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)

@Table(name = "customer_communication_contacts", schema = "customer")
public class CustomerCommunicationContacts extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "customer_communication_contacts_seq",
            sequenceName = "customer.customer_communication_contacts_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_communication_contacts_seq"
    )    private Long id;

    @Column(name = "send_sms", nullable = false, columnDefinition = "boolean default false")
    private boolean sendSms;

    @Column(name = "platform_id")
    private Long platformId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "contact_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CustomerCommContactTypes contactType;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationsId;

    @Column(name = "contact_value")
    private String contactValue;
}



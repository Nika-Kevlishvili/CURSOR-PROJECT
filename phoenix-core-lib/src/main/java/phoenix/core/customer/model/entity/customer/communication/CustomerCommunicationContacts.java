package phoenix.core.customer.model.entity.customer.communication;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.enums.customer.CustomerCommContactTypes;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
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
    @Type(type = "pgsql_enum")
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "contact_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private CustomerCommContactTypes contactType;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationsId;

    @Column(name = "contact_value")
    private String contactValue;
}



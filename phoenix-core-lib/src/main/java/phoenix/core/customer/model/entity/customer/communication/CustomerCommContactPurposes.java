package phoenix.core.customer.model.entity.customer.communication;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import phoenix.core.customer.model.entity.BaseEntity;
import phoenix.core.customer.model.enums.customer.Status;

import javax.persistence.*;

@Entity
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "customer_comm_contact_purposes", schema = "customer")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerCommContactPurposes extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "customer_comm_contact_purposes_seq",
            sequenceName = "customer.customer_comm_contact_purposes_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "customer_comm_contact_purposes_seq"
    )
    private Long id;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationsId;

    @Column(name = "contact_purpose_id", nullable = false)
    private Long contactPurposeId;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "status", nullable = false)
    private Status status;

}

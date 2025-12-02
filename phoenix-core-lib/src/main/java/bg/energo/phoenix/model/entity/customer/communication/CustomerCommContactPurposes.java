package bg.energo.phoenix.model.entity.customer.communication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.customer.Status;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity

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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private Status status;

}

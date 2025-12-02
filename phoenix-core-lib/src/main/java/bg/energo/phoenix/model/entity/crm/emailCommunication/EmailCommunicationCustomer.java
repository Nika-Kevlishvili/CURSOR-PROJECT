package bg.energo.phoenix.model.entity.crm.emailCommunication;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.crm.emailCommunication.EmailCommunicationCustomerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Types;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "email_communication_customers", schema = "crm")
public class EmailCommunicationCustomer extends BaseEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "email_communication_customers_id_seq"
    )
    @SequenceGenerator(
            name = "email_communication_customers_id_seq",
            sequenceName = "crm.email_communication_customers_id_seq",
            allocationSize = 1
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationId;

    @Column(name = "email_communication_id")
    private Long emailCommunicationId;

    @Column(name = "contact_purpose_id")
    private Long contactPurposeId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EmailCommunicationCustomerStatus status;

    @Column(name = "email_body")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String emailBody;

    @Column(name = "product_contract_detail_id")
    private Long productContractDetailId;

    @Column(name = "service_contract_detail_id")
    private Long serviceContractDetailId;

}
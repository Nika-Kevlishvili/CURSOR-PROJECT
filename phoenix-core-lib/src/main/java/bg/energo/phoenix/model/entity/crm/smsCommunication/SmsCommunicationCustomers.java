package bg.energo.phoenix.model.entity.crm.smsCommunication;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sms_communication_customers", schema = "crm")
public class SmsCommunicationCustomers extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "sms_communication_customers_id_seq",
            sequenceName = "crm.sms_communication_customers_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sms_communication_customers_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "customer_communication_id")
    private Long customerCommunicationId;

    @Column(name = "sms_communication_id")
    private Long smsCommunicationId;

    @Column(name = "sms_comm_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SmsCommStatus smsCommStatus;

    @Column(name = "sms_body")
    private String smsBody;

    @Column(name = "service_contract_detail_id")
    private Long serviceContractDetailid;

    @Column(name = "product_contract_detail_id")
    private Long productContractDetailId;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "contract_id")
    private Long contractId;

}

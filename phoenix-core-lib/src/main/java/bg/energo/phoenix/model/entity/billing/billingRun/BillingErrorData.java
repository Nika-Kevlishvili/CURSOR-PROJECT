package bg.energo.phoenix.model.entity.billing.billingRun;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.service.billing.billingRun.errors.BillingProtocol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Table(name = "billing_error_data", schema = "billing")
public class BillingErrorData extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "billing_errors_id_seq",
            sequenceName = "billing.billing_error_data_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "billing_errors_id_seq"
    )
    private Long id;
    @Column(name = "invoice_number")
    private String invoiceNumbers;



    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "error_protocol")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private BillingProtocol errorProtocol;
    @Column(name = "billing_id")
    private Long billingRunId;

}

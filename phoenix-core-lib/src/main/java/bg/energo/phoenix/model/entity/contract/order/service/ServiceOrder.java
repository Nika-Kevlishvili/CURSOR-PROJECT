package bg.energo.phoenix.model.entity.contract.order.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Builder
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders", schema = "service_order")
public class ServiceOrder extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "orders_id_seq",
            sequenceName = "orders_id_seq",
            schema = "service_order",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "orders_id_seq"
    )
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "service_detail_id")
    private Long serviceDetailId;

    @Column(name = "direct_debit")
    private Boolean directDebit;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "iban")
    private String iban;

    @Column(name = "applicable_interest_rate_id")
    private Long applicableInterestRateId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "prepayment_term_in_calendar_days")
    private Integer prepaymentTermInCalendarDays;

    @Column(name = "customer_detail_id")
    private Long customerDetailId;

    @Column(name = "customer_communication_id_for_billing")
    private Long customerCommunicationIdForBilling;

    @Column(name = "invoice_payment_term_id")
    private Long invoicePaymentTermId;

    @Column(name = "order_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ServiceOrderStatus orderStatus; // business status

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status; // entity status

    @Column(name = "status_modify_date")
    private LocalDate statusModifyDate;

    @Column(name = "invoice_payment_term_value")
    private Integer invoicePaymentTermValue;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "service_contract_term_id")
    private Long serviceContractTermId ;

    @Column(name = "contract_term_certain_date")
    private LocalDate contractTermCertainDateValue ;

    @Column(name = "order_invoice_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OrderInvoiceStatus orderInvoiceStatus;

    @Column(name = "invoice_template_id")
    private Long invoiceTemplateId;
    @Column(name = "email_template_id")
    private Long emailTemplateId;
}

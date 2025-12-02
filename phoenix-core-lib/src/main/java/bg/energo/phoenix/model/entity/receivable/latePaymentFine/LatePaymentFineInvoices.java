package bg.energo.phoenix.model.entity.receivable.latePaymentFine;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "late_payment_fine_invoices", schema = "receivable")
public class LatePaymentFineInvoices extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "late_payment_fine_invoices_id_seq",
            sequenceName = "receivable.late_payment_fine_invoices_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "late_payment_fine_invoices_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "late_paid_amount")
    private BigDecimal latePaidAmount;

    @Column(name = "overdue_start_date")
    private LocalDate overdueStartDate;

    @Column(name = "overdue_end_date")
    private LocalDate overdueEndDate;

    @Column(name = "number_of_days")
    private Long numberOfDays;

    @Column(name = "percentage")
    private BigDecimal percentage;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "late_payment_fine_id")
    private Long latePaymentFineId;

    @Column(name = "fee")
    private BigDecimal fee;

    @Column(name = "currency_id")
    private Long currencyId;

}

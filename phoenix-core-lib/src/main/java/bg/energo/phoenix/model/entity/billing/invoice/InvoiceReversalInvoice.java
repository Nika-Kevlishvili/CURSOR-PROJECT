package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceReversalStatus;
import bg.energo.phoenix.service.billing.invoice.reversal.InvoiceReversalModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(schema = "invoice",name = "invoice_reversal_invoice")
public class InvoiceReversalInvoice extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "invoice_reversal_invoice_id_seq",
            sequenceName = "invoice_reversal_invoice_id_seq",
            schema = "invoice",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "invoice_reversal_invoice_id_seq"
    )
    private Long id;

    @Column(name = "reversal_id")
    private Long reversalId;
    @Column(name = "invoice_id")
    private Long invoiceId;


    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private InvoiceReversalStatus reversalStatus;

    public InvoiceReversalInvoice(InvoiceReversalModel invoiceReversalModel,Long reversalId) {
        this.reversalId=reversalId;
        this.invoiceId=invoiceReversalModel.getInvoiceId();
        this.reversalStatus=InvoiceReversalStatus.NOT_STARTED;
    }
}

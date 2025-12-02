package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(schema = "invoice", name = "invoice_cancelations")
public class InvoiceCancellation extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "invoice_cancelations_id_seq",
            sequenceName = "invoice_cancelations_id_seq",
            schema = "invoice",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "invoice_cancelations_id_seq"
    )
    private Long id;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "tax_event_date")
    private LocalDate taxEventDate;

    @Column(name = "contract_template_id")
    private Long contractTemplateId;
    @Column(name = "email_template_id")
    private Long emailTemplateId;
}

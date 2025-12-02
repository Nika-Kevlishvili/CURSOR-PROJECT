package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice_cancellation_numbers", schema = "invoice")
public class InvoiceCancellationNumber extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_cancellation_numbers_id_gen")
    @SequenceGenerator(name = "invoice_cancellation_numbers_id_gen", schema = "invoice", sequenceName = "invoice_cancellation_numbers_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "number", nullable = false)
    private Long number;

    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;

    @NotNull
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;
}
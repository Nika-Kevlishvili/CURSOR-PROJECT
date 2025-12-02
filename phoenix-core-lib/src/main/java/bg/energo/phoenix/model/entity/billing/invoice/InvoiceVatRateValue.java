package bg.energo.phoenix.model.entity.billing.invoice;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice_vat_rate_values", schema = "invoice")
public class InvoiceVatRateValue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_vat_rate_values_id_gen")
    @SequenceGenerator(schema = "invoice", name = "invoice_vat_rate_values_id_gen", sequenceName = "invoice_vat_rate_values_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "vat_rate_percent", nullable = false)
    private BigDecimal vatRatePercent;

    @NotNull
    @Column(name = "amount_excluding_vat", nullable = false)
    private BigDecimal amountExcludingVat;

    @NotNull
    @Column(name = "value_of_vat", nullable = false)
    private BigDecimal valueOfVat;

    @NotNull
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;
}
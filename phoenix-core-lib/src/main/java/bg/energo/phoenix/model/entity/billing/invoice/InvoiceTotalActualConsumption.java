package bg.energo.phoenix.model.entity.billing.invoice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_total_actual_consumption", schema = "invoice")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceTotalActualConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "total_actual_consumption")
    private BigDecimal totalActualConsumption;
}

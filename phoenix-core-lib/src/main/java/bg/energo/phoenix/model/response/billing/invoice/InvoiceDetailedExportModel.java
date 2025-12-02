package bg.energo.phoenix.model.response.billing.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceDetailedExportModel {

    private BigDecimal vatRatePercent;
    private BigDecimal amountExcludingVat;
    private BigDecimal valueOfVat;
    private Long invoiceId;

    @Override
    public String toString() {
        return "{" +
               "vatRatePercent=" + vatRatePercent +
               ", amountExcludingVat=" + amountExcludingVat +
               ", valueOfVat=" + valueOfVat +
               '}';
    }
}

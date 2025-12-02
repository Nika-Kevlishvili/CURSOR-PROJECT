package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class BillingRunDocumentSummaryDataAdvancePaymentModel {
    public String PC;
    public BigDecimal Value;
    public String ValueUnit;
}

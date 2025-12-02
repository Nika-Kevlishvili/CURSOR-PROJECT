package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BillingRunDocumentSummaryDataModel {
    public BigDecimal TotalExclVat;
    @Getter
    public BigDecimal VatRate;
    public BigDecimal TotalVat;
    public BigDecimal TotalBeforeInterimDeduction=BigDecimal.ZERO;
    public BigDecimal TotalAfterInterimDeduction=BigDecimal.ZERO;
    public List<BillingRunDocumentSummaryDataPriceComponentModel> DirectPC = new ArrayList<>();
    public List<BillingRunDocumentSummaryDataPriceComponentModel> IndirectPC = new ArrayList<>();

    public List<BillingRunDocumentSummaryDataPriceComponentGroupModel> PCG = new ArrayList<>();
    public List<BillingRunDocumentSummaryDataAdvancePaymentModel> AdvancePayments = new ArrayList<>();
    public List<BillingRunDocumentSummaryDataAdvancePaymentModel> DeductedAdvancePayments = new ArrayList<>();
}

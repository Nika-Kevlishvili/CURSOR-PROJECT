package bg.energo.phoenix.service.billing.model.documentmodels;

import lombok.Builder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
public class BillingRunDocumentDetailedDataPriceComponentScale {
    public LocalDate PeriodFrom;
    public LocalDate PeriodTo;
    public String PeriodCounter;
    public String Meter;
    public String Product;
    public List<BillingRunDocumentDetailedDataPriceComponentModel> ListPC = new ArrayList<>();
}

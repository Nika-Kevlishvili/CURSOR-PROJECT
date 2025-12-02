package bg.energo.phoenix.model.response.billing.invoice;

import java.util.List;

public class SummaryDataResponse {
    private List<InvoiceSummaryDataResponse> directPc;
    private List<SummaryDataPriceComponentGroupResponse> fromPcGroups;

}

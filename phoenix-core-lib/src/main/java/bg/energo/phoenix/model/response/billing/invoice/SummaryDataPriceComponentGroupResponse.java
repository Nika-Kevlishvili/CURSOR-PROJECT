package bg.energo.phoenix.model.response.billing.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SummaryDataPriceComponentGroupResponse {
    private InvoiceSummaryDataResponse groupInfo;
    private List<InvoiceSummaryDataResponse> groupPcList;
}

package bg.energo.phoenix.model.response.billing.billingRun.manualInvoice;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ManualInvoiceDetailedDataParametersResponse {

    private List<DetailedDataRowParametersViewResponse> detailedDataRowParametersList;

}

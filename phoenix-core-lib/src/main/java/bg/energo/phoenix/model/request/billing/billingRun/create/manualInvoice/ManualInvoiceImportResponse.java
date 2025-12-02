package bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice;

import bg.energo.phoenix.model.response.billing.billingRun.DetailedDataRowParametersResponse;
import bg.energo.phoenix.model.response.billing.billingRun.SummaryDataRowParametersResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManualInvoiceImportResponse {

    private List<SummaryDataRowParametersResponse> summaryDataRowList;

    private List<DetailedDataRowParametersResponse> detailedDataRowParametersList;

}

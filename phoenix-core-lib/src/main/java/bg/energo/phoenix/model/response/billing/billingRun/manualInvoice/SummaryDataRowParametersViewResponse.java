package bg.energo.phoenix.model.response.billing.billingRun.manualInvoice;

import bg.energo.phoenix.model.response.billing.billingRun.SummaryDataRowParametersResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDataRowParametersViewResponse extends SummaryDataRowParametersResponse {
    private Long id;
}

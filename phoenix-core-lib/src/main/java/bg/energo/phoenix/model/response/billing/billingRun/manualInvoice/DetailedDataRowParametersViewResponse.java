package bg.energo.phoenix.model.response.billing.billingRun.manualInvoice;

import bg.energo.phoenix.model.response.billing.billingRun.DetailedDataRowParametersResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetailedDataRowParametersViewResponse extends DetailedDataRowParametersResponse {

    private Long id;

}

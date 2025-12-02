package bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice;

import bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice.SummaryDataRowParameters;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SummaryDataRowEditParameters extends SummaryDataRowParameters {

    private Long id;

}

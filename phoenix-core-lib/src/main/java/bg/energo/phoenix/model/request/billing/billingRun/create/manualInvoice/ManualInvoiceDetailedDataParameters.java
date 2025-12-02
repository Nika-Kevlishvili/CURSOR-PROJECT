package bg.energo.phoenix.model.request.billing.billingRun.create.manualInvoice;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ManualInvoiceDetailedDataParameters {

    private List<@Valid DetailedDataRowParameters> detailedDataRowParametersList;

}

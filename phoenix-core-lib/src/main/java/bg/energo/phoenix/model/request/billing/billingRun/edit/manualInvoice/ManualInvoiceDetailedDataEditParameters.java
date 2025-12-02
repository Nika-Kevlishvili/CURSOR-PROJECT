package bg.energo.phoenix.model.request.billing.billingRun.edit.manualInvoice;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.DetailedDataRowIdsValidator;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@DetailedDataRowIdsValidator
public class ManualInvoiceDetailedDataEditParameters {

    private List<@Valid DetailedDataRowEditParameters> detailedDataRowParametersList;

}

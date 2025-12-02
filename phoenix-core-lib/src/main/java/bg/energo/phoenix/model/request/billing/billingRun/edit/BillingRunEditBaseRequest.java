package bg.energo.phoenix.model.request.billing.billingRun.edit;

import bg.energo.phoenix.model.customAnotations.billing.billingRun.BillingRunEditBaseRequestValidator;
import bg.energo.phoenix.model.enums.billing.billings.BillingType;
import bg.energo.phoenix.model.request.billing.billingRun.BillingRunCommonParameters;
import bg.energo.phoenix.model.request.billing.billingRun.StandardBillingParameters;
import bg.energo.phoenix.model.request.billing.billingRun.iap.InterimAndAdvancePaymentParameters;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@BillingRunEditBaseRequestValidator
public class BillingRunEditBaseRequest {

    public @Valid StandardBillingParameters basicParameters;

    private @Valid BillingRunCommonParameters commonParameters;

    private @Valid InterimAndAdvancePaymentParameters interimAndAdvancePaymentParameters;

    @NotNull(message = "basicParameters.billingType-[billingType] must not be null;")
    private BillingType billingType;

}

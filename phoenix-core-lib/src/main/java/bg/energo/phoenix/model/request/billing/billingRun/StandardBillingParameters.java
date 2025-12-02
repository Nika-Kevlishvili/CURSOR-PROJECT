package bg.energo.phoenix.model.request.billing.billingRun;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.billing.billings.ApplicationModelType;
import bg.energo.phoenix.model.enums.billing.billings.BillingApplicationLevel;
import bg.energo.phoenix.model.enums.billing.billings.BillingCriteria;
import bg.energo.phoenix.model.enums.billing.billings.BillingEndDate;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@StandardBillingRunConditionValidator
public class StandardBillingParameters {

    @NotEmpty(message = "basicParameters.applicationModelType-[applicationModelType] is mandatory;")
    private List<ApplicationModelType> applicationModelType;

    @NotNull(message = "basicParameters.billingCriteria-[billingCriteria] is mandatory;")
    private BillingCriteria billingCriteria;

    private BillingApplicationLevel billingApplicationLevel;

    private String customersContractOrPODConditions;

    private String listOfCustomersContractsOrPOD;

    @DateRangeValidator(fieldPath = "basicParameters.maxEndDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate maxEndDate;

    private BillingEndDate periodicMaxEndDate;

    private Integer periodicMaxEndDateValue;
}

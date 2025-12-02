package bg.energo.phoenix.model.request.product.service;

import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;
import bg.energo.phoenix.model.enums.product.service.ServicePaymentMethod;
import bg.energo.phoenix.model.enums.product.service.ServicePeriodicity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAdditionalSettingsRequest {

    @NotNull(message = "additionalSettings.periodicity-Periodicity must not be null;")
    private ServicePeriodicity periodicity;

    @NotNull(message = "additionalSettings.paymentMethod-Payment method must not be null;")
    private ServicePaymentMethod paymentMethod;

    @NotNull(message = "additionalSettings.paymentBeforeExecution-[Payment before execution] field must not be null;")
    private Boolean paymentBeforeExecution;

    @NotNull(message = "additionalSettings.executionLevel-Execution level must not be null;")
    private ServiceExecutionLevel executionLevel;

    private List<Long> collectionChannelIds;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField1-Additional Field 1 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField1;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField2-Additional Field 2 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField2;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField3-Additional Field 3 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField3;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField4-Additional Field 4 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField4;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField5-Additional Field 5 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField5;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField6-Additional Field 6 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField6;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField7-Additional Field 7 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField7;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField8-Additional Field 8 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField8;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField9-Additional Field 9 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField9;

    @Size(min = 1, max = 1024, message = "additionalSettings.additionalField10-Additional Field 10 should not be blank and should match allowed length: range [{min}:{max}];")
    private String additionalField10;

    private List<ServiceAdditionalParamsRequest> serviceAdditionalParams;

}

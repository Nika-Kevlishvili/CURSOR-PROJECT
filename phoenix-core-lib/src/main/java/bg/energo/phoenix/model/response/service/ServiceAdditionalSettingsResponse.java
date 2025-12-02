package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;
import bg.energo.phoenix.model.enums.product.service.ServiceIneligiblePaymentChannel;
import bg.energo.phoenix.model.enums.product.service.ServicePaymentMethod;
import bg.energo.phoenix.model.enums.product.service.ServicePeriodicity;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ServiceAdditionalSettingsResponse {

    private ServicePeriodicity periodicity;

    private ServicePaymentMethod paymentMethod;

    private Boolean paymentBeforeExecution;

    private ServiceExecutionLevel executionLevel;

    private Set<ServiceIneligiblePaymentChannel> ineligiblePaymentChannels;

    private String additionalField1;
    private String additionalField2;
    private String additionalField3;
    private String additionalField4;
    private String additionalField5;
    private String additionalField6;
    private String additionalField7;
    private String additionalField8;
    private String additionalField9;
    private String additionalField10;

    private List<ShortResponse> collectionChannels;

    private List<ServiceAdditionalParamsResponse> serviceAdditionalParams;
}

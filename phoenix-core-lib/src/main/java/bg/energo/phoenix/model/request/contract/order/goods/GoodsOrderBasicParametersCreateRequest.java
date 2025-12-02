package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import bg.energo.phoenix.model.request.contract.ProxyEditRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GoodsOrderBasicParametersCreateRequest {

    private Boolean directDebit;

    private Long bankId;

    @ValidIBAN(errorMessageKey = "basicParameters.iban")
    private String iban;

    @NotNull(message = "basicParameters.interestRateId-Interest Rate should not be null;")
    private Long interestRateId;

    @Min(value = 1, message = "basicParameters.paymentTermInCalendarDays-Value must be between 1 and 9999 characters;")
    @Max(value = 9999, message = "basicParameters.paymentTermInCalendarDays-Value must be between 1 and 9999 characters;")
    private Integer paymentTermInCalendarDays;

    private Long campaignId;

    @NotNull(message = "basicParameters.customerDetailId-Customer version is mandatory;")
    private Long customerDetailId;

    @NotNull(message = "basicParameters.customerCommunicationIdForBilling-Communication for billing is mandatory;")
    private Long customerCommunicationIdForBilling;


   // private List<@Valid GoodsOrderProxyAddRequest> goodsOrderProxyAddRequest;
    private List<@Valid ProxyEditRequest> proxy;

    @Valid
    private GoodsOrderPaymentTermRequest paymentTerm;

    private Long employeeId;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.internalIntermediaries")
    private List<Long> internalIntermediaries;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.externalIntermediaries")
    private List<Long> externalIntermediaries;

    @DuplicatedValuesValidator(fieldPath = "basicParameters.assistingEmployees")
    private List<Long> assistingEmployees;

    private List<@Valid RelatedEntityRequest> relatedEntities;

    private boolean noInterestInOverdueDebts;

    private Long invoiceTemplateId;
    private Long emailTemplateId;
}

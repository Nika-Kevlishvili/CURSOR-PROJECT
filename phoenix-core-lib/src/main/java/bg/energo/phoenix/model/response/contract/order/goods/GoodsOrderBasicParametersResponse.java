package bg.energo.phoenix.model.response.contract.order.goods;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderStatus;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import bg.energo.phoenix.model.response.customer.CustomerCommunicationDataResponse;
import bg.energo.phoenix.model.response.customer.CustomerDetailsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsOrderBasicParametersResponse {

    private Long id;

    private LocalDateTime createDate;

    private String orderNumber;

    private EntityStatus status;

    private Boolean directDebit;
    private BankResponse bank;
    private String iban;

    private Long applicableInterestRateId;
    private String applicableInterestRateName;

    private Long campaignId;
    private String campaignName;

    private Integer prepaymentTermInCalendarDays;

    private CustomerDetailsShortResponse customer;

    private CustomerCommunicationDataResponse billingCommunicationData;

    private Boolean noInterestOnOverdueDebts;

    private String incomeAccountNumber;

    private String costCenterControllingOrder;

    private GoodsOrderStatus orderStatus;
    private LocalDate statusModifyDate;
    private OrderInvoiceStatus orderInvoiceStatus;
    private List<GoodsOrderProxyResponse> proxyResponse;

    private Long employeeId;
    private String employeeName;

    private List<GoodsOrderPaymentTermResponse> paymentTerms;
    private List<GoodsOrderSubObjectShortResponse> internalIntermediaries;
    private List<GoodsOrderSubObjectShortResponse> externalIntermediaries;
    private List<GoodsOrderSubObjectShortResponse> assistingEmployees;
    private List<SystemActivityShortResponse> activities;
    private List<RelatedEntityResponse> relatedEntities;
    private List<TaskShortResponse> tasks;
    private ShortResponse invoice;

    private ContractTemplateShortResponse invoiceTemplateResponse;
    private ContractTemplateShortResponse emailTemplateResponse;
}

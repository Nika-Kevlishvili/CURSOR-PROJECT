package bg.energo.phoenix.model.response.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunications;
import bg.energo.phoenix.model.entity.nomenclature.contract.Campaign;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.enums.contract.order.service.ServiceOrderStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import bg.energo.phoenix.model.response.activity.SystemActivityShortResponse;
import bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyBaseResponse;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.model.response.template.ContractTemplateShortResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceOrderBasicParametersResponse {

    private String orderNumber;
    private LocalDate statusModifyDate;
    private LocalDateTime creationDate;
    private Long serviceDetailId;
    private String serviceDetailName;
    private Long serviceId;
    private Long serviceVersionId;
    private ServiceOrderBankingDetailsResponse bankingDetails;
    private Long interestRateId;
    private String interestRateName;
    private Long campaignId;
    private String campaignName;
    private Integer prepaymentTermInCalendarDays;
    private Long customerId;
    private Long versionId;
    private Long customerDetailId;
    private String customerDetailName;
    private String customerIdentifier;
    private CustomerType customerType;
    private Boolean businessActivity;
    private Long customerCommunicationIdForBilling;
    private String customerCommunicationNameForBilling;
    private ServiceOrderStatus orderStatus;
    private Integer quantity;
    private EntityStatus status;
    private ServiceOrderSubObjectShortResponse employee;
    private OrderInvoiceStatus orderInvoiceStatus;

    private List<SystemActivityShortResponse> activities;
    private List<ServiceOrderSubObjectShortResponse> internalIntermediaries;
    private List<ServiceOrderSubObjectShortResponse> externalIntermediaries;
    private List<ServiceOrderSubObjectShortResponse> assistingEmployees;
    private List<RelatedEntityResponse> relatedEntities;
    private List<ServiceOrderProxyBaseResponse> proxies;
    private List<TaskShortResponse> tasks;
    private List<ShortResponse> invoices;
    private String concatPurposes;
    private ContractTemplateShortResponse invoiceTemplateResponse;
    private ContractTemplateShortResponse emailTemplateResponse;

    public ServiceOrderBasicParametersResponse(ServiceOrder serviceOrder,
                                               ServiceDetails serviceDetail,
                                               InterestRate interestRate,
                                               Campaign campaign,
                                               Bank bank,
                                               CustomerDetails customerDetail,
                                               CustomerCommunications customerCommunication,
                                               Customer customer,
                                               String legalFormName) {
        this.orderNumber = serviceOrder.getOrderNumber();
        this.statusModifyDate = serviceOrder.getStatusModifyDate();
        this.creationDate = serviceOrder.getCreateDate();
        this.serviceDetailId = serviceDetail.getId();
        this.serviceDetailName = serviceDetail.getName();
        this.serviceId = serviceDetail.getService().getId();
        this.serviceVersionId = serviceDetail.getVersion();
        this.bankingDetails = new ServiceOrderBankingDetailsResponse(bank, serviceOrder);
        this.interestRateId = interestRate.getId();
        this.interestRateName = interestRate.getName();
        this.campaignId = campaign == null ? null : campaign.getId();
        this.campaignName = campaign == null ? null : campaign.getName();
        this.prepaymentTermInCalendarDays = serviceOrder.getPrepaymentTermInCalendarDays();
        this.customerId = customerDetail.getCustomerId();
        this.versionId = customerDetail.getVersionId();
        this.customerDetailId = customerDetail.getId();
        this.customerDetailName = String.format(
                "%s (%s %s %s %s",
                customer.getIdentifier(),
                customerDetail.getName(),
                StringUtils.isNotEmpty(customerDetail.getMiddleName()) ? customerDetail.getMiddleName() : "",
                StringUtils.isNotEmpty(customerDetail.getLastName()) ? customerDetail.getLastName() : "",
                StringUtils.isNotEmpty(legalFormName) ? legalFormName : ""
        ).trim();
        this.customerDetailName=this.customerDetailName+")";
        this.customerIdentifier = customer.getIdentifier();
        this.customerType = customer.getCustomerType();
        this.customerCommunicationIdForBilling = customerCommunication.getId();
        this.customerCommunicationNameForBilling = customerCommunication.getContactTypeName();
        this.businessActivity=customerDetail.getBusinessActivity();
        this.orderStatus = serviceOrder.getOrderStatus();
        this.quantity = serviceOrder.getQuantity();
        this.status = serviceOrder.getStatus();
        this.orderInvoiceStatus = serviceOrder.getOrderInvoiceStatus();
    }

}

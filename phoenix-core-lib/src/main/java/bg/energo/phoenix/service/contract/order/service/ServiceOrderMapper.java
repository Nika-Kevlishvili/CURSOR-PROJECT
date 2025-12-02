package bg.energo.phoenix.service.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderProxy;
import bg.energo.phoenix.model.enums.contract.order.OrderInvoiceStatus;
import bg.energo.phoenix.model.request.contract.order.service.*;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderAuthorizedProxyRequest;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyBaseRequest;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyRequest;
import bg.energo.phoenix.model.request.contract.order.service.proxy.ServiceOrderProxyUpdateRequest;

public class ServiceOrderMapper {

    public static ServiceOrder fromCreateRequestToEntity(ServiceOrderCreateRequest request, String orderNumber, Long employeeId) {
        ServiceOrderBasicParametersCreateRequest basicParameters = request.getBasicParameters();
        ServiceOrderServiceParametersRequest serviceParameters = request.getServiceParameters();

        return ServiceOrder.builder()
                .orderNumber(orderNumber)
                .orderStatus(basicParameters.getOrderStatus())
                .statusModifyDate(basicParameters.getStatusModifyDate())
                .serviceDetailId(basicParameters.getServiceDetailId())
                .directDebit(basicParameters.getBankingDetails().getDirectDebit())
                .bankId(basicParameters.getBankingDetails().getBankId())
                .iban(basicParameters.getBankingDetails().getIban())
                .applicableInterestRateId(basicParameters.getInterestRateId())
                .campaignId(basicParameters.getCampaignId())
                .prepaymentTermInCalendarDays(basicParameters.getPrepaymentTermInCalendarDays())
                .customerDetailId(basicParameters.getCustomerDetailId())
                .customerCommunicationIdForBilling(basicParameters.getCustomerCommunicationIdForBilling())
                .quantity(serviceParameters.getQuantity())
                .status(EntityStatus.ACTIVE)
                .employeeId(employeeId)
                .invoiceTemplateId(basicParameters.getInvoiceTemplateId())
                .emailTemplateId(basicParameters.getEmailTemplateId())
                .orderInvoiceStatus(OrderInvoiceStatus.NOT_GENERATED)
                .build();
    }


    public static void fromUpdateRequestToEntity(ServiceOrder serviceOrder, ServiceOrderUpdateRequest request) {
        ServiceOrderBasicParametersUpdateRequest basicParameters = request.getBasicParameters();
        ServiceOrderServiceParametersRequest serviceParameters = request.getServiceParameters();

        serviceOrder.setOrderStatus(basicParameters.getOrderStatus());
        serviceOrder.setStatusModifyDate(basicParameters.getStatusModifyDate());
        serviceOrder.setServiceDetailId(basicParameters.getServiceDetailId());
        serviceOrder.setDirectDebit(basicParameters.getBankingDetails().getDirectDebit());
        serviceOrder.setBankId(basicParameters.getBankingDetails().getBankId());
        serviceOrder.setIban(basicParameters.getBankingDetails().getIban());
        serviceOrder.setApplicableInterestRateId(basicParameters.getInterestRateId());
        serviceOrder.setCampaignId(basicParameters.getCampaignId());
        serviceOrder.setPrepaymentTermInCalendarDays(basicParameters.getPrepaymentTermInCalendarDays());
        serviceOrder.setCustomerDetailId(basicParameters.getCustomerDetailId());
        serviceOrder.setCustomerCommunicationIdForBilling(basicParameters.getCustomerCommunicationIdForBilling());
        serviceOrder.setQuantity(serviceParameters.getQuantity());
        serviceOrder.setEmployeeId(basicParameters.getEmployeeId());
        serviceOrder.setInvoiceTemplateId(basicParameters.getInvoiceTemplateId());
        serviceOrder.setEmailTemplateId(basicParameters.getEmailTemplateId());
    }


    public static ServiceOrderProxy fromProxyCreateRequestToEntity(ServiceOrderProxyBaseRequest proxyCreateRequest, ServiceOrder serviceOrder) {
        ServiceOrderProxyRequest proxy = proxyCreateRequest.getProxy();
        ServiceOrderAuthorizedProxyRequest authorizedProxy = proxyCreateRequest.getAuthorizedProxy();

        ServiceOrderProxy orderProxy = new ServiceOrderProxy();
        orderProxy.setStatus(EntityStatus.ACTIVE);
        orderProxy.setOrderId(serviceOrder.getId());

        populateProxyFields(proxy, orderProxy);

        if (authorizedProxy != null) {
            populateAuthorizedProxyFields(orderProxy, authorizedProxy);
        }

        return orderProxy;
    }


    public static ServiceOrderProxy fromProxyUpdateRequestToEntity(ServiceOrderProxy serviceOrderProxy, ServiceOrderProxyUpdateRequest proxyRequest) {
        ServiceOrderProxyRequest proxy = proxyRequest.getProxy();
        ServiceOrderAuthorizedProxyRequest authorizedProxy = proxyRequest.getAuthorizedProxy();

        populateProxyFields(proxy, serviceOrderProxy);

        if (authorizedProxy != null) {
            populateAuthorizedProxyFields(serviceOrderProxy, authorizedProxy);
        } else {
            serviceOrderProxy.setAuthorizedProxyForeignEntity(null);
            serviceOrderProxy.setAuthorizedProxyName(null);
            serviceOrderProxy.setAuthorizedProxyIdentifier(null);
            serviceOrderProxy.setAuthorizedProxyEmail(null);
            serviceOrderProxy.setAuthorizedProxyMobilePhone(null);
            serviceOrderProxy.setAuthorizedProxyAttorneyPowerNumber(null);
            serviceOrderProxy.setAuthorizedProxyDate(null);
            serviceOrderProxy.setAuthorizedProxyValidTill(null);
            serviceOrderProxy.setAuthorizedProxyNotaryPublic(null);
            serviceOrderProxy.setAuthorizedProxyRegistrationNumber(null);
            serviceOrderProxy.setAuthorizedProxyOperationArea(null);
        }

        return serviceOrderProxy;
    }


    private static void populateProxyFields(ServiceOrderProxyRequest proxy, ServiceOrderProxy orderProxy) {
        orderProxy.setName(proxy.getName());
        orderProxy.setForeignEntity(proxy.getForeignEntity());
        orderProxy.setIdentifier(proxy.getCustomerIdentifier());
        orderProxy.setEmail(proxy.getEmail());
        orderProxy.setMobilePhone(proxy.getPhone());
        orderProxy.setAttorneyPowerNumber(proxy.getPowerOfAttorneyNumber());
        orderProxy.setDate(proxy.getDate());
        orderProxy.setValidTill(proxy.getValidTill());
        orderProxy.setNotaryPublic(proxy.getNotaryPublic());
        orderProxy.setRegistrationNumber(proxy.getRegistrationNumber());
        orderProxy.setOperationArea(proxy.getOperationArea());
    }


    private static void populateAuthorizedProxyFields(ServiceOrderProxy serviceOrderProxy, ServiceOrderAuthorizedProxyRequest authorizedProxy) {
        serviceOrderProxy.setAuthorizedProxyForeignEntity(authorizedProxy.getForeignEntity());
        serviceOrderProxy.setAuthorizedProxyName(authorizedProxy.getName());
        serviceOrderProxy.setAuthorizedProxyIdentifier(authorizedProxy.getCustomerIdentifier());
        serviceOrderProxy.setAuthorizedProxyEmail(authorizedProxy.getEmail());
        serviceOrderProxy.setAuthorizedProxyMobilePhone(authorizedProxy.getPhone());
        serviceOrderProxy.setAuthorizedProxyAttorneyPowerNumber(authorizedProxy.getPowerOfAttorneyNumber());
        serviceOrderProxy.setAuthorizedProxyDate(authorizedProxy.getDate());
        serviceOrderProxy.setAuthorizedProxyValidTill(authorizedProxy.getValidTill());
        serviceOrderProxy.setAuthorizedProxyNotaryPublic(authorizedProxy.getNotaryPublic());
        serviceOrderProxy.setAuthorizedProxyRegistrationNumber(authorizedProxy.getRegistrationNumber());
        serviceOrderProxy.setAuthorizedProxyOperationArea(authorizedProxy.getOperationArea());
    }

}

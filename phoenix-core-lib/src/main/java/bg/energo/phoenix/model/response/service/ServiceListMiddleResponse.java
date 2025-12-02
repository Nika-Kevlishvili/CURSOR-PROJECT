package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.enums.product.service.ServiceDetailStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;

import java.time.LocalDateTime;

public interface ServiceListMiddleResponse {

    Long getServiceId();

    String getName();

    String getServiceGroupName();

    ServiceStatus getStatus();

    ServiceDetailStatus getServiceDetailStatus();

    String getServiceTypeName();

    String getContractTermsName();

    String getSalesChannelsName();

    Boolean getGlobalSalesChannel();

    String getContractTemplateName();

    LocalDateTime getDateOfCreation();

    boolean getIndividualService();

}

package bg.energo.phoenix.model.enums.product.service.list;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceTableColumn {
    SERVICE_ID("id"),
    SERVICE_NAME("name"),
    SERVICE_GROUP_NAME("serviceGroupName"),
    SERVICE_DETAIL_STATUS("serviceDetailStatus"),
    SERVICE_TYPE_NAME("serviceTypeName"),
    CONTRACT_TERMS("contractTermsName"),
    SALES_CHANNELS("salesChannelsName"),
    CONTRACT_TEMPLATE_NAME(""), // TODO: will be implemented later
    SERVICE_DATE_OF_CREATION("create_date"),
    INDIVIDUAL_SERVICE("individualService");

    private final String value;
}
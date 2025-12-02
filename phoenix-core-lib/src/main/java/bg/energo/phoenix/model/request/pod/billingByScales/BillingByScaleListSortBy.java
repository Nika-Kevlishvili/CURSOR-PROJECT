package bg.energo.phoenix.model.request.pod.billingByScales;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BillingByScaleListSortBy {
    ID("id"),
    POINT_OF_DELIVERY("identifier"),
    DATE_FROM("dateFrom"),
    DATE_TO("dateTo"),
    INVOICE_ID("invoiced"),
    CREATE_DATE("createDate"),
    INVOICED("invoiced");

    private final String value;
}

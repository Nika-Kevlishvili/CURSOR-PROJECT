package bg.energo.phoenix.model.response.pod.discount;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscountResponse {
    private Long id;

    private BigDecimal amountInPercent;

    private BigDecimal amountInMoneyPerKWH;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private String orderNumber;

    private String certificationNumber;

    private CurrencyResponse currency;

    private Long volumeWithoutDiscountInKWH;

    private DiscountCustomerShortResponse customer;

    private List<DiscountPointOfDeliveryShortResponse> pointOfDeliveries;

    private EntityStatus status;

    private Boolean isLockedByInvoice;

}

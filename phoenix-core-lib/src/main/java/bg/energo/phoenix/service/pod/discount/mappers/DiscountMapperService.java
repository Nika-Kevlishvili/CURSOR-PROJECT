package bg.energo.phoenix.service.pod.discount.mappers;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerDetails;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.pod.discount.Discount;
import bg.energo.phoenix.model.request.pod.discount.DiscountRequest;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyResponse;
import bg.energo.phoenix.model.response.pod.discount.DiscountCustomerShortResponse;
import bg.energo.phoenix.model.response.pod.discount.DiscountPointOfDeliveryShortResponse;
import bg.energo.phoenix.model.response.pod.discount.DiscountResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DiscountMapperService {
    public DiscountResponse mapFromEntity(Discount discount, Customer customer, CustomerDetails customerDetails, Currency currency, List<DiscountPointOfDeliveryShortResponse> discountPointOfDeliveries) {
        DiscountResponse discountResponse = DiscountResponse
                .builder()
                .id(discount.getId())
                .amountInPercent(discount.getAmountInPercent())
                .amountInMoneyPerKWH(discount.getAmountInMoneyPerKWH())
                .dateFrom(discount.getDateFrom())
                .dateTo(discount.getDateTo())
                .orderNumber(discount.getOrderNumber())
                .certificationNumber(discount.getCertificationNumber())
                .currency(new CurrencyResponse(currency))
                .volumeWithoutDiscountInKWH(discount.getVolumeWithoutDiscountInKWH())
                .status(discount.getStatus())
                .pointOfDeliveries(discountPointOfDeliveries)
                .isLockedByInvoice(discount.getInvoiced() != null && discount.getInvoiced())
                .build();

        if (customer != null && customerDetails != null) {
            discountResponse.setCustomer(new DiscountCustomerShortResponse(
                    customer.getId(),
                    customer.getIdentifier(),
                    String.join(" ",
                            Objects.requireNonNullElse(customerDetails.getName(), ""),
                            Objects.requireNonNullElse(customerDetails.getMiddleName(), ""),
                            Objects.requireNonNullElse(customerDetails.getLastName(), "")).replace("\\s+", " ").trim()));
        }

        return discountResponse;
    }

    public Discount mapToEntity(DiscountRequest request) {
        return Discount
                .builder()
                .amountInPercent(request.getAmountOfDiscount())
                .amountInMoneyPerKWH(request.getAmountOfDiscountInMoneyPerKWH())
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .orderNumber(request.getOrderNumber())
                .certificationNumber(request.getCertificationNumber())
                .currencyId(request.getCurrencyId())
                .volumeWithoutDiscountInKWH(request.getVolumeWithoutDiscountInKWH())
                .customerId(request.getCustomerId())
                .status(EntityStatus.ACTIVE)
                .build();
    }

    public Discount updateDiscount(Discount discount, DiscountRequest request) {
        discount.setAmountInPercent(request.getAmountOfDiscount());
        discount.setAmountInMoneyPerKWH(request.getAmountOfDiscountInMoneyPerKWH());
        discount.setDateFrom(request.getDateFrom());
        discount.setDateTo(request.getDateTo());
        discount.setOrderNumber(request.getOrderNumber());
        discount.setCertificationNumber(request.getCertificationNumber());
        discount.setCurrencyId(request.getCurrencyId());
        discount.setVolumeWithoutDiscountInKWH(request.getVolumeWithoutDiscountInKWH());
        discount.setCustomerId(request.getCustomerId());
        return discount;
    }
}

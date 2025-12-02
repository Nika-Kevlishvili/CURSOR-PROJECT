package bg.energo.phoenix.model.request.pod.discount;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.customAnotations.pod.discount.DiscountDateValidator;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DiscountDateValidator
public class DiscountRequest {
    @DateRangeValidator(fieldPath = "dateFrom", fromDate = "1990-01-01", toDate = "2090-12-31", includedDate = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "dateFrom-Date From must not be null;")
    private LocalDate dateFrom;

    @DateRangeValidator(fieldPath = "dateTo", fromDate = "1990-01-01", toDate = "2090-12-31", includedDate = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "dateTo-Date To must not be null;")
    private LocalDate dateTo;

    @Size(min = 1, max = 512, message = "orderNumber-Order Number length is out of range: [{min}:{max}];")
    @Pattern(regexp = "^[А-Яа-яA-Za-z\\d!#$%&'*+\\-/=?^_`|{}~.()\\s]*$", message = "orderNumber-Order Number has invalid pattern;")
    @NotBlank(message = "orderNumber-Must not be blank;")
    private String orderNumber;

    @Size(min = 1, max = 512, message = "certificationNumber-Certification Number length is out of range: [{min}:{max}];")
    @Pattern(regexp = "^[А-Яа-яA-Za-z\\d!#$%&'*+\\-/=?^_`|{}~.()\\s]*$", message = "certificationNumber-Certification Number has invalid pattern;")
    @NotBlank(message = "certificationNumber-Certification Number must not be blank;")
    private String certificationNumber;

    @NotNull(message = "amountOfDiscount-Amount Of Discount must not be null;")
    @DecimalMin(value = "0.01", message = "amountOfDiscount-Amount Of Discount must be greater or equal then: [{value}];")
    @DecimalMax(value = "100.00", message = "amountOfDiscount-Amount Of Discount must be less or equal then: [{value}];")
    @Digits(integer = 3, fraction = 2, message = "amountOfDiscount-Invalid Decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal amountOfDiscount;

    @NotNull(message = "amountOfDiscountInMoneyPerKWH-Amount Of Discount In Money Per KWH must not be null;")
    @DecimalMin(value = "0.01", message = "amountOfDiscountInMoneyPerKWH-Amount Of Discount In Money Per KWH must be greater or equal then: [{value}];")
    @DecimalMax(value = "99999999.99999", message = "amountOfDiscountInMoneyPerKWH-Amount Of Discount In Money Per KWH must be less or equal then: [{value}];")
    @Digits(integer = 8, fraction = 5, message = "amountOfDiscountInMoneyPerKWH-Amount Of Discount In Money Per KWH invalid decimal fractions: max [{integer}] digits and [{fraction}] fractions;")
    private BigDecimal amountOfDiscountInMoneyPerKWH;

    @NotNull(message = "currencyId-Currency must not be null;")
    private Long currencyId;

    @Min(value = 1, message = "amountOfDiscountInMoneyPerKWH-Amount Of Discount In Money Per KWH must be greater or equal then: [{value}];")
    @Max(value = 99999999, message = "amountOfDiscountInMoneyPerKWH-Amount Of Discount In Money Per KWH must be less or equal then: [{value}];")
    private Long volumeWithoutDiscountInKWH;

    @NotNull(message = "customerId-Customer ID must not be null;")
    private Long customerId;

    @NotNull(message = "pointOfDeliveryIds-Point of Delivery IDs must not be null;")
    @Size(min = 1, message = "pointOfDeliveryIds-Point of Delivery IDs must contain at least {min} object;")
    private Set<Long> pointOfDeliveryIds;
}

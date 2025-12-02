package bg.energo.phoenix.model.request.customer.list;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.customer.list.CustomerRelatedPaymentSearchField;
import bg.energo.phoenix.model.request.receivable.payment.PaymentListColumns;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRelatedPaymentsListRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private BigDecimal initialAmountFrom;

    private BigDecimal initialAmountTo;

    private BigDecimal currentAmountFrom;

    private BigDecimal currentAmountTo;

    private List<Long> collectionChannelIds;

    private Boolean blockedForOffsetting;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "blockedForOffsettingToDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate paymentDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "blockedForOffsettingToDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate paymentDateTo;

    private PaymentListColumns columns;

    private CustomerRelatedPaymentSearchField searchFields;

    private Sort.Direction direction;

}

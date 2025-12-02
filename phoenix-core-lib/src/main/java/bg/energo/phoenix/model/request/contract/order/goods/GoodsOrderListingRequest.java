package bg.energo.phoenix.model.request.contract.order.goods;

import bg.energo.phoenix.model.customAnotations.DuplicatedValuesValidator;
import bg.energo.phoenix.model.customAnotations.PromptSymbolReplacer;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderListingSearchFields;
import bg.energo.phoenix.model.enums.contract.order.goods.GoodsOrderListingSortFields;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Data
@PromptSymbolReplacer
public class GoodsOrderListingRequest {
    private String prompt;

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String goodsName;

    @DuplicatedValuesValidator(fieldPath = "goodsIds")
    private List<Long> goodsIds;

    @DuplicatedValuesValidator(fieldPath = "goodsSupplierIds")
    private List<Long> goodsSupplierIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfOrderCreationFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfOrderCreationTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceMaturityDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceMaturityDateTo;

    private Set<Boolean> invoicePaid;

    private Boolean directDebit;

    @DuplicatedValuesValidator(fieldPath = "accountManagerIds")
    private List<Long> accountManagerIds;

    private GoodsOrderListingSearchFields searchBy;

    private GoodsOrderListingSortFields sortBy;

    private Sort.Direction direction;

    @JsonIgnore
    @AssertTrue(message = "invoiceMaturityDateFrom-Invoice maturity date to cannot be before invoice maturity date from;")
    public boolean isInvoiceMaturityDatesValid() {
        if (Stream.of(invoiceMaturityDateFrom, invoiceMaturityDateTo).noneMatch(Objects::isNull)) {
            return !invoiceMaturityDateTo.isBefore(invoiceMaturityDateFrom);
        }

        return true;
    }

    @JsonIgnore
    @AssertTrue(message = "dateOfOrderCreationFrom-Date of order creation to cannot be before date of order creation from;")
    public boolean isDatesOfOrderCreationValid() {
        if (Stream.of(dateOfOrderCreationFrom, dateOfOrderCreationTo).noneMatch(Objects::isNull)) {
            return !dateOfOrderCreationTo.isBefore(dateOfOrderCreationFrom);
        }

        return true;
    }
}

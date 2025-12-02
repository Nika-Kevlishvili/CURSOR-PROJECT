package bg.energo.phoenix.model.response.receivable.customerLiability;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerLiabilityAndReceivableListingResponse {
    private List<CustomerLiabilityAndReceivableListingMiddleResponse> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;
    private boolean first;
    private boolean last;
    private int numberOfElements;

    private BigDecimal totalInitialAmount;
    private BigDecimal totalCurrentAmount;

    public CustomerLiabilityAndReceivableListingResponse(Page<CustomerLiabilityAndReceivableListingMiddleResponse> page,
                                                         BigDecimal totalInitialAmount,
                                                         BigDecimal totalCurrentAmount) {
        this.content = page.getContent();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.size = page.getSize();
        this.number = page.getNumber();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.numberOfElements = page.getNumberOfElements();
        this.totalInitialAmount = totalInitialAmount;
        this.totalCurrentAmount = totalCurrentAmount;
    }
}

package bg.energo.phoenix.model.response.pod.discount;

import bg.energo.phoenix.model.entity.EntityStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DiscountListResponse {
    private Long id;
    private String podName;
    private String customerIdentifier;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal amountInPercent;
    private BigDecimal amountInMoneyPerKWH;
    private String invoiced;

    private LocalDateTime createDate;
    private EntityStatus status;

    public DiscountListResponse(Long id, String podIdentifier, String customerIdentifier, LocalDate fromDate, LocalDate toDate, BigDecimal amountInPercent, BigDecimal amountInMoneyPerKWH, String invoiced, LocalDateTime createDate, EntityStatus status) {
        this.id = id;
        this.podName = podIdentifier;
        this.customerIdentifier = customerIdentifier.replaceAll("\\s+", " ").trim();
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.amountInPercent = amountInPercent;
        this.amountInMoneyPerKWH = amountInMoneyPerKWH;
        this.invoiced = invoiced;
        this.createDate = createDate;
        this.status = status;
    }
}

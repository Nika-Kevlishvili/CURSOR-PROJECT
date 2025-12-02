package bg.energo.phoenix.model.response.receivable.rescheduling;

import bg.energo.phoenix.model.enums.receivable.CustomerLiabilitiesOutgoingDocType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReschedulingLiabilityResponse {

    private Long liabilityId;
    private String liability;
    private BigDecimal originalInitialAmount;
    private LocalDate dueDate;
    private BigDecimal originalCurrentAmount;
    private LocalDate interestsFromDate;
    private CustomerLiabilitiesOutgoingDocType outgoingDocumentType;
    private Long outgoingDocumentId;
    private boolean checked;
    private BigDecimal liabilityCurrentAmountInLeva;
    private BigDecimal liabilityCurrentAmountInEuro;
    private BigDecimal liabilityInitialAmountInLeva;
    private BigDecimal liabilityInitialAmountInEuro;
    private Long currencyId;
    private String currencyName;

    public ReschedulingLiabilityResponse(ReschedulingLiabilityMiddleResponse middleResponse) {
        this.liabilityId = middleResponse.getLiabilityId();
        this.liability = middleResponse.getLiability();
        this.dueDate = middleResponse.getDueDate();
        this.interestsFromDate = middleResponse.getInterestsFromDate();
        this.outgoingDocumentId = middleResponse.getOutgoingDocumentId();
        this.originalCurrentAmount = middleResponse.getOriginalCurrentAmount();
        this.originalInitialAmount = middleResponse.getOriginalInitialAmount();
        this.liabilityCurrentAmountInEuro = middleResponse.getCurrentLiabilityAmountInEuro();
        this.liabilityCurrentAmountInLeva = middleResponse.getCurrentLiabilityAmountInLeva();
        this.liabilityInitialAmountInEuro = middleResponse.getInitialLiabilityAmountInEuro();
        this.liabilityInitialAmountInLeva = middleResponse.getInitialLiabilityAmountInLeva();
        this.currencyId=middleResponse.getCurrencyId();
        this.currencyName=middleResponse.getCurrencyName();
        this.outgoingDocumentType = middleResponse.getOutgoingDocumentType() != null ? CustomerLiabilitiesOutgoingDocType.valueOf(middleResponse.getOutgoingDocumentType()) : null;
        checked = false;
    }
}

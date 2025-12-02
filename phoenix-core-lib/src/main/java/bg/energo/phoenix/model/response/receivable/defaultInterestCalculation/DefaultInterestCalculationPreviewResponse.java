package bg.energo.phoenix.model.response.receivable.defaultInterestCalculation;

import bg.energo.phoenix.model.enums.receivable.CustomerLiabilitiesOutgoingDocType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefaultInterestCalculationPreviewResponse {
    private Long LiabilityId;
    private String liabilityNumber;
    private String outgoingDocumentInfo;
    private BigDecimal initialAmount;
    private LocalDate dueDate;
    private BigDecimal currentAmount;
    private Long outgoingDocumentId;
    private CustomerLiabilitiesOutgoingDocType outgoingDocumentType;
}

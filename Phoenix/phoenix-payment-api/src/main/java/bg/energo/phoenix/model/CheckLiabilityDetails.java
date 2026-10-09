package bg.energo.phoenix.model;

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
public class CheckLiabilityDetails {

    private Long liabilityId;
    private BigDecimal currentAmount;
    private BigDecimal lfpAmount;
    private BigDecimal totalAmount;
    private String outgoingDocumentFromExternalSystem;
    private Long currencyId;
    private List<String> podIdentifiers;
    private LocalDate dueDate;
    private LocalDate documentDate;
    private String documentNumber;

}



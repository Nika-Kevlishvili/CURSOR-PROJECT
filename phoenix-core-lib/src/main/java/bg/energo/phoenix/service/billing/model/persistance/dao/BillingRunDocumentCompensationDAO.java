package bg.energo.phoenix.service.billing.model.persistance.dao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class BillingRunDocumentCompensationDAO {
    private String documentNumber;
    private LocalDate documentDate;
    private LocalDate period;
    private BigDecimal volumes;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private String podId;
}

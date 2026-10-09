package bg.energo.migration.integration.phoenix.customer.model.request.bankingdetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBankingDetails {
    private Boolean directDebit;
    private Long bankId;
    private String bic;
    private String iban;
    private String declaredConsumption;
    private List<Long> preferenceIds;
    private Long creditRatingId;
}

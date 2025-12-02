package bg.energo.phoenix.model.response.contract.serviceContract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractBankingDetailsResponse {
    private Boolean directDebit;
    private Long bankId;
    private String bankName;
    private String bic;
    private String iban;
}

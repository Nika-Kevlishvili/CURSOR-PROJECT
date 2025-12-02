package bg.energo.phoenix.model.request.billing.companyDetails.baseDTO;

import bg.energo.phoenix.model.customAnotations.customer.ValidIBAN;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class BaseCompanyBankDTO {
    private Long bankId;
    private String bic;
    @NotBlank(message = "baseCompanyBankDTO.iban cannot be null")
    @ValidIBAN(errorMessageKey = "baseCompanyBankDTO.iban")
    private String iban;

}

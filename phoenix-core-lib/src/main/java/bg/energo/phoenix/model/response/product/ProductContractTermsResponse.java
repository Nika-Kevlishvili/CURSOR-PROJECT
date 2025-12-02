package bg.energo.phoenix.model.response.product;

import bg.energo.phoenix.model.entity.product.product.ProductContractTerms;
import bg.energo.phoenix.model.enums.product.product.ProductContractTermRenewalType;
import bg.energo.phoenix.model.enums.product.product.ProductTermPeriodType;
import bg.energo.phoenix.model.enums.product.product.ProductTermType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductContractTermsResponse {
    private Long id;
    private String name;
    private boolean perpetuityCause;
    private ProductTermPeriodType typeOfTerms;
    private ProductTermType periodType;
    private Integer value;
    private Boolean automaticRenewal;
    private Integer numberOfRenewals;
    private boolean fromGroup;
    private Integer renewalPeriodValue;
    private ProductContractTermRenewalType renewalPeriodType;

    public ProductContractTermsResponse(ProductContractTerms contractTerms) {
        this.id = contractTerms.getId();
        this.name = contractTerms.getName();
        this.perpetuityCause = contractTerms.getPerpetuityCause();
        this.typeOfTerms = contractTerms.getPeriodType();
        this.periodType = contractTerms.getType();
        this.value = contractTerms.getValue();
        this.automaticRenewal = contractTerms.getAutomaticRenewal();
        this.numberOfRenewals = contractTerms.getNumberOfRenewals();
        this.renewalPeriodValue = contractTerms.getRenewalPeriodValue();
        this.renewalPeriodType = contractTerms.getRenewalPeriodType();
    }
}

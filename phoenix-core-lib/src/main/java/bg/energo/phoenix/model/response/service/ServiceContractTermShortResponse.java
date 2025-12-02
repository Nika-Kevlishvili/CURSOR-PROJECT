package bg.energo.phoenix.model.response.service;

import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermRenewalType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceContractTermShortResponse {

    private Long id;
    private String name;
    private ServiceContractTermPeriodType periodType;
    private ServiceContractTermType termType;
    private Integer value;
    private Boolean perpetuityCause;
    private Boolean automaticRenewal;
    private Integer numberOfRenewals;
    private boolean termFromGroup;
    private Integer renewalPeriodValue;
    private ServiceContractTermRenewalType renewalPeriodType;

    public ServiceContractTermShortResponse(ServiceContractTerm contractTerms) {
        this.id = contractTerms.getId();
        this.name = contractTerms.getName();
        this.perpetuityCause = contractTerms.getPerpetuityClause();
        this.periodType = contractTerms.getPeriodType();
        this.termType = contractTerms.getTermType();
        this.value = contractTerms.getValue();
        this.automaticRenewal = contractTerms.getAutomaticRenewal();
        this.numberOfRenewals = contractTerms.getNumberOfRenewals();
        this.renewalPeriodValue = contractTerms.getRenewalPeriodValue();
        this.renewalPeriodType = contractTerms.getRenewalPeriodType();
    }
}

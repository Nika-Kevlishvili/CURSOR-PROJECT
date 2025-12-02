package bg.energo.phoenix.model.response.contract.serviceContract;

import bg.energo.phoenix.model.entity.product.service.ServiceContractTerm;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermPeriodType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermType;
import lombok.Data;

@Data
public class ServiceContractTermsResponse {
    private Long id;
    private String name;
    private boolean perpetuityCause;
    private ServiceContractTermPeriodType typeOfTerms;
    private ServiceContractTermType periodType;
    private Integer value;
    private Boolean automaticRenewal;
    private Integer numberOfRenewals;
    private boolean termFromGroup;


    public ServiceContractTermsResponse(ServiceContractTerm contractTerms) {
        this.id = contractTerms.getId();
        this.name = contractTerms.getName();
        this.perpetuityCause = contractTerms.getPerpetuityClause();
        this.typeOfTerms = contractTerms.getPeriodType();
        this.periodType = contractTerms.getTermType();
        this.value = contractTerms.getValue();
        this.automaticRenewal = contractTerms.getAutomaticRenewal();
        this.numberOfRenewals = contractTerms.getNumberOfRenewals();
    }
}

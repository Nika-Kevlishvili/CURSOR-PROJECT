package bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract;

import lombok.Data;

@Data
public class ServiceContractTerminationGenericModel {
    private ServiceContractTerminationWithContractTermsResponse termsResponses;
    private ServiceContractTerminationByTermsResponse contractTerminationByTermsResponse;
    private ServiceContractTerminationWithActionsResponse actionsResponse;

    public ServiceContractTerminationGenericModel(ServiceContractTerminationWithContractTermsResponse termsResponses){
        this.termsResponses = termsResponses;
    }

    public ServiceContractTerminationGenericModel(ServiceContractTerminationByTermsResponse contractTerminationByTermsResponse){
        this.contractTerminationByTermsResponse = contractTerminationByTermsResponse;
    }

    public ServiceContractTerminationGenericModel(ServiceContractTerminationWithActionsResponse actionsResponse) {
        this.actionsResponse = actionsResponse;
    }

    public Long getId() {
        if (termsResponses != null) return termsResponses.getContractId();
        else if (contractTerminationByTermsResponse != null) return contractTerminationByTermsResponse.getId();
        else if (actionsResponse != null) return actionsResponse.getContractId();
        else return null;
    }
}

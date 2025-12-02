package bg.energo.phoenix.model.response.contract.productContract.terminations;

import lombok.Data;

@Data
public class ProductContractTerminationGenericModel {
    private ProductContractTerminationWithContractTermsResponse termsResponses;
    private ProductContractTerminationWithActionsResponse actionsResponse;
    private ProductContractTerminationWithPodsResponse podsResponse;
    /**
     * Termination for product Contract with {@link bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus#ENTERED_INTO_FORCE} and Automatic termination term
     */
    private ProductContractTerminationByTermsResponse contractTerminationByTermsResponse;

    public ProductContractTerminationGenericModel(ProductContractTerminationWithContractTermsResponse termsResponses) {
        this.termsResponses = termsResponses;
    }

    public ProductContractTerminationGenericModel(ProductContractTerminationWithPodsResponse podsResponse) {
        this.podsResponse = podsResponse;
    }

    public ProductContractTerminationGenericModel(ProductContractTerminationWithActionsResponse actionsResponse) {
        this.actionsResponse = actionsResponse;
    }

    public ProductContractTerminationGenericModel(ProductContractTerminationByTermsResponse productContractTerminationByTermsResponse) {
        this.contractTerminationByTermsResponse = productContractTerminationByTermsResponse;
    }

    public Long getId() {
        if (termsResponses != null)
            return termsResponses.getContractId();
        else if (podsResponse != null) {
            return podsResponse.getId();
        } else if (actionsResponse != null) {
            return actionsResponse.getContractId();
        } else if (contractTerminationByTermsResponse != null) {
            return contractTerminationByTermsResponse.getId();
        } else {
            return null;
        }
    }

    public Long getTerminationId() {
        if (termsResponses != null)
            return termsResponses.getTerminationId();
        else if (podsResponse != null) {
            return podsResponse.getTerminationId();
        } else if (actionsResponse != null) {
            return actionsResponse.getTerminationId();
        } else if (contractTerminationByTermsResponse != null) {
            return null;
        } else {
            return null;
        }
    }
}

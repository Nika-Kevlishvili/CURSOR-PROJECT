package bg.energo.phoenix.model.response.contract.productContract.terminations.serviceContract;

import bg.energo.phoenix.model.enums.product.product.ProductTermType;
import bg.energo.phoenix.model.enums.product.service.ServiceContractTermRenewalType;

public interface ServiceContractTerminationWithContractTermsResponse {
    Long getDetailId();

    Long getContractId();

    Long getTerminationId();

    Boolean getAutomaticRenewal();

    Boolean getPerpetuityCause();

    Integer getNumberOfRenewals();

    Integer getTermValue();

    ProductTermType getTermType();

    Long getTermId();

    ServiceContractTermRenewalType getRenewalPeriodType();

    Integer getRenewalValue();
}

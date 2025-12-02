package bg.energo.phoenix.model.response.contract.productContract.terminations;

import bg.energo.phoenix.model.enums.product.product.ProductContractTermRenewalType;
import bg.energo.phoenix.model.enums.product.product.ProductTermType;

public interface ProductContractTerminationWithContractTermsResponse {

    Long getDetailId();

    Long getContractId();

    Long getTerminationId();

    Boolean getAutomaticRenewal();

    Boolean getPerpetuityCause();

    Integer getNumberOfRenewals();

    Integer getTermValue();

    ProductTermType getTermType();

    Long getTermId();

    Integer getRenewalValue();

    ProductContractTermRenewalType getRenewalPeriodType();

}

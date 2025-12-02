package bg.energo.phoenix.model.response.contract.productContract.terminations;

import bg.energo.phoenix.model.enums.product.termination.terminations.AutoTerminationFrom;

import java.time.LocalDate;

public interface ProductContractTerminationWithPodsResponse {
    Long getId();
    LocalDate getDeactivationDate();
    AutoTerminationFrom getAutoTerminationFrom();
    Long getTerminationId();
    Boolean getNoticeDue();
}

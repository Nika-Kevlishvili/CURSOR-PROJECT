package bg.energo.phoenix.model.response.customer.list;

import java.time.LocalDate;

public interface CustomerRelatedContractListResponse {

    String getContractNumber();

    Long getVersion();

    String getContractType();

    String getContractName();

    LocalDate getDateOfSigning();

    String getContractStatus();

    String getContractSubStatus();

    LocalDate getActivationDate();

    LocalDate getContractTermEndDate();

    LocalDate getEntryIntoForceDate();

    LocalDate getCreationDate();

    Long getContractId();

    Long getContractDetailId();

    String getStatus();

}

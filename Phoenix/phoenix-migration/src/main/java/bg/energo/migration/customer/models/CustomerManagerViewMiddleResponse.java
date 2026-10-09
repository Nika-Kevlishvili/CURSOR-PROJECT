package bg.energo.migration.customer.models;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;

public interface CustomerManagerViewMiddleResponse {
    String getPersonalNumber();
    String getCustomerId();
    String getName();
    String getMiddleName();
    String getSurname();
    Long getTitleId();
    Long getRepresentationMethodId();
    String getJobPosition();
    Status getStatus();
}

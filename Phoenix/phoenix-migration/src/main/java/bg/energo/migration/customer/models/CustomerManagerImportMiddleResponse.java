package bg.energo.migration.customer.models;

import bg.energo.migration.integration.phoenix.customer.model.enums.Status;

public interface CustomerManagerImportMiddleResponse {
    String getCustomerIdentifier();
    String getTitle1();
    String getName1();
    String getMiddlename1();
    String getSurname1();
    String getPosition1();
    String getRepresentation1();
    String getPersonalNumber1();
    String getTitle2();
    String getName2();
    String getMiddlename2();
    String getSurname2();
    String getPosition2();
    String getRepresentation2();
    String getTitle3();
    String getName3();
    String getMiddlename3();
    String getSurname3();
    String getPosition3();
    String getRepresentation3();
}

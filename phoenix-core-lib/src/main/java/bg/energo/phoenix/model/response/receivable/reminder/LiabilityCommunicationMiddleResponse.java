package bg.energo.phoenix.model.response.receivable.reminder;

import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;

public interface LiabilityCommunicationMiddleResponse {

    Long getCommunicationId();

    Long getContactId();

    CustomerCommContactTypes getContactType();

    String getContactValue();

}
